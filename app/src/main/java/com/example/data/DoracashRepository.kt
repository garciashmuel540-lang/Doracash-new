package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class DoracashRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    init {
        // Explicit persistent disk cache so offline edits on one phone auto-sync to others
        runCatching {
            db.firestoreSettings = firestoreSettings {
                setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            }
        }
    }

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión con Google para acceder a Doracash.")

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeDashboard(userId: String): Flow<DoracashSnapshot> {
        val profileFlow = userDoc(userId)
            .snapshots()
            .map { snap ->
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = userId, userId = userId)
            }
            .wrapStreamError(userId, "users/$userId")

        val accountsFlow = userDoc(userId).collection("accounts")
            .snapshots()
            .map { snap ->
                snap.toObjects(WalletAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name.lowercase() }
            }
            .wrapStreamError(userId, "users/$userId/accounts")

        val transactionsFlow = userDoc(userId).collection("transactions")
            .snapshots()
            .map { snap ->
                snap.toObjects(FinanceTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.dateIso }
            }
            .wrapStreamError(userId, "users/$userId/transactions")

        val billsFlow = userDoc(userId).collection("scheduledBills")
            .snapshots()
            .map { snap ->
                snap.toObjects(ScheduledBill::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.startDateIso }
            }
            .wrapStreamError(userId, "users/$userId/scheduledBills")

        val budgetsFlow = userDoc(userId).collection("budgets")
            .snapshots()
            .map { snap ->
                snap.toObjects(CategoryBudget::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .wrapStreamError(userId, "users/$userId/budgets")

        val goalsFlow = userDoc(userId).collection("goals")
            .snapshots()
            .map { snap ->
                snap.toObjects(SavingsGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .wrapStreamError(userId, "users/$userId/goals")

        val reconFlow = userDoc(userId).collection("reconciliations")
            .snapshots()
            .map { snap ->
                snap.toObjects(CashReconciliation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.dateIso }
            }
            .wrapStreamError(userId, "users/$userId/reconciliations")

        val cardsFlow = userDoc(userId).collection("cards")
            .snapshots()
            .map { snap ->
                snap.toObjects(BankCard::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<BankCard> { it.debtBalance }
                            .thenBy { it.alias.lowercase() }
                    )
            }
            .wrapStreamError(userId, "users/$userId/cards")

        val devicesFlow = userDoc(userId).collection("devices")
            .snapshots()
            .map { snap ->
                snap.toObjects(SyncedDevice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.lastSeenIso }
            }
            .wrapStreamError(userId, "users/$userId/devices")

        val releaseFlow = userDoc(userId).collection("appMeta").document("release")
            .snapshots()
            .map { snap ->
                snap.toObject(AppReleaseInfo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .wrapStreamError(userId, "users/$userId/appMeta/release")

        return combine(
            listOf(
                profileFlow,
                accountsFlow,
                transactionsFlow,
                billsFlow,
                budgetsFlow,
                goalsFlow,
                reconFlow,
                cardsFlow,
                devicesFlow,
                releaseFlow
            )
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            DoracashSnapshot(
                profile = values[0] as UserProfile,
                accounts = values[1] as List<WalletAccount>,
                transactions = values[2] as List<FinanceTransaction>,
                scheduledBills = values[3] as List<ScheduledBill>,
                budgets = values[4] as List<CategoryBudget>,
                goals = values[5] as List<SavingsGoal>,
                reconciliations = values[6] as List<CashReconciliation>,
                cards = values[7] as List<BankCard>,
                devices = values[8] as List<SyncedDevice>,
                releaseInfo = values[9] as AppReleaseInfo?
            )
        }
    }

    private fun <T> Flow<T>.wrapStreamError(uid: String, path: String): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val ex = firestoreError ?: (error as? Exception)
        if (ex != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(ex, OperationType.LIST, path)
        }
        throw error
    }

    suspend fun ensureInitialized(userId: String, displayName: String): Result<Unit> =
        safeWrite("users/$userId", OperationType.CREATE) {
            val uid = requireUserId()
            check(uid == userId)
            val snap = userDoc(uid).get().await()
            if (!snap.exists()) {
                val batch = db.batch()
                val profile = UserProfile(
                    userId = uid,
                    displayName = displayName.ifBlank { "Usuario Doracash" }.take(80),
                    displayCurrency = CurrencyCode.NIO.name,
                    exchangeRate = 36.62,
                    darkThemeMode = DarkThemeMode.SYSTEM.name
                )
                batch.set(userDoc(uid), profile.toCreateMap())

                val cashAcc = WalletAccount(
                    userId = uid,
                    name = "Efectivo en Cartera",
                    accountType = AccountType.CASH.name,
                    currency = CurrencyCode.NIO.name,
                    balance = 0.0,
                    iconKey = "cash"
                )
                val bankAcc = WalletAccount(
                    userId = uid,
                    name = "Banco Principal",
                    accountType = AccountType.BANK.name,
                    currency = CurrencyCode.NIO.name,
                    balance = 0.0,
                    iconKey = "bank"
                )
                batch.set(userDoc(uid).collection("accounts").document(), cashAcc.toCreateMap())
                batch.set(userDoc(uid).collection("accounts").document(), bankAcc.toCreateMap())
                batch.commit().await()
            }
        }

    suspend fun updateSettings(
        displayCurrency: CurrencyCode,
        exchangeRate: Double,
        darkThemeMode: DarkThemeMode
    ): Result<Unit> = safeWrite("users", OperationType.UPDATE) {
        val uid = requireUserId()
        require(exchangeRate in 0.01..10000.0) { "La tasa de cambio debe ser mayor a 0." }
        userDoc(uid).update(
            mapOf(
                "displayCurrency" to displayCurrency.name,
                "exchangeRate" to FinanceUtils.round2(exchangeRate),
                "darkThemeMode" to darkThemeMode.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun saveAccount(
        editAccount: WalletAccount?,
        name: String,
        type: AccountType,
        currency: CurrencyCode,
        balance: Double,
        iconKey: String
    ): Result<Unit> = safeWrite("accounts", OperationType.WRITE) {
        val uid = requireUserId()
        val trimmed = name.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 60) { "Nombre de cuenta inválido." }
        val col = userDoc(uid).collection("accounts")
        if (editAccount == null) {
            val acc = WalletAccount(
                userId = uid,
                name = trimmed,
                accountType = type.name,
                currency = currency.name,
                balance = FinanceUtils.round2(balance),
                iconKey = iconKey.ifBlank { type.defaultIconKey }
            )
            col.document().set(acc.toCreateMap()).await()
        } else {
            col.document(editAccount.id).update(
                mapOf(
                    "name" to trimmed,
                    "accountType" to type.name,
                    "currency" to currency.name,
                    "balance" to FinanceUtils.round2(balance),
                    "iconKey" to iconKey.ifBlank { type.defaultIconKey },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun deleteAccount(accountId: String): Result<Unit> =
        safeWrite("accounts/$accountId", OperationType.DELETE) {
            val uid = requireUserId()
            require(accountId.isNotBlank())
            userDoc(uid).collection("accounts").document(accountId).delete().await()
        }

    suspend fun addTransaction(
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
    ): Result<Unit> = safeWrite("transactions", OperationType.CREATE) {
        val uid = requireUserId()
        require(amount > 0.0) { "El monto debe ser mayor a cero." }
        require(dateIso.length == 10) { "Fecha inválida." }

        val batch = db.batch()
        val txRef = userDoc(uid).collection("transactions").document()
        val accRef = userDoc(uid).collection("accounts").document(account.id)

        if (txType == TxType.TRANSFER) {
            require(toAccount != null && toAccount.id != account.id) {
                "Selecciona una cuenta destino distinta."
            }
            val converted = FinanceUtils.convertCurrency(
                amount,
                account.currencyCode,
                toAccount.currencyCode,
                exchangeRate
            )
            val tx = FinanceTransaction(
                userId = uid,
                txType = TxType.TRANSFER.name,
                amount = FinanceUtils.round2(amount),
                currency = account.currency,
                accountId = account.id,
                toAccountId = toAccount.id,
                categoryId = "transfer",
                note = note.trim().ifBlank { "Transferencia a ${toAccount.name}" }.take(160),
                dateIso = dateIso
            )
            batch.set(txRef, tx.toCreateMap())
            batch.update(
                accRef,
                mapOf(
                    "balance" to FinanceUtils.round2(account.balance - amount),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userDoc(uid).collection("accounts").document(toAccount.id),
                mapOf(
                    "balance" to FinanceUtils.round2(toAccount.balance + converted),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            val isSavingsEntry = savingsType != null || CategoryCatalog.classificationOf(categoryId) == EntryClassification.SAVINGS
            var resolvedCategoryId = categoryId

            if (isSavingsEntry) {
                val effectiveType = savingsType ?: CategoryCatalog.savingsTypeOf(categoryId) ?: SavingsType.OTRO
                val nioAdded = FinanceUtils.convertCurrency(
                    amount,
                    account.currencyCode,
                    CurrencyCode.NIO,
                    exchangeRate
                )
                val linkedId = CategoryCatalog.linkedGoalId(categoryId)
                val matchedGoal = targetSavingsGoal
                    ?: existingGoals.firstOrNull { it.id == linkedId }
                    ?: existingGoals.firstOrNull { it.savingsType == effectiveType }

                val goalId = if (matchedGoal != null) {
                    batch.update(
                        userDoc(uid).collection("goals").document(matchedGoal.id),
                        mapOf(
                            "currentNio" to FinanceUtils.round2(matchedGoal.currentNio + nioAdded),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    matchedGoal.id
                } else {
                    val newGoalRef = userDoc(uid).collection("goals").document()
                    val autoName = customSavingsName?.trim()?.takeIf { it.isNotBlank() } ?: effectiveType.label
                    val newGoal = SavingsGoal(
                        userId = uid,
                        name = autoName.take(80),
                        targetNio = 1.0,
                        currentNio = FinanceUtils.round2(nioAdded),
                        iconKey = effectiveType.key,
                        deadlineIso = ""
                    )
                    batch.set(newGoalRef, newGoal.toCreateMap())
                    newGoalRef.id
                }
                resolvedCategoryId = CategoryCatalog.buildSavingsCategoryId(effectiveType, goalId)
            }

            val tx = FinanceTransaction(
                userId = uid,
                txType = txType.name,
                amount = FinanceUtils.round2(amount),
                currency = account.currency,
                accountId = account.id,
                categoryId = resolvedCategoryId,
                note = note.trim().take(160),
                dateIso = dateIso
            )
            batch.set(txRef, tx.toCreateMap())

            var netDelta = if (txType == TxType.INCOME) amount else -amount

            if (txType == TxType.INCOME && applyAutoCardDebit && existingCards.isNotEmpty()) {
                val plannedDebits = FinanceUtils.planAutoCardDebits(
                    incomeAmount = amount,
                    incomeCurrency = account.currencyCode,
                    cards = existingCards,
                    exchangeRate = exchangeRate
                )
                for (debit in plannedDebits) {
                    batch.update(
                        userDoc(uid).collection("cards").document(debit.card.id),
                        mapOf(
                            "debtBalance" to FinanceUtils.round2(debit.remainingDebtAfter),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val debitTx = FinanceTransaction(
                        userId = uid,
                        txType = TxType.EXPENSE.name,
                        amount = FinanceUtils.round2(debit.paymentInIncomeCurrency),
                        currency = account.currency,
                        accountId = account.id,
                        categoryId = "deudas",
                        note = "Débito auto → ${debit.card.alias} (•••• ${debit.card.last4})".take(160),
                        dateIso = dateIso
                    )
                    batch.set(userDoc(uid).collection("transactions").document(), debitTx.toCreateMap())
                    netDelta -= debit.paymentInIncomeCurrency
                }
            }

            batch.update(
                accRef,
                mapOf(
                    "balance" to FinanceUtils.round2(account.balance + netDelta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
    }

    suspend fun deleteTransaction(
        tx: FinanceTransaction,
        accounts: List<WalletAccount>,
        exchangeRate: Double,
        goals: List<SavingsGoal> = emptyList()
    ): Result<Unit> = safeWrite("transactions/${tx.id}", OperationType.DELETE) {
        val uid = requireUserId()
        val batch = db.batch()
        val txRef = userDoc(uid).collection("transactions").document(tx.id)
        batch.delete(txRef)

        val srcAcc = accounts.firstOrNull { it.id == tx.accountId }
        if (tx.typeEnum == TxType.TRANSFER) {
            if (srcAcc != null) {
                batch.update(
                    userDoc(uid).collection("accounts").document(srcAcc.id),
                    mapOf(
                        "balance" to FinanceUtils.round2(srcAcc.balance + tx.amount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            val dstAcc = accounts.firstOrNull { it.id == tx.toAccountId }
            if (dstAcc != null) {
                val converted = FinanceUtils.convertCurrency(
                    tx.amount,
                    tx.currencyCode,
                    dstAcc.currencyCode,
                    exchangeRate
                )
                batch.update(
                    userDoc(uid).collection("accounts").document(dstAcc.id),
                    mapOf(
                        "balance" to FinanceUtils.round2(dstAcc.balance - converted),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        } else if (srcAcc != null) {
            val revertDelta = if (tx.typeEnum == TxType.INCOME) -tx.amount else tx.amount
            batch.update(
                userDoc(uid).collection("accounts").document(srcAcc.id),
                mapOf(
                    "balance" to FinanceUtils.round2(srcAcc.balance + revertDelta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        if (CategoryCatalog.classificationOf(tx.categoryId) == EntryClassification.SAVINGS && goals.isNotEmpty()) {
            val linkedId = CategoryCatalog.linkedGoalId(tx.categoryId)
            val sType = CategoryCatalog.savingsTypeOf(tx.categoryId)
            val targetGoal = goals.firstOrNull { it.id == linkedId }
                ?: sType?.let { t -> goals.firstOrNull { it.savingsType == t } }
            if (targetGoal != null) {
                val nioRemoved = FinanceUtils.convertCurrency(tx.amount, tx.currencyCode, CurrencyCode.NIO, exchangeRate)
                val updatedNio = (targetGoal.currentNio - nioRemoved).coerceAtLeast(0.0)
                batch.update(
                    userDoc(uid).collection("goals").document(targetGoal.id),
                    mapOf(
                        "currentNio" to FinanceUtils.round2(updatedNio),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }
        batch.commit().await()
    }

    suspend fun saveReconciliation(
        account: WalletAccount,
        countedBalance: Double,
        adjustBalance: Boolean,
        note: String
    ): Result<Unit> = safeWrite("reconciliations", OperationType.CREATE) {
        val uid = requireUserId()
        val counted = FinanceUtils.round2(countedBalance)
        val diff = FinanceUtils.round2(counted - account.balance)
        val shouldAdjust = adjustBalance && kotlin.math.abs(diff) >= 0.01
        val today = FinanceUtils.todayIso()
        val cleanNote = note.trim().take(160)

        val batch = db.batch()
        val recRef = userDoc(uid).collection("reconciliations").document()
        val rec = CashReconciliation(
            userId = uid,
            dateIso = today,
            accountId = account.id,
            currency = account.currency,
            systemBalance = account.balance,
            countedBalance = counted,
            difference = diff,
            balanceAdjusted = shouldAdjust,
            note = cleanNote
        )
        batch.set(recRef, rec.toCreateMap())

        if (shouldAdjust) {
            val isSurplus = diff > 0
            val adjTx = FinanceTransaction(
                userId = uid,
                txType = if (isSurplus) TxType.INCOME.name else TxType.EXPENSE.name,
                amount = kotlin.math.abs(diff),
                currency = account.currency,
                accountId = account.id,
                categoryId = if (isSurplus) "ajuste_i" else "ajuste",
                note = cleanNote.ifBlank {
                    if (isSurplus) "Ajuste conciliación (Sobrante)" else "Ajuste conciliación (Faltante)"
                },
                dateIso = today
            )
            batch.set(userDoc(uid).collection("transactions").document(), adjTx.toCreateMap())
            batch.update(
                userDoc(uid).collection("accounts").document(account.id),
                mapOf(
                    "balance" to counted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
    }

    suspend fun addScheduledBill(
        title: String,
        amount: Double,
        currency: CurrencyCode,
        accountId: String,
        categoryId: String,
        frequency: BillFrequency,
        startDateIso: String
    ): Result<Unit> = safeWrite("scheduledBills", OperationType.CREATE) {
        val uid = requireUserId()
        val trimmed = title.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 80) { "Título inválido." }
        require(amount > 0.0) { "El monto debe ser mayor a cero." }
        require(startDateIso.length == 10) { "Fecha inválida." }

        val bill = ScheduledBill(
            userId = uid,
            title = trimmed,
            amount = FinanceUtils.round2(amount),
            currency = currency.name,
            accountId = accountId,
            categoryId = categoryId,
            frequency = frequency.name,
            startDateIso = startDateIso,
            paidDates = emptyList()
        )
        userDoc(uid).collection("scheduledBills").document().set(bill.toCreateMap()).await()
    }

    suspend fun payScheduledBill(
        bill: ScheduledBill,
        occurrenceIso: String,
        account: WalletAccount?,
        exchangeRate: Double,
        existingGoals: List<SavingsGoal> = emptyList()
    ): Result<Unit> = safeWrite("scheduledBills/${bill.id}", OperationType.UPDATE) {
        val uid = requireUserId()
        if (bill.paidDates.contains(occurrenceIso)) return@safeWrite

        val batch = db.batch()
        val updatedPaid = (bill.paidDates + occurrenceIso).distinct().takeLast(120)
        batch.update(
            userDoc(uid).collection("scheduledBills").document(bill.id),
            mapOf(
                "paidDates" to updatedPaid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        if (account != null) {
            val amtInAcc = FinanceUtils.convertCurrency(
                bill.amount,
                bill.currencyCode,
                account.currencyCode,
                exchangeRate
            )
            batch.update(
                userDoc(uid).collection("accounts").document(account.id),
                mapOf(
                    "balance" to FinanceUtils.round2(account.balance - amtInAcc),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        var resolvedCategoryId = bill.categoryId
        if (CategoryCatalog.classificationOf(bill.categoryId) == EntryClassification.SAVINGS) {
            val sType = CategoryCatalog.savingsTypeOf(bill.categoryId) ?: SavingsType.OTRO
            val linkedId = CategoryCatalog.linkedGoalId(bill.categoryId)
            val nioAdded = FinanceUtils.convertCurrency(
                bill.amount,
                bill.currencyCode,
                CurrencyCode.NIO,
                exchangeRate
            )
            val matchedGoal = existingGoals.firstOrNull { it.id == linkedId }
                ?: existingGoals.firstOrNull { it.savingsType == sType }
            val goalId = if (matchedGoal != null) {
                batch.update(
                    userDoc(uid).collection("goals").document(matchedGoal.id),
                    mapOf(
                        "currentNio" to FinanceUtils.round2(matchedGoal.currentNio + nioAdded),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                matchedGoal.id
            } else {
                val newGoalRef = userDoc(uid).collection("goals").document()
                val newGoal = SavingsGoal(
                    userId = uid,
                    name = bill.title.ifBlank { sType.label }.take(80),
                    targetNio = 1.0,
                    currentNio = FinanceUtils.round2(nioAdded),
                    iconKey = sType.key,
                    deadlineIso = ""
                )
                batch.set(newGoalRef, newGoal.toCreateMap())
                newGoalRef.id
            }
            resolvedCategoryId = CategoryCatalog.buildSavingsCategoryId(sType, goalId)
        }

        val tx = FinanceTransaction(
            userId = uid,
            txType = TxType.EXPENSE.name,
            amount = bill.amount,
            currency = bill.currency,
            accountId = account?.id ?: bill.accountId,
            categoryId = resolvedCategoryId,
            note = "Pago programado: ${bill.title}".take(160),
            dateIso = FinanceUtils.todayIso()
        )
        batch.set(userDoc(uid).collection("transactions").document(), tx.toCreateMap())
        batch.commit().await()
    }

    suspend fun deleteScheduledBill(billId: String): Result<Unit> =
        safeWrite("scheduledBills/$billId", OperationType.DELETE) {
            val uid = requireUserId()
            userDoc(uid).collection("scheduledBills").document(billId).delete().await()
        }

    suspend fun saveBudget(
        existing: CategoryBudget?,
        categoryId: String,
        limitNio: Double
    ): Result<Unit> = safeWrite("budgets", OperationType.WRITE) {
        val uid = requireUserId()
        require(limitNio > 0.0) { "El límite debe ser mayor a cero." }
        val col = userDoc(uid).collection("budgets")
        if (existing == null) {
            val budget = CategoryBudget(
                userId = uid,
                categoryId = categoryId,
                limitNio = FinanceUtils.round2(limitNio)
            )
            col.document().set(budget.toCreateMap()).await()
        } else {
            col.document(existing.id).update(
                mapOf(
                    "categoryId" to categoryId,
                    "limitNio" to FinanceUtils.round2(limitNio),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun deleteBudget(budgetId: String): Result<Unit> =
        safeWrite("budgets/$budgetId", OperationType.DELETE) {
            val uid = requireUserId()
            userDoc(uid).collection("budgets").document(budgetId).delete().await()
        }

    suspend fun addGoal(
        name: String,
        targetNio: Double,
        iconKey: String,
        deadlineIso: String,
        initialNio: Double = 0.0
    ): Result<Unit> = safeWrite("goals", OperationType.CREATE) {
        val uid = requireUserId()
        val trimmed = name.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 80) { "Nombre de ahorro inválido." }
        val normalizedTarget = if (targetNio > 1.0) FinanceUtils.round2(targetNio) else 1.0
        val normalizedInitial = if (initialNio >= 0.0) FinanceUtils.round2(initialNio) else 0.0

        val goal = SavingsGoal(
            userId = uid,
            name = trimmed,
            targetNio = normalizedTarget,
            currentNio = normalizedInitial,
            iconKey = iconKey.ifBlank { SavingsType.CAJITA.key }.take(30),
            deadlineIso = deadlineIso.take(10)
        )
        userDoc(uid).collection("goals").document().set(goal.toCreateMap()).await()
    }

    suspend fun depositToGoal(
        goal: SavingsGoal,
        account: WalletAccount,
        amountInAccountCurrency: Double,
        exchangeRate: Double
    ): Result<Unit> = safeWrite("goals/${goal.id}", OperationType.UPDATE) {
        val uid = requireUserId()
        require(amountInAccountCurrency > 0.0) { "El abono debe ser mayor a cero." }

        val nioAdded = FinanceUtils.convertCurrency(
            amountInAccountCurrency,
            account.currencyCode,
            CurrencyCode.NIO,
            exchangeRate
        )
        val batch = db.batch()
        batch.update(
            userDoc(uid).collection("goals").document(goal.id),
            mapOf(
                "currentNio" to FinanceUtils.round2(goal.currentNio + nioAdded),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            userDoc(uid).collection("accounts").document(account.id),
            mapOf(
                "balance" to FinanceUtils.round2(account.balance - amountInAccountCurrency),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        val tx = FinanceTransaction(
            userId = uid,
            txType = TxType.EXPENSE.name,
            amount = FinanceUtils.round2(amountInAccountCurrency),
            currency = account.currency,
            accountId = account.id,
            categoryId = CategoryCatalog.buildSavingsCategoryId(goal.savingsType, goal.id),
            note = "Abono a ${goal.savingsType.shortLabel}: ${goal.name}".take(160),
            dateIso = FinanceUtils.todayIso()
        )
        batch.set(userDoc(uid).collection("transactions").document(), tx.toCreateMap())
        batch.commit().await()
    }

    suspend fun recoverFromGoal(
        goal: SavingsGoal,
        account: WalletAccount,
        amountInAccountCurrency: Double,
        exchangeRate: Double
    ): Result<Unit> = safeWrite("goals/${goal.id}", OperationType.UPDATE) {
        val uid = requireUserId()
        require(amountInAccountCurrency > 0.0) { "El monto debe ser mayor a cero." }

        val nioRecovered = FinanceUtils.convertCurrency(
            amountInAccountCurrency,
            account.currencyCode,
            CurrencyCode.NIO,
            exchangeRate
        )
        val updatedNio = (goal.currentNio - nioRecovered).coerceAtLeast(0.0)

        val batch = db.batch()
        batch.update(
            userDoc(uid).collection("goals").document(goal.id),
            mapOf(
                "currentNio" to FinanceUtils.round2(updatedNio),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            userDoc(uid).collection("accounts").document(account.id),
            mapOf(
                "balance" to FinanceUtils.round2(account.balance + amountInAccountCurrency),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        val tx = FinanceTransaction(
            userId = uid,
            txType = TxType.INCOME.name,
            amount = FinanceUtils.round2(amountInAccountCurrency),
            currency = account.currency,
            accountId = account.id,
            categoryId = "otros_i",
            note = if (goal.savingsType == SavingsType.PRESTAMO) {
                "Recuperación de préstamo: ${goal.name}".take(160)
            } else {
                "Retiro de ahorro (${goal.savingsType.shortLabel}): ${goal.name}".take(160)
            },
            dateIso = FinanceUtils.todayIso()
        )
        batch.set(userDoc(uid).collection("transactions").document(), tx.toCreateMap())
        batch.commit().await()
    }

    suspend fun deleteGoal(goalId: String): Result<Unit> =
        safeWrite("goals/$goalId", OperationType.DELETE) {
            val uid = requireUserId()
            userDoc(uid).collection("goals").document(goalId).delete().await()
        }

    suspend fun saveCard(
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
    ): Result<Unit> = safeWrite("cards", OperationType.WRITE) {
        val uid = requireUserId()
        val trimmedAlias = alias.trim()
        require(trimmedAlias.isNotEmpty() && trimmedAlias.length <= 60) { "El alias de la tarjeta es obligatorio." }
        val cleanLast4 = last4.filter { it.isDigit() }.takeLast(4).padStart(4, '0')
        require(cleanLast4.length == 4) { "Ingresa los últimos 4 dígitos." }
        val safeDebt = FinanceUtils.round2(debtBalance.coerceAtLeast(0.0))
        val safeLimit = FinanceUtils.round2(creditLimit.coerceAtLeast(0.0))
        val safeAutoVal = FinanceUtils.round2(autoDebitValue.coerceAtLeast(0.0))
        val safeCutoff = cutoffDay.coerceIn(1, 31)
        val safeDue = dueDay.coerceIn(1, 31)
        val col = userDoc(uid).collection("cards")

        if (editCard == null) {
            val card = BankCard(
                userId = uid,
                alias = trimmedAlias,
                bankName = bankName.trim().take(40),
                cardKind = cardKind.name,
                network = network.name,
                last4 = cleanLast4,
                currency = currency.name,
                debtBalance = safeDebt,
                creditLimit = safeLimit,
                linkedAccountId = linkedAccountId.take(128),
                autoDebitEnabled = autoDebitEnabled,
                autoDebitMode = autoDebitMode.name,
                autoDebitValue = safeAutoVal,
                googleWalletLinked = googleWalletLinked,
                cardFrozen = cardFrozen,
                cutoffDay = safeCutoff,
                dueDay = safeDue,
                colorTheme = colorTheme.key
            )
            col.document().set(card.toCreateMap()).await()
        } else {
            col.document(editCard.id).update(
                mapOf(
                    "alias" to trimmedAlias,
                    "bankName" to bankName.trim().take(40),
                    "cardKind" to cardKind.name,
                    "network" to network.name,
                    "last4" to cleanLast4,
                    "currency" to currency.name,
                    "debtBalance" to safeDebt,
                    "creditLimit" to safeLimit,
                    "linkedAccountId" to linkedAccountId.take(128),
                    "autoDebitEnabled" to autoDebitEnabled,
                    "autoDebitMode" to autoDebitMode.name,
                    "autoDebitValue" to safeAutoVal,
                    "googleWalletLinked" to googleWalletLinked,
                    "cardFrozen" to cardFrozen,
                    "cutoffDay" to safeCutoff,
                    "dueDay" to safeDue,
                    "colorTheme" to colorTheme.key,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun updateCardFlags(
        cardId: String,
        autoDebitEnabled: Boolean? = null,
        googleWalletLinked: Boolean? = null,
        cardFrozen: Boolean? = null
    ): Result<Unit> = safeWrite("cards/$cardId", OperationType.UPDATE) {
        val uid = requireUserId()
        val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
        autoDebitEnabled?.let { updates["autoDebitEnabled"] = it }
        googleWalletLinked?.let { updates["googleWalletLinked"] = it }
        cardFrozen?.let { updates["cardFrozen"] = it }
        if (updates.size > 1) {
            userDoc(uid).collection("cards").document(cardId).update(updates).await()
        }
    }

    suspend fun deleteCard(cardId: String): Result<Unit> =
        safeWrite("cards/$cardId", OperationType.DELETE) {
            val uid = requireUserId()
            require(cardId.isNotBlank())
            userDoc(uid).collection("cards").document(cardId).delete().await()
        }

    suspend fun recordCardCharge(
        card: BankCard,
        amountInCardCurrency: Double,
        categoryId: String,
        note: String,
        dateIso: String,
        linkedAccount: WalletAccount?,
        fallbackAccount: WalletAccount?,
        exchangeRate: Double
    ): Result<Unit> = safeWrite("cards/${card.id}", OperationType.UPDATE) {
        val uid = requireUserId()
        require(!card.cardFrozen) { "Esta tarjeta está congelada. Descongélala para registrar compras." }
        require(amountInCardCurrency > 0.0) { "El monto debe ser mayor a cero." }
        require(dateIso.length == 10) { "Fecha inválida." }

        val batch = db.batch()
        val cleanAmt = FinanceUtils.round2(amountInCardCurrency)
        val cleanNote = note.trim().ifBlank { "Compra con ${card.alias} (•••• ${card.last4})" }.take(160)

        if (card.cardKindEnum == CardKind.DEBIT) {
            requireNotNull(linkedAccount) { "Vinculá una cuenta a la tarjeta de débito para registrar la compra." }
            val deductInAcc = FinanceUtils.round2(
                FinanceUtils.convertCurrency(cleanAmt, card.currencyCode, linkedAccount.currencyCode, exchangeRate)
            )
            batch.update(
                userDoc(uid).collection("accounts").document(linkedAccount.id),
                mapOf(
                    "balance" to FinanceUtils.round2(linkedAccount.balance - deductInAcc),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val tx = FinanceTransaction(
                userId = uid,
                txType = TxType.EXPENSE.name,
                amount = deductInAcc,
                currency = linkedAccount.currency,
                accountId = linkedAccount.id,
                categoryId = categoryId,
                note = cleanNote,
                dateIso = dateIso
            )
            batch.set(userDoc(uid).collection("transactions").document(), tx.toCreateMap())
        } else {
            batch.update(
                userDoc(uid).collection("cards").document(card.id),
                mapOf(
                    "debtBalance" to FinanceUtils.round2(card.debtBalance + cleanAmt),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val refAcc = linkedAccount ?: fallbackAccount
            if (refAcc != null) {
                val tx = FinanceTransaction(
                    userId = uid,
                    txType = TxType.EXPENSE.name,
                    amount = cleanAmt,
                    currency = card.currency,
                    accountId = refAcc.id,
                    categoryId = categoryId,
                    note = cleanNote,
                    dateIso = dateIso
                )
                batch.set(userDoc(uid).collection("transactions").document(), tx.toCreateMap())
            }
        }
        batch.commit().await()
    }

    suspend fun payCardDebt(
        card: BankCard,
        payAccount: WalletAccount,
        amountInAccountCurrency: Double,
        exchangeRate: Double
    ): Result<Unit> = safeWrite("cards/${card.id}", OperationType.UPDATE) {
        val uid = requireUserId()
        require(amountInAccountCurrency > 0.0) { "El abono debe ser mayor a cero." }

        val cleanPayAcc = FinanceUtils.round2(amountInAccountCurrency)
        val payInCardCurrency = FinanceUtils.round2(
            FinanceUtils.convertCurrency(cleanPayAcc, payAccount.currencyCode, card.currencyCode, exchangeRate)
        )
        val remainingDebt = FinanceUtils.round2((card.debtBalance - payInCardCurrency).coerceAtLeast(0.0))

        val batch = db.batch()
        batch.update(
            userDoc(uid).collection("cards").document(card.id),
            mapOf(
                "debtBalance" to remainingDebt,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            userDoc(uid).collection("accounts").document(payAccount.id),
            mapOf(
                "balance" to FinanceUtils.round2(payAccount.balance - cleanPayAcc),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        val tx = FinanceTransaction(
            userId = uid,
            txType = TxType.EXPENSE.name,
            amount = cleanPayAcc,
            currency = payAccount.currency,
            accountId = payAccount.id,
            categoryId = "deudas",
            note = "Pago tarjeta ${card.alias} (•••• ${card.last4})".take(160),
            dateIso = FinanceUtils.todayIso()
        )
        batch.set(userDoc(uid).collection("transactions").document(), tx.toCreateMap())
        batch.commit().await()
    }

    suspend fun registerDeviceAndSyncRelease(
        deviceId: String,
        deviceName: String,
        versionName: String,
        versionCode: Int
    ): Result<Unit> = safeWrite("devices/$deviceId", OperationType.WRITE) {
        val uid = requireUserId()
        val safeId = deviceId.replace(Regex("[^a-zA-Z0-9_-]"), "").take(64).ifBlank { "android_default" }
        val safeName = deviceName.trim().ifBlank { "Teléfono Android" }.take(80)
        val safeVerName = versionName.trim().ifBlank { "1.0" }.take(24)
        val safeVerCode = versionCode.coerceIn(1, 1_000_000)
        val today = FinanceUtils.todayIso()

        val devRef = userDoc(uid).collection("devices").document(safeId)
        val devSnap = devRef.get().await()
        if (!devSnap.exists()) {
            val dev = SyncedDevice(
                userId = uid,
                deviceName = safeName,
                versionName = safeVerName,
                versionCode = safeVerCode,
                platform = "ANDROID",
                lastSeenIso = today
            )
            devRef.set(dev.toCreateMap()).await()
        } else {
            devRef.update(
                mapOf(
                    "deviceName" to safeName,
                    "versionName" to safeVerName,
                    "versionCode" to safeVerCode,
                    "platform" to "ANDROID",
                    "lastSeenIso" to today,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val releaseRef = userDoc(uid).collection("appMeta").document("release")
        val releaseSnap = releaseRef.get().await()
        if (!releaseSnap.exists()) {
            val initRelease = AppReleaseInfo(
                userId = uid,
                latestVersionCode = safeVerCode,
                latestVersionName = safeVerName,
                downloadUrl = "",
                releaseNotes = "Sincronización activa en la nube para todos tus teléfonos",
                forceNotice = false
            )
            releaseRef.set(initRelease.toCreateMap()).await()
        } else {
            val currentRemote = (releaseSnap.getLong("latestVersionCode") ?: 1L).toInt()
            if (safeVerCode > currentRemote) {
                releaseRef.update(
                    mapOf(
                        "latestVersionCode" to safeVerCode,
                        "latestVersionName" to safeVerName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    suspend fun publishAppUpdateChannel(
        latestVersionCode: Int,
        latestVersionName: String,
        downloadUrl: String,
        releaseNotes: String,
        forceNotice: Boolean
    ): Result<Unit> = safeWrite("appMeta/release", OperationType.WRITE) {
        val uid = requireUserId()
        val cleanUrl = downloadUrl.trim().take(400)
        require(cleanUrl.isEmpty() || cleanUrl.startsWith("https://")) {
            "El enlace de descarga debe iniciar con https://"
        }
        val safeVerCode = latestVersionCode.coerceIn(1, 1_000_000)
        val safeVerName = latestVersionName.trim().ifBlank { "1.0" }.take(24)
        val safeNotes = releaseNotes.trim().take(240)
        val releaseRef = userDoc(uid).collection("appMeta").document("release")
        val snap = releaseRef.get().await()
        if (!snap.exists()) {
            val model = AppReleaseInfo(
                userId = uid,
                latestVersionCode = safeVerCode,
                latestVersionName = safeVerName,
                downloadUrl = cleanUrl,
                releaseNotes = safeNotes,
                forceNotice = forceNotice
            )
            releaseRef.set(model.toCreateMap()).await()
        } else {
            releaseRef.update(
                mapOf(
                    "latestVersionCode" to safeVerCode,
                    "latestVersionName" to safeVerName,
                    "downloadUrl" to cleanUrl,
                    "releaseNotes" to safeNotes,
                    "forceNotice" to forceNotice,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun removeSyncedDevice(deviceId: String): Result<Unit> =
        safeWrite("devices/$deviceId", OperationType.DELETE) {
            val uid = requireUserId()
            require(deviceId.isNotBlank())
            userDoc(uid).collection("devices").document(deviceId).delete().await()
        }

    suspend fun seedDemoData(): Result<Unit> = safeWrite("users", OperationType.WRITE) {
        val uid = requireUserId()
        clearSubcollectionsInternal(uid)
        val today = FinanceUtils.todayIso()
        val ym = today.take(7)
        val prevYm = FinanceUtils.addDaysIso("$ym-01", -15).take(7)

        val batch = db.batch()
        val cashRef = userDoc(uid).collection("accounts").document()
        val bankRef = userDoc(uid).collection("accounts").document()
        val usdRef = userDoc(uid).collection("accounts").document()
        val cardRef = userDoc(uid).collection("accounts").document()

        batch.set(cashRef, WalletAccount(userId = uid, name = "Efectivo en Cartera", accountType = AccountType.CASH.name, currency = CurrencyCode.NIO.name, balance = 4850.0, iconKey = "cash").toCreateMap())
        batch.set(bankRef, WalletAccount(userId = uid, name = "Banco Principal (C$)", accountType = AccountType.BANK.name, currency = CurrencyCode.NIO.name, balance = 18400.0, iconKey = "bank").toCreateMap())
        batch.set(usdRef, WalletAccount(userId = uid, name = "Ahorro Dólares ($)", accountType = AccountType.SAVINGS.name, currency = CurrencyCode.USD.name, balance = 450.0, iconKey = "savings").toCreateMap())
        batch.set(cardRef, WalletAccount(userId = uid, name = "Tarjeta de Crédito", accountType = AccountType.CREDIT.name, currency = CurrencyCode.NIO.name, balance = -2100.0, iconKey = "credit").toCreateMap())

        val txs = listOf(
            FinanceTransaction(userId = uid, txType = TxType.INCOME.name, amount = 24000.0, currency = CurrencyCode.NIO.name, accountId = bankRef.id, categoryId = "salario", note = "Salario del mes", dateIso = "$ym-01"),
            FinanceTransaction(userId = uid, txType = TxType.EXPENSE.name, amount = 6500.0, currency = CurrencyCode.NIO.name, accountId = bankRef.id, categoryId = "vivienda", note = "Alquiler / Vivienda", dateIso = "$ym-03"),
            FinanceTransaction(userId = uid, txType = TxType.EXPENSE.name, amount = 2150.0, currency = CurrencyCode.NIO.name, accountId = cashRef.id, categoryId = "comida", note = "Supermercado semanal", dateIso = "$ym-06"),
            FinanceTransaction(userId = uid, txType = TxType.EXPENSE.name, amount = 980.0, currency = CurrencyCode.NIO.name, accountId = bankRef.id, categoryId = "servicios", note = "Energía eléctrica", dateIso = "$ym-09"),
            FinanceTransaction(userId = uid, txType = TxType.INCOME.name, amount = 120.0, currency = CurrencyCode.USD.name, accountId = usdRef.id, categoryId = "freelance", note = "Proyecto extra", dateIso = "$ym-11"),
            FinanceTransaction(userId = uid, txType = TxType.EXPENSE.name, amount = 650.0, currency = CurrencyCode.NIO.name, accountId = cashRef.id, categoryId = "transporte", note = "Combustible", dateIso = today),
            FinanceTransaction(userId = uid, txType = TxType.EXPENSE.name, amount = 1400.0, currency = CurrencyCode.NIO.name, accountId = cardRef.id, categoryId = "ocio", note = "Cena y cine", dateIso = FinanceUtils.addDaysIso(today, -2)),
            FinanceTransaction(userId = uid, txType = TxType.INCOME.name, amount = 22500.0, currency = CurrencyCode.NIO.name, accountId = bankRef.id, categoryId = "salario", note = "Salario mes anterior", dateIso = "$prevYm-01"),
            FinanceTransaction(userId = uid, txType = TxType.EXPENSE.name, amount = 11800.0, currency = CurrencyCode.NIO.name, accountId = bankRef.id, categoryId = "vivienda", note = "Gastos mes anterior", dateIso = "$prevYm-05")
        )
        txs.forEach { batch.set(userDoc(uid).collection("transactions").document(), it.toCreateMap()) }

        val bills = listOf(
            ScheduledBill(userId = uid, title = "Internet Fibra Óptica", amount = 1150.0, currency = CurrencyCode.NIO.name, accountId = bankRef.id, categoryId = "servicios", frequency = BillFrequency.MONTHLY.name, startDateIso = "$ym-22"),
            ScheduledBill(userId = uid, title = "Agua Potable", amount = 340.0, currency = CurrencyCode.NIO.name, accountId = cashRef.id, categoryId = "servicios", frequency = BillFrequency.MONTHLY.name, startDateIso = "$ym-26"),
            ScheduledBill(userId = uid, title = "Corte Tarjeta Crédito", amount = 2100.0, currency = CurrencyCode.NIO.name, accountId = bankRef.id, categoryId = "deudas", frequency = BillFrequency.MONTHLY.name, startDateIso = FinanceUtils.addDaysIso(today, 2)),
            ScheduledBill(userId = uid, title = "Suscripción Streaming", amount = 11.0, currency = CurrencyCode.USD.name, accountId = usdRef.id, categoryId = "ocio", frequency = BillFrequency.MONTHLY.name, startDateIso = "$ym-05", paidDates = listOf("$ym-05"))
        )
        bills.forEach { batch.set(userDoc(uid).collection("scheduledBills").document(), it.toCreateMap()) }

        val budgets = listOf(
            CategoryBudget(userId = uid, categoryId = "comida", limitNio = 6000.0),
            CategoryBudget(userId = uid, categoryId = "servicios", limitNio = 3000.0),
            CategoryBudget(userId = uid, categoryId = "transporte", limitNio = 2500.0),
            CategoryBudget(userId = uid, categoryId = "ocio", limitNio = 2000.0)
        )
        budgets.forEach { batch.set(userDoc(uid).collection("budgets").document(), it.toCreateMap()) }

        val goals = listOf(
            SavingsGoal(userId = uid, name = "Cajita de Emergencia", targetNio = 15000.0, currentNio = 4500.0, iconKey = SavingsType.CAJITA.key, deadlineIso = ""),
            SavingsGoal(userId = uid, name = "Cuenta Ahorro LAFISE", targetNio = 30000.0, currentNio = 18500.0, iconKey = SavingsType.LAFISE.key, deadlineIso = "2027-06-30"),
            SavingsGoal(userId = uid, name = "Préstamo a Carlos (Ahorro)", targetNio = 1.0, currentNio = 3200.0, iconKey = SavingsType.PRESTAMO.key, deadlineIso = "")
        )
        goals.forEach { batch.set(userDoc(uid).collection("goals").document(), it.toCreateMap()) }

        val rec = CashReconciliation(
            userId = uid,
            dateIso = FinanceUtils.addDaysIso(today, -5),
            accountId = cashRef.id,
            currency = CurrencyCode.NIO.name,
            systemBalance = 4900.0,
            countedBalance = 4850.0,
            difference = -50.0,
            balanceAdjusted = true,
            note = "Faltante menor de cambio"
        )
        batch.set(userDoc(uid).collection("reconciliations").document(), rec.toCreateMap())

        val demoCards = listOf(
            BankCard(
                userId = uid,
                alias = "BAC Cashback Oro",
                bankName = "BAC Credomatic",
                cardKind = CardKind.CREDIT.name,
                network = CardNetwork.VISA.name,
                last4 = "4821",
                currency = CurrencyCode.NIO.name,
                debtBalance = 2100.0,
                creditLimit = 18000.0,
                linkedAccountId = bankRef.id,
                autoDebitEnabled = true,
                autoDebitMode = AutoDebitMode.FULL_DEBT.name,
                autoDebitValue = 100.0,
                googleWalletLinked = true,
                cardFrozen = false,
                cutoffDay = 15,
                dueDay = 5,
                colorTheme = CardColorTheme.GOLD.key
            ),
            BankCard(
                userId = uid,
                alias = "LAFISE Débito",
                bankName = "Banco LAFISE",
                cardKind = CardKind.DEBIT.name,
                network = CardNetwork.MASTERCARD.name,
                last4 = "9134",
                currency = CurrencyCode.NIO.name,
                debtBalance = 0.0,
                creditLimit = 0.0,
                linkedAccountId = bankRef.id,
                autoDebitEnabled = false,
                autoDebitMode = AutoDebitMode.MANUAL.name,
                autoDebitValue = 0.0,
                googleWalletLinked = true,
                cardFrozen = false,
                cutoffDay = 30,
                dueDay = 1,
                colorTheme = CardColorTheme.DORA_BLUE.key
            )
        )
        demoCards.forEach { batch.set(userDoc(uid).collection("cards").document(), it.toCreateMap()) }

        batch.commit().await()
    }

    suspend fun clearAllUserData(): Result<Unit> = safeWrite("users", OperationType.DELETE) {
        val uid = requireUserId()
        clearSubcollectionsInternal(uid)
        val batch = db.batch()
        batch.set(
            userDoc(uid).collection("accounts").document(),
            WalletAccount(userId = uid, name = "Efectivo en Cartera", accountType = AccountType.CASH.name, currency = CurrencyCode.NIO.name, balance = 0.0, iconKey = "cash").toCreateMap()
        )
        batch.set(
            userDoc(uid).collection("accounts").document(),
            WalletAccount(userId = uid, name = "Banco Principal", accountType = AccountType.BANK.name, currency = CurrencyCode.NIO.name, balance = 0.0, iconKey = "bank").toCreateMap()
        )
        batch.commit().await()
    }

    private suspend fun clearSubcollectionsInternal(uid: String) {
        val cols = listOf("accounts", "transactions", "scheduledBills", "budgets", "goals", "reconciliations", "cards")
        val batch = db.batch()
        var ops = 0
        for (c in cols) {
            val snap = userDoc(uid).collection(c).get().await()
            for (doc in snap.documents) {
                batch.delete(doc.reference)
                ops++
            }
        }
        if (ops > 0) batch.commit().await()
    }

    private suspend inline fun safeWrite(
        path: String,
        op: OperationType,
        crossinline block: suspend () -> Unit
    ): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, op, path)
        Result.failure(e)
    }
}
