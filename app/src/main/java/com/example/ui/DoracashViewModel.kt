package com.example.ui

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.DoraemonAction
import com.example.ai.DoraemonActionType
import com.example.ai.DoraemonAssistant
import com.example.ai.DoraemonChatMessage
import com.example.ai.DoraemonChatUiState
import com.example.ai.DoraemonTipUiState
import com.example.data.AccountType
import com.example.data.AutoDebitMode
import com.example.data.BankCard
import com.example.data.BillFrequency
import com.example.data.CardColorTheme
import com.example.data.CardKind
import com.example.data.CardNetwork
import com.example.data.CategoryBudget
import com.example.data.CategoryCatalog
import com.example.data.CurrencyCode
import com.example.data.DarkThemeMode
import com.example.data.DoracashRepository
import com.example.data.DoracashSnapshot
import com.example.data.EntryClassification
import com.example.data.FinanceTransaction
import com.example.data.FinanceUtils
import com.example.data.SavingsGoal
import com.example.data.SavingsType
import com.example.data.ScheduledBill
import com.example.data.TxType
import com.example.data.UiState
import com.example.data.WalletAccount
import com.example.ui.components.DoraemonGesture
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class DoracashViewModel(
    private val repository: DoracashRepository,
    private val currentUserId: String,
    private val displayName: String,
    private val assistant: DoraemonAssistant = DoraemonAssistant(),
    private val prefs: SharedPreferences? = null,
    val currentDeviceId: String = "",
    val currentDeviceName: String = "Android",
    val currentVersionName: String = "1.0",
    val currentVersionCode: Int = 1
) : ViewModel() {

    val uiState: StateFlow<UiState<DoracashSnapshot>> = repository.observeDashboard(currentUserId)
        .map<DoracashSnapshot, UiState<DoracashSnapshot>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing Doracash data", root)
            emit(UiState.Error(root.localizedMessage ?: "No se pudieron cargar tus finanzas."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _doraemonEnabled = MutableStateFlow(
        prefs?.getBoolean(PREF_DORAEMON_ENABLED, true) ?: true
    )
    val doraemonEnabled: StateFlow<Boolean> = _doraemonEnabled.asStateFlow()

    private val _doraemonTipState = MutableStateFlow<DoraemonTipUiState>(DoraemonTipUiState.Idle)
    val doraemonTipState: StateFlow<DoraemonTipUiState> = _doraemonTipState.asStateFlow()

    private val _dismissedAlertKey = MutableStateFlow<String?>(null)
    val dismissedAlertKey: StateFlow<String?> = _dismissedAlertKey.asStateFlow()

    private val _doraemonMessages = MutableStateFlow<List<DoraemonChatMessage>>(emptyList())
    val doraemonMessages: StateFlow<List<DoraemonChatMessage>> = _doraemonMessages.asStateFlow()

    private val _doraemonChatState = MutableStateFlow<DoraemonChatUiState>(DoraemonChatUiState.Idle)
    val doraemonChatState: StateFlow<DoraemonChatUiState> = _doraemonChatState.asStateFlow()

    private val _requestedTabNavigation = MutableStateFlow<DoracashTab?>(null)
    val requestedTabNavigation: StateFlow<DoracashTab?> = _requestedTabNavigation.asStateFlow()

    private var lastEvaluatedSignature: String? = null

    init {
        viewModelScope.launch {
            repository.ensureInitialized(currentUserId, displayName)
                .onFailure { _feedbackMessage.value = it.localizedMessage }
            if (currentDeviceId.isNotBlank()) {
                repository.registerDeviceAndSyncRelease(
                    deviceId = currentDeviceId,
                    deviceName = currentDeviceName,
                    versionName = currentVersionName,
                    versionCode = currentVersionCode
                ).onFailure { err ->
                    Log.w(TAG, "Device sync registration warning", err)
                }
            }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun consumeTabNavigation() {
        _requestedTabNavigation.value = null
    }

    fun clearDoraemonChat() {
        _doraemonMessages.value = emptyList()
        _doraemonChatState.value = DoraemonChatUiState.Idle
    }

    fun setDoraemonEnabled(enabled: Boolean, snapshot: DoracashSnapshot?) {
        _doraemonEnabled.value = enabled
        prefs?.edit()?.putBoolean(PREF_DORAEMON_ENABLED, enabled)?.apply()
        if (!enabled) {
            _doraemonTipState.value = DoraemonTipUiState.NothingImportant
            _feedbackMessage.value = "Mascota Doraemon desactivada"
        } else {
            _dismissedAlertKey.value = null
            lastEvaluatedSignature = null
            _feedbackMessage.value = "Doraemon activado · Solo aparecerá con avisos importantes"
            if (snapshot != null) {
                evaluateImportantMoment(snapshot, force = true)
            }
        }
    }

    fun dismissDoraemonNotice() {
        val current = _doraemonTipState.value
        if (current is DoraemonTipUiState.Important) {
            _dismissedAlertKey.value = current.alertKey
        }
        _doraemonTipState.value = DoraemonTipUiState.NothingImportant
    }

    fun evaluateImportantMoment(snapshot: DoracashSnapshot, force: Boolean = false) {
        if (!_doraemonEnabled.value) {
            _doraemonTipState.value = DoraemonTipUiState.NothingImportant
            return
        }
        val signature = buildSnapshotSignature(snapshot)
        if (!force && signature == lastEvaluatedSignature) return
        if (_doraemonTipState.value is DoraemonTipUiState.Checking) return

        lastEvaluatedSignature = signature
        viewModelScope.launch {
            _doraemonTipState.value = DoraemonTipUiState.Checking
            try {
                val notice = assistant.evaluateImportantNotice(snapshot)
                if (!_doraemonEnabled.value) {
                    _doraemonTipState.value = DoraemonTipUiState.NothingImportant
                    return@launch
                }
                if (notice.isImportant && (force || notice.alertKey != _dismissedAlertKey.value)) {
                    if (force) _dismissedAlertKey.value = null
                    _doraemonTipState.value = DoraemonTipUiState.Important(
                        tip = notice.message,
                        gesture = notice.gesture,
                        alertKey = notice.alertKey
                    )
                } else {
                    _doraemonTipState.value = DoraemonTipUiState.NothingImportant
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Doraemon important-check timed out", e)
                _doraemonTipState.value = DoraemonTipUiState.Error(
                    "Doraemon tardó demasiado en revisar tus finanzas."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to evaluate important Doraemon notice", e)
                _doraemonTipState.value = DoraemonTipUiState.NothingImportant
            }
        }
    }

    private fun buildSnapshotSignature(s: DoracashSnapshot): String {
        val balSum = s.accounts.sumOf { FinanceUtils.round2(it.balance) }
        val paidCount = s.scheduledBills.sumOf { it.paidDates.size }
        val cardDebtSum = s.cards.sumOf { FinanceUtils.round2(it.debtBalance) }
        return "${s.accounts.size}|$balSum|${s.transactions.size}|${s.scheduledBills.size}|$paidCount|${s.budgets.size}|${s.goals.size}|${s.cards.size}|$cardDebtSum|${FinanceUtils.todayIso()}"
    }

    fun sendDoraemonMessage(userText: String, snapshot: DoracashSnapshot) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _doraemonChatState.value is DoraemonChatUiState.Thinking) return

        val userMsg = DoraemonChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = trimmed
        )
        if (trimmed.trim('¿', '?').equals("Cómo van mis finanzas", ignoreCase = true)) {
            val currency = CurrencyCode.safeValueOf(snapshot.profile.displayCurrency)
            val rate = snapshot.profile.exchangeRate
            val balance = snapshot.accounts.sumOf { FinanceUtils.convertCurrency(it.balance, it.currencyCode, currency, rate) }
            val month = FinanceUtils.currentYearMonth()
            fun monthly(type: TxType) = snapshot.transactions.filter { it.typeEnum == type && it.dateIso.startsWith(month) }
                .sumOf { FinanceUtils.convertCurrency(it.amount, it.currencyCode, currency, rate) }
            val reply = "Saldo en tus cuentas: ${FinanceUtils.formatMoney(balance, currency)}. " +
                "Este mes ingresaron ${FinanceUtils.formatMoney(monthly(TxType.INCOME), currency)} y gastaste ${FinanceUtils.formatMoney(monthly(TxType.EXPENSE), currency)}. " +
                "Movimientos pendientes: ${snapshot.pending.size}."
            _doraemonMessages.value = _doraemonMessages.value + userMsg + DoraemonChatMessage(id = UUID.randomUUID().toString(), isUser = false, text = reply)
            _doraemonChatState.value = DoraemonChatUiState.Idle
            return
        }
        val historyBeforeTurn = _doraemonMessages.value
        _doraemonMessages.value = historyBeforeTurn + userMsg
        _doraemonChatState.value = DoraemonChatUiState.Thinking

        viewModelScope.launch {
            try {
                val aiResp = assistant.chatWithDoraemon(
                    userMessage = trimmed,
                    snapshot = snapshot,
                    recentHistory = historyBeforeTurn
                )
                val actionResults = executeDoraemonActions(aiResp.actions, snapshot)
                val botMsg = DoraemonChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = aiResp.reply,
                    gesture = aiResp.gesture,
                    executedActions = actionResults
                )
                _doraemonMessages.value = _doraemonMessages.value + botMsg
                _doraemonChatState.value = DoraemonChatUiState.Idle
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Doraemon chat timed out", e)
                _doraemonChatState.value = DoraemonChatUiState.Error(
                    "La consulta tardó más de 60 segundos. Intenta de nuevo."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Doraemon chat failed", e)
                _doraemonChatState.value = DoraemonChatUiState.Error(
                    if (generateSequence<Throwable>(e) { it.cause }.any { it.message.orEmpty().contains("app check", true) || it.message.orEmpty().contains("appcheck", true) })
                        "Para activar este teléfono, copiá el código privado y registralo en App Check."
                    else e.localizedMessage ?: "No se pudo consultar a Doraemon."
                )
            }
        }
    }

    private suspend fun executeDoraemonActions(
        actions: List<DoraemonAction>,
        snapshot: DoracashSnapshot
    ): List<String> {
        if (actions.isEmpty()) return emptyList()
        val summaries = mutableListOf<String>()
        val rate = snapshot.profile.exchangeRate
        val today = FinanceUtils.todayIso()

        for (action in actions) {
            try {
                when (action.type) {
                    DoraemonActionType.ADD_TRANSACTION -> {
                        val txType = TxType.safeValueOf(action.txType)
                        val amt = action.amount ?: throw IllegalArgumentException("Falta monto válido.")
                        val srcAcc = resolveAccount(snapshot.accounts, action.accountRef)
                            ?: snapshot.accounts.firstOrNull()
                            ?: throw IllegalStateException("No hay cuentas disponibles.")
                        val dstAcc = if (txType == TxType.TRANSFER) {
                            resolveAccount(snapshot.accounts, action.toAccountRef)
                                ?: snapshot.accounts.firstOrNull { it.id != srcAcc.id }
                                ?: throw IllegalStateException("No encontré cuenta destino para transferir.")
                        } else null
                        val defaultCat = if (txType == TxType.INCOME) "otros_i" else "otros_g"
                        val catId = action.categoryId?.takeIf { it.isNotBlank() } ?: defaultCat
                        val dateIso = action.dateIso?.takeIf { it.length == 10 } ?: today
                        val note = action.note ?: ""
                        val sType = CategoryCatalog.savingsTypeOf(catId)
                        val targetGoal = action.targetRef?.let { resolveGoal(snapshot.goals, it) }
                        val applyAuto = txType == TxType.INCOME && (action.applyAutoDebit ?: true)

                        repository.addTransaction(
                            txType = txType,
                            amount = amt,
                            account = srcAcc,
                            toAccount = dstAcc,
                            categoryId = catId,
                            note = note,
                            dateIso = dateIso,
                            exchangeRate = rate,
                            savingsType = sType,
                            targetSavingsGoal = targetGoal,
                            existingGoals = snapshot.goals,
                            customSavingsName = action.title,
                            existingCards = snapshot.cards,
                            applyAutoCardDebit = applyAuto
                        ).getOrThrow()

                        val fmtAmt = FinanceUtils.formatMoney(amt, srcAcc.currencyCode)
                        val savingsSuffix = if (sType != null) " + sumado a ${sType.shortLabel}" else ""
                        val autoDebitCount = if (applyAuto) {
                            FinanceUtils.planAutoCardDebits(amt, srcAcc.currencyCode, snapshot.cards, rate).size
                        } else 0
                        val debitSuffix = if (autoDebitCount > 0) " · débito auto a $autoDebitCount tarjeta(s)" else ""
                        summaries.add("${txType.label} registrado: $fmtAmt (${srcAcc.name})$savingsSuffix$debitSuffix")
                    }

                    DoraemonActionType.DELETE_TRANSACTION -> {
                        val ref = action.targetRef ?: action.note
                        val tx = resolveTransaction(snapshot.transactions, ref)
                            ?: throw IllegalStateException("No encontré el movimiento a eliminar.")
                        repository.deleteTransaction(tx, snapshot.accounts, rate, snapshot.goals).getOrThrow()
                        summaries.add("Movimiento eliminado: ${tx.note.ifBlank { tx.categoryId }}")
                    }

                    DoraemonActionType.RECONCILE_ACCOUNT -> {
                        val acc = resolveAccount(snapshot.accounts, action.accountRef ?: action.targetRef)
                            ?: snapshot.accounts.firstOrNull { it.typeEnum == AccountType.CASH }
                            ?: snapshot.accounts.firstOrNull()
                            ?: throw IllegalStateException("No encontré la cuenta a conciliar.")
                        val counted = action.amount ?: throw IllegalArgumentException("Falta el saldo contado.")
                        val adjust = action.adjustBalance ?: true
                        val note = action.note ?: "Arqueo asistido por Doraemon"
                        repository.saveReconciliation(acc, counted, adjust, note).getOrThrow()
                        summaries.add("Arqueo en ${acc.name}: ${FinanceUtils.formatMoney(counted, acc.currencyCode)}")
                    }

                    DoraemonActionType.ADD_SCHEDULED_BILL -> {
                        val title = action.title ?: action.note ?: throw IllegalArgumentException("Falta el título del pago.")
                        val amt = action.amount ?: throw IllegalArgumentException("Falta el monto del pago.")
                        val acc = resolveAccount(snapshot.accounts, action.accountRef) ?: snapshot.accounts.firstOrNull()
                        val curr = action.currency?.let { CurrencyCode.safeValueOf(it) }
                            ?: acc?.currencyCode
                            ?: CurrencyCode.NIO
                        val freq = BillFrequency.safeValueOf(action.frequency)
                        val startDate = action.dateIso?.takeIf { it.length == 10 } ?: today
                        val catId = action.categoryId ?: "servicios"
                        repository.addScheduledBill(
                            title = title,
                            amount = amt,
                            currency = curr,
                            accountId = acc?.id.orEmpty(),
                            categoryId = catId,
                            frequency = freq,
                            startDateIso = startDate
                        ).getOrThrow()
                        summaries.add("Pago programado: $title (${FinanceUtils.formatMoney(amt, curr)})")
                    }

                    DoraemonActionType.PAY_SCHEDULED_BILL -> {
                        val bill = resolveBill(snapshot.scheduledBills, action.targetRef ?: action.title)
                            ?: throw IllegalStateException("No encontré el pago programado.")
                        val now = Calendar.getInstance()
                        val monthOcc = FinanceUtils.occurrencesForMonth(
                            listOf(bill),
                            now.get(Calendar.YEAR),
                            now.get(Calendar.MONTH)
                        ).firstOrNull()?.dateIso
                        val occIso = action.dateIso?.takeIf { it.length == 10 } ?: monthOcc ?: bill.startDateIso
                        val acc = resolveAccount(snapshot.accounts, action.accountRef)
                            ?: snapshot.accounts.firstOrNull { it.id == bill.accountId }
                            ?: snapshot.accounts.firstOrNull()
                        repository.payScheduledBill(bill, occIso, acc, rate, snapshot.goals).getOrThrow()
                        val savingsNote = if (CategoryCatalog.classificationOf(bill.categoryId) == EntryClassification.SAVINGS) {
                            " (sumado a Ahorros)"
                        } else ""
                        summaries.add("Pago realizado: ${bill.title}$savingsNote")
                    }

                    DoraemonActionType.DELETE_SCHEDULED_BILL -> {
                        val bill = resolveBill(snapshot.scheduledBills, action.targetRef ?: action.title)
                            ?: throw IllegalStateException("No encontré el recordatorio a eliminar.")
                        repository.deleteScheduledBill(bill.id).getOrThrow()
                        summaries.add("Recordatorio eliminado: ${bill.title}")
                    }

                    DoraemonActionType.SAVE_BUDGET -> {
                        val catId = action.categoryId ?: action.targetRef ?: "comida"
                        val limit = action.amount ?: throw IllegalArgumentException("Falta límite del presupuesto.")
                        val existing = snapshot.budgets.firstOrNull {
                            it.id == action.targetRef || it.categoryId.equals(catId, ignoreCase = true)
                        }
                        repository.saveBudget(existing, catId, limit).getOrThrow()
                        summaries.add("Presupuesto ${catId}: C$ ${FinanceUtils.round2(limit)}")
                    }

                    DoraemonActionType.DELETE_BUDGET -> {
                        val ref = action.targetRef ?: action.categoryId
                        val b = snapshot.budgets.firstOrNull {
                            it.id == ref || it.categoryId.equals(ref, ignoreCase = true)
                        } ?: throw IllegalStateException("No encontré el presupuesto.")
                        repository.deleteBudget(b.id).getOrThrow()
                        summaries.add("Presupuesto eliminado: ${b.categoryId}")
                    }

                    DoraemonActionType.ADD_GOAL -> {
                        val name = action.title ?: action.note ?: throw IllegalArgumentException("Falta el nombre del ahorro.")
                        val target = action.amount ?: 1.0
                        val deadline = action.dateIso?.takeIf { it.length == 10 } ?: ""
                        val iconKey = action.categoryId?.let { SavingsType.fromKey(it).key } ?: SavingsType.CAJITA.key
                        repository.addGoal(name, target, iconKey, deadline).getOrThrow()
                        summaries.add("Ahorro creado: $name")
                    }

                    DoraemonActionType.DEPOSIT_TO_GOAL -> {
                        val goal = resolveGoal(snapshot.goals, action.targetRef ?: action.title)
                            ?: throw IllegalStateException("No encontré ese ahorro.")
                        val acc = resolveAccount(snapshot.accounts, action.accountRef)
                            ?: snapshot.accounts.firstOrNull()
                            ?: throw IllegalStateException("No hay cuenta origen para abonar.")
                        val amt = action.amount ?: throw IllegalArgumentException("Falta el monto a abonar.")
                        repository.depositToGoal(goal, acc, amt, rate).getOrThrow()
                        summaries.add("Abono a '${goal.name}': ${FinanceUtils.formatMoney(amt, acc.currencyCode)}")
                    }

                    DoraemonActionType.DELETE_GOAL -> {
                        val goal = resolveGoal(snapshot.goals, action.targetRef ?: action.title)
                            ?: throw IllegalStateException("No encontré la meta a eliminar.")
                        repository.deleteGoal(goal.id).getOrThrow()
                        summaries.add("Meta eliminada: ${goal.name}")
                    }

                    DoraemonActionType.SAVE_ACCOUNT -> {
                        val existing = resolveAccount(snapshot.accounts, action.targetRef)
                        val name = action.title ?: existing?.name ?: action.accountRef
                            ?: throw IllegalArgumentException("Falta el nombre de la cuenta.")
                        val type = action.accountType?.let { AccountType.safeValueOf(it) }
                            ?: existing?.typeEnum
                            ?: AccountType.BANK
                        val curr = action.currency?.let { CurrencyCode.safeValueOf(it) }
                            ?: existing?.currencyCode
                            ?: CurrencyCode.NIO
                        val bal = action.amount ?: existing?.balance ?: 0.0
                        repository.saveAccount(existing, name, type, curr, bal, type.defaultIconKey).getOrThrow()
                        summaries.add(if (existing == null) "Cuenta creada: $name" else "Cuenta actualizada: $name")
                    }

                    DoraemonActionType.DELETE_ACCOUNT -> {
                        val acc = resolveAccount(snapshot.accounts, action.targetRef ?: action.accountRef)
                            ?: throw IllegalStateException("No encontré la cuenta a eliminar.")
                        repository.deleteAccount(acc.id).getOrThrow()
                        summaries.add("Cuenta eliminada: ${acc.name}")
                    }

                    DoraemonActionType.SAVE_CARD -> {
                        val existing = resolveCard(snapshot.cards, action.targetRef ?: action.last4)
                        val alias = action.title ?: existing?.alias ?: "Tarjeta"
                        val bank = action.bankName ?: existing?.bankName ?: ""
                        val kind = action.cardKind?.let { CardKind.safeValueOf(it) }
                            ?: existing?.cardKindEnum
                            ?: CardKind.CREDIT
                        val net = action.network?.let { CardNetwork.safeValueOf(it) }
                            ?: existing?.networkEnum
                            ?: CardNetwork.VISA
                        val last4 = action.last4 ?: existing?.last4 ?: "0000"
                        val curr = action.currency?.let { CurrencyCode.safeValueOf(it) }
                            ?: existing?.currencyCode
                            ?: CurrencyCode.NIO
                        val debt = action.amount ?: existing?.debtBalance ?: 0.0
                        val limit = action.creditLimit ?: existing?.creditLimit ?: 0.0
                        val linked = resolveAccount(snapshot.accounts, action.accountRef)?.id
                            ?: existing?.linkedAccountId
                            ?: snapshot.accounts.firstOrNull()?.id.orEmpty()
                        val mode = action.autoDebitMode?.let { AutoDebitMode.safeValueOf(it) }
                            ?: existing?.autoDebitModeEnum
                            ?: if (kind == CardKind.CREDIT) AutoDebitMode.FULL_DEBT else AutoDebitMode.MANUAL
                        val autoVal = action.autoDebitValue ?: existing?.autoDebitValue ?: 100.0
                        val autoOn = existing?.autoDebitEnabled ?: (mode != AutoDebitMode.MANUAL)
                        repository.saveCard(
                            editCard = existing,
                            alias = alias,
                            bankName = bank,
                            cardKind = kind,
                            network = net,
                            last4 = last4,
                            currency = curr,
                            debtBalance = debt,
                            creditLimit = limit,
                            linkedAccountId = linked,
                            autoDebitEnabled = autoOn,
                            autoDebitMode = mode,
                            autoDebitValue = autoVal,
                            googleWalletLinked = existing?.googleWalletLinked ?: true,
                            cardFrozen = existing?.cardFrozen ?: false,
                            cutoffDay = existing?.cutoffDay ?: 15,
                            dueDay = existing?.dueDay ?: 5,
                            colorTheme = existing?.colorThemeEnum ?: CardColorTheme.DORA_BLUE
                        ).getOrThrow()
                        summaries.add(if (existing == null) "Tarjeta agregada: $alias (••••$last4)" else "Tarjeta actualizada: $alias")
                    }

                    DoraemonActionType.ADD_CARD_CHARGE -> {
                        val card = resolveCard(snapshot.cards, action.targetRef ?: action.last4)
                            ?: throw IllegalStateException("No encontré esa tarjeta.")
                        val amt = action.amount ?: throw IllegalArgumentException("Falta el monto de la compra.")
                        val linked = snapshot.accounts.firstOrNull { it.id == card.linkedAccountId }
                        val fallback = resolveAccount(snapshot.accounts, action.accountRef) ?: snapshot.accounts.firstOrNull()
                        val cat = action.categoryId ?: "comida"
                        val note = action.note ?: "Compra con ${card.alias}"
                        val dateIso = action.dateIso?.takeIf { it.length == 10 } ?: today
                        repository.recordCardCharge(
                            card = card,
                            amountInCardCurrency = amt,
                            categoryId = cat,
                            note = note,
                            dateIso = dateIso,
                            linkedAccount = linked,
                            fallbackAccount = fallback,
                            exchangeRate = rate
                        ).getOrThrow()
                        summaries.add("Cargo en ${card.alias}: ${FinanceUtils.formatMoney(amt, card.currencyCode)}")
                    }

                    DoraemonActionType.PAY_CARD_DEBT -> {
                        val card = resolveCard(snapshot.cards, action.targetRef ?: action.last4)
                            ?: throw IllegalStateException("No encontré esa tarjeta.")
                        val payAcc = resolveAccount(snapshot.accounts, action.accountRef)
                            ?: snapshot.accounts.firstOrNull { it.id == card.linkedAccountId }
                            ?: snapshot.accounts.firstOrNull()
                            ?: throw IllegalStateException("No hay cuenta disponible para pagar.")
                        val amt = action.amount ?: FinanceUtils.convertCurrency(
                            card.debtBalance,
                            card.currencyCode,
                            payAcc.currencyCode,
                            rate
                        )
                        repository.payCardDebt(card, payAcc, amt, rate).getOrThrow()
                        summaries.add("Pago a tarjeta ${card.alias}: ${FinanceUtils.formatMoney(amt, payAcc.currencyCode)}")
                    }

                    DoraemonActionType.UPDATE_SETTINGS -> {
                        val curr = action.currency?.let { CurrencyCode.safeValueOf(it) }
                            ?: CurrencyCode.safeValueOf(snapshot.profile.displayCurrency)
                        val newRate = action.exchangeRate?.takeIf { it > 0.0 } ?: snapshot.profile.exchangeRate
                        val theme = action.darkThemeMode?.let { DarkThemeMode.safeValueOf(it) }
                            ?: DarkThemeMode.safeValueOf(snapshot.profile.darkThemeMode)
                        repository.updateSettings(curr, newRate, theme).getOrThrow()
                        summaries.add("Ajustes actualizados (${curr.name}, ${theme.label})")
                    }

                    DoraemonActionType.NAVIGATE_TAB -> {
                        val route = action.tab ?: "dashboard"
                        val tab = DoracashTab.fromRoute(route)
                        _requestedTabNavigation.value = tab
                        summaries.add("Abriendo pestaña: ${tab.label}")
                    }

                    DoraemonActionType.LOAD_DEMO_DATA -> {
                        repository.seedDemoData().getOrThrow()
                        summaries.add("Datos demo cargados")
                    }

                    DoraemonActionType.CLEAR_ALL_DATA -> {
                        repository.clearAllUserData().getOrThrow()
                        summaries.add("Bolsillo reiniciado")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Doraemon action ${action.type} failed", e)
                summaries.add("No se pudo ejecutar ${action.type.name}: ${e.localizedMessage ?: "error"}")
            }
        }
        return summaries
    }

    private fun resolveAccount(accounts: List<WalletAccount>, ref: String?): WalletAccount? {
        val clean = ref?.trim().orEmpty()
        if (clean.isEmpty()) return null
        return accounts.firstOrNull { it.id == clean }
            ?: accounts.firstOrNull { it.name.equals(clean, ignoreCase = true) }
            ?: accounts.firstOrNull { it.name.contains(clean, ignoreCase = true) }
    }

    private fun resolveBill(bills: List<ScheduledBill>, ref: String?): ScheduledBill? {
        val clean = ref?.trim().orEmpty()
        if (clean.isEmpty()) return null
        return bills.firstOrNull { it.id == clean }
            ?: bills.firstOrNull { it.title.equals(clean, ignoreCase = true) }
            ?: bills.firstOrNull { it.title.contains(clean, ignoreCase = true) }
    }

    private fun resolveGoal(goals: List<SavingsGoal>, ref: String?): SavingsGoal? {
        val clean = ref?.trim().orEmpty()
        if (clean.isEmpty()) return null
        return goals.firstOrNull { it.id == clean }
            ?: goals.firstOrNull { it.name.equals(clean, ignoreCase = true) }
            ?: goals.firstOrNull { it.name.contains(clean, ignoreCase = true) }
    }

    private fun resolveTransaction(txs: List<FinanceTransaction>, ref: String?): FinanceTransaction? {
        val clean = ref?.trim().orEmpty()
        if (clean.isEmpty()) return null
        return txs.firstOrNull { it.id == clean }
            ?: txs.firstOrNull { it.note.equals(clean, ignoreCase = true) }
            ?: txs.firstOrNull { it.note.contains(clean, ignoreCase = true) }
    }

    private fun resolveCard(cards: List<BankCard>, ref: String?): BankCard? {
        val clean = ref?.trim().orEmpty()
        if (clean.isEmpty()) return null
        return cards.firstOrNull { it.id == clean }
            ?: cards.firstOrNull { it.last4 == clean }
            ?: cards.firstOrNull { it.alias.equals(clean, ignoreCase = true) }
            ?: cards.firstOrNull { it.alias.contains(clean, ignoreCase = true) || it.bankName.contains(clean, ignoreCase = true) }
    }

    fun toggleDisplayCurrency(snapshot: DoracashSnapshot) {
        val current = CurrencyCode.safeValueOf(snapshot.profile.displayCurrency)
        val next = if (current == CurrencyCode.NIO) CurrencyCode.USD else CurrencyCode.NIO
        updateSettings(
            displayCurrency = next,
            exchangeRate = snapshot.profile.exchangeRate,
            darkThemeMode = DarkThemeMode.safeValueOf(snapshot.profile.darkThemeMode)
        )
    }

    fun updateSettings(
        displayCurrency: CurrencyCode,
        exchangeRate: Double,
        darkThemeMode: DarkThemeMode
    ) = launchAction("Preferencias actualizadas") {
        repository.updateSettings(displayCurrency, exchangeRate, darkThemeMode)
    }

    fun saveAccount(
        editAccount: WalletAccount?,
        name: String,
        type: AccountType,
        currency: CurrencyCode,
        balance: Double,
        iconKey: String
    ) = launchAction(if (editAccount == null) "Cuenta creada" else "Cuenta actualizada") {
        repository.saveAccount(editAccount, name, type, currency, balance, iconKey)
    }

    fun deleteAccount(accountId: String) = launchAction("Cuenta eliminada") {
        repository.deleteAccount(accountId)
    }

    fun addTransaction(
        txType: TxType,
        amount: Double,
        account: WalletAccount,
        toAccount: WalletAccount?,
        categoryId: String,
        note: String,
        dateIso: String,
        exchangeRate: Double,
        savingsType: SavingsType? = null,
        targetSavingsGoal: SavingsGoal? = null,
        existingGoals: List<SavingsGoal> = emptyList(),
        customSavingsName: String? = null,
        existingCards: List<BankCard> = emptyList(),
        applyAutoCardDebit: Boolean = false
    ) {
        val plannedCount = if (txType == TxType.INCOME && applyAutoCardDebit) {
            FinanceUtils.planAutoCardDebits(amount, account.currencyCode, existingCards, exchangeRate).size
        } else 0
        val successText = when {
            plannedCount > 0 -> "Ingreso guardado y debitado a $plannedCount tarjeta(s)"
            savingsType != null || CategoryCatalog.classificationOf(categoryId) == EntryClassification.SAVINGS ->
                "Movimiento guardado y sumado a Mis Ahorros"
            else -> "Movimiento guardado en el bolsillo"
        }
        launchAction(successText) {
            repository.addTransaction(
                txType = txType,
                amount = amount,
                account = account,
                toAccount = toAccount,
                categoryId = categoryId,
                note = note,
                dateIso = dateIso,
                exchangeRate = exchangeRate,
                savingsType = savingsType,
                targetSavingsGoal = targetSavingsGoal,
                existingGoals = existingGoals,
                customSavingsName = customSavingsName,
                existingCards = existingCards,
                applyAutoCardDebit = applyAutoCardDebit
            )
        }
    }

    fun deleteTransaction(
        tx: FinanceTransaction,
        accounts: List<WalletAccount>,
        exchangeRate: Double,
        goals: List<SavingsGoal> = emptyList()
    ) = launchAction("Movimiento eliminado") {
        repository.deleteTransaction(tx, accounts, exchangeRate, goals)
    }

    fun saveReconciliation(
        account: WalletAccount,
        countedBalance: Double,
        adjustBalance: Boolean,
        note: String
    ) = launchAction(
        if (adjustBalance) "Conciliación completada y saldo ajustado"
        else "Auditoría registrada"
    ) {
        repository.saveReconciliation(account, countedBalance, adjustBalance, note)
    }

    fun addScheduledBill(
        title: String,
        amount: Double,
        currency: CurrencyCode,
        accountId: String,
        categoryId: String,
        frequency: BillFrequency,
        startDateIso: String
    ) = launchAction("Fecha de pago programada") {
        repository.addScheduledBill(title, amount, currency, accountId, categoryId, frequency, startDateIso)
    }

    fun payScheduledBill(
        bill: ScheduledBill,
        occurrenceIso: String,
        account: WalletAccount?,
        exchangeRate: Double,
        existingGoals: List<SavingsGoal> = emptyList()
    ) = launchAction(
        if (CategoryCatalog.classificationOf(bill.categoryId) == EntryClassification.SAVINGS) {
            "Pago registrado y sumado a Ahorros: ${bill.title}"
        } else {
            "Pago registrado: ${bill.title}"
        }
    ) {
        repository.payScheduledBill(bill, occurrenceIso, account, exchangeRate, existingGoals)
    }

    fun deleteScheduledBill(billId: String) = launchAction("Recordatorio eliminado") {
        repository.deleteScheduledBill(billId)
    }

    fun saveBudget(
        existing: CategoryBudget?,
        categoryId: String,
        limitNio: Double
    ) = launchAction("Presupuesto guardado") {
        repository.saveBudget(existing, categoryId, limitNio)
    }

    fun deleteBudget(budgetId: String) = launchAction("Presupuesto eliminado") {
        repository.deleteBudget(budgetId)
    }

    fun addGoal(
        name: String,
        targetNio: Double,
        iconKey: String,
        deadlineIso: String,
        initialNio: Double = 0.0
    ) = launchAction("Ahorro registrado en el bolsillo") {
        repository.addGoal(name, targetNio, iconKey, deadlineIso, initialNio)
    }

    fun depositToGoal(
        goal: SavingsGoal,
        account: WalletAccount,
        amount: Double,
        exchangeRate: Double
    ) = launchAction("Aporte sumado a ${goal.name}") {
        repository.depositToGoal(goal, account, amount, exchangeRate)
    }

    fun recoverFromGoal(
        goal: SavingsGoal,
        account: WalletAccount,
        amount: Double,
        exchangeRate: Double
    ) = launchAction(
        if (goal.savingsType == SavingsType.PRESTAMO) {
            "Préstamo recuperado de ${goal.name}"
        } else {
            "Retiro realizado de ${goal.name}"
        }
    ) {
        repository.recoverFromGoal(goal, account, amount, exchangeRate)
    }

    fun deleteGoal(goalId: String) = launchAction("Ahorro eliminado") {
        repository.deleteGoal(goalId)
    }

    fun saveCard(
        editCard: BankCard?,
        alias: String,
        bankName: String,
        cardKind: CardKind,
        network: CardNetwork,
        last4: String,
        currency: CurrencyCode,
        debtBalance: Double,
        creditLimit: Double,
        linkedAccountId: String,
        autoDebitEnabled: Boolean,
        autoDebitMode: AutoDebitMode,
        autoDebitValue: Double,
        googleWalletLinked: Boolean,
        cardFrozen: Boolean,
        cutoffDay: Int,
        dueDay: Int,
        colorTheme: CardColorTheme
    ) = launchAction(if (editCard == null) "Tarjeta agregada a tu Wallet" else "Tarjeta actualizada") {
        repository.saveCard(
            editCard = editCard,
            alias = alias,
            bankName = bankName,
            cardKind = cardKind,
            network = network,
            last4 = last4,
            currency = currency,
            debtBalance = debtBalance,
            creditLimit = creditLimit,
            linkedAccountId = linkedAccountId,
            autoDebitEnabled = autoDebitEnabled,
            autoDebitMode = autoDebitMode,
            autoDebitValue = autoDebitValue,
            googleWalletLinked = googleWalletLinked,
            cardFrozen = cardFrozen,
            cutoffDay = cutoffDay,
            dueDay = dueDay,
            colorTheme = colorTheme
        )
    }

    fun toggleCardFrozen(card: BankCard) = launchAction(
        if (card.cardFrozen) "Tarjeta descongelada" else "Tarjeta congelada por seguridad"
    ) {
        repository.updateCardFlags(cardId = card.id, cardFrozen = !card.cardFrozen)
    }

    fun toggleCardWalletLink(card: BankCard) = launchAction(
        if (card.googleWalletLinked) "Desvinculada de Google Wallet" else "Marcada en Google Wallet"
    ) {
        repository.updateCardFlags(cardId = card.id, googleWalletLinked = !card.googleWalletLinked)
    }

    fun toggleCardAutoDebit(card: BankCard) = launchAction(
        if (card.autoDebitEnabled) "Auto-débito pausado para ${card.alias}" else "Auto-débito activado para ${card.alias}"
    ) {
        repository.updateCardFlags(cardId = card.id, autoDebitEnabled = !card.autoDebitEnabled)
    }

    fun deleteCard(cardId: String) = launchAction("Tarjeta eliminada") {
        repository.deleteCard(cardId)
    }

    fun recordCardCharge(
        card: BankCard,
        amountInCardCurrency: Double,
        categoryId: String,
        note: String,
        dateIso: String,
        accounts: List<WalletAccount>,
        exchangeRate: Double
    ) = launchAction("Compra registrada en ${card.alias}") {
        val linked = accounts.firstOrNull { it.id == card.linkedAccountId }
        val fallback = accounts.firstOrNull { it.currency == card.currency } ?: accounts.firstOrNull()
        repository.recordCardCharge(
            card = card,
            amountInCardCurrency = amountInCardCurrency,
            categoryId = categoryId,
            note = note,
            dateIso = dateIso,
            linkedAccount = linked,
            fallbackAccount = fallback,
            exchangeRate = exchangeRate
        )
    }

    fun payCardDebt(
        card: BankCard,
        payAccount: WalletAccount,
        amountInAccountCurrency: Double,
        exchangeRate: Double
    ) = launchAction("Abono aplicado a ${card.alias}") {
        repository.payCardDebt(card, payAccount, amountInAccountCurrency, exchangeRate)
    }

    fun publishAppUpdateChannel(
        latestVersionCode: Int,
        latestVersionName: String,
        downloadUrl: String,
        releaseNotes: String,
        forceNotice: Boolean
    ) = launchAction("Canal de actualización sincronizado con tus teléfonos") {
        repository.publishAppUpdateChannel(
            latestVersionCode = latestVersionCode,
            latestVersionName = latestVersionName,
            downloadUrl = downloadUrl,
            releaseNotes = releaseNotes,
            forceNotice = forceNotice
        )
    }

    fun removeSyncedDevice(deviceId: String) = launchAction("Teléfono desvinculado de la lista") {
        repository.removeSyncedDevice(deviceId)
    }

    fun savePending(tx: FinanceTransaction, goalName: String?) = launchAction("Movimiento pendiente guardado") { repository.savePending(tx, goalName) }
    fun settlePending(id: String, rate: Double) = launchAction("Movimiento realizado") { repository.settlePending(id, rate) }
    fun editTransaction(tx: FinanceTransaction, note: String, date: String) = launchAction("Movimiento actualizado") {
        repository.editTransaction(tx, note, date)
    }

    fun loadDemoData() = launchAction("Datos de demostración cargados") {
        repository.seedDemoData()
    }

    fun clearAllData() = launchAction("Bolsillo reiniciado en limpio") {
        repository.clearAllUserData()
    }

    private fun launchAction(successMsg: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isBusy.value = true
            block()
                .onSuccess { _feedbackMessage.value = successMsg }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Ocurrió un error al guardar." }
            _isBusy.value = false
        }
    }

    private companion object {
        const val TAG = "DoracashVM"
        const val PREF_DORAEMON_ENABLED = "doraemon_mascot_enabled"
    }
}
