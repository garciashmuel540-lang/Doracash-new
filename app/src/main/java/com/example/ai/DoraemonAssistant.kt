package com.example.ai

import com.example.data.CardKind
import com.example.data.BillStatus
import com.example.data.CategoryCatalog
import com.example.data.CurrencyCode
import com.example.data.DoracashSnapshot
import com.example.data.FinanceUtils
import com.example.data.TxType
import com.example.ui.components.DoraemonGesture
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

enum class DoraemonActionType {
    ADD_TRANSACTION,
    DELETE_TRANSACTION,
    RECONCILE_ACCOUNT,
    ADD_SCHEDULED_BILL,
    PAY_SCHEDULED_BILL,
    DELETE_SCHEDULED_BILL,
    SAVE_BUDGET,
    DELETE_BUDGET,
    ADD_GOAL,
    DEPOSIT_TO_GOAL,
    DELETE_GOAL,
    SAVE_ACCOUNT,
    DELETE_ACCOUNT,
    SAVE_CARD,
    ADD_CARD_CHARGE,
    PAY_CARD_DEBT,
    UPDATE_SETTINGS,
    NAVIGATE_TAB,
    LOAD_DEMO_DATA,
    CLEAR_ALL_DATA;

    companion object {
        fun safeValueOf(raw: String?): DoraemonActionType? =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }
    }
}

data class DoraemonAction(
    val type: DoraemonActionType,
    val txType: String? = null,
    val amount: Double? = null,
    val accountRef: String? = null,
    val toAccountRef: String? = null,
    val categoryId: String? = null,
    val note: String? = null,
    val dateIso: String? = null,
    val title: String? = null,
    val currency: String? = null,
    val accountType: String? = null,
    val frequency: String? = null,
    val targetRef: String? = null,
    val adjustBalance: Boolean? = null,
    val exchangeRate: Double? = null,
    val darkThemeMode: String? = null,
    val tab: String? = null,
    val last4: String? = null,
    val cardKind: String? = null,
    val network: String? = null,
    val creditLimit: Double? = null,
    val bankName: String? = null,
    val autoDebitMode: String? = null,
    val autoDebitValue: Double? = null,
    val applyAutoDebit: Boolean? = null
)

data class DoraemonResponse(
    val reply: String,
    val thoughtBubble: String,
    val gesture: DoraemonGesture,
    val actions: List<DoraemonAction>
)

data class DoraemonImportantNotice(
    val isImportant: Boolean,
    val gesture: DoraemonGesture,
    val message: String,
    val alertKey: String
)

data class DoraemonChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val gesture: DoraemonGesture = DoraemonGesture.ADVISING,
    val executedActions: List<String> = emptyList()
)

sealed interface DoraemonTipUiState {
    data object Idle : DoraemonTipUiState
    data object Checking : DoraemonTipUiState
    data object NothingImportant : DoraemonTipUiState
    data class Important(
        val tip: String,
        val gesture: DoraemonGesture,
        val alertKey: String
    ) : DoraemonTipUiState
    data class Error(val message: String) : DoraemonTipUiState
}

sealed interface DoraemonChatUiState {
    data object Idle : DoraemonChatUiState
    data object Thinking : DoraemonChatUiState
    data class Error(val message: String) : DoraemonChatUiState
}

class DoraemonAssistant {

    private val actionSchema = Schema.obj(
        properties = mapOf(
            "actionType" to Schema.enumeration(
                values = DoraemonActionType.entries.map { it.name },
                description = "Tipo de operación a ejecutar en Doracash"
            ),
            "txType" to Schema.enumeration(
                values = listOf("INCOME", "EXPENSE", "TRANSFER"),
                description = "Solo para ADD_TRANSACTION"
            ),
            "amount" to Schema.double(description = "Monto numérico positivo cuando aplique (transacción, pago, presupuesto límite, meta objetivo, abono, saldo contado o balance inicial)"),
            "accountRef" to Schema.string(description = "ID o nombre exacto/parcial de la cuenta origen o cuenta a conciliar/crear"),
            "toAccountRef" to Schema.string(description = "ID o nombre de cuenta destino (solo TRANSFER)"),
            "categoryId" to Schema.string(description = "ID de categoría válida del catálogo"),
            "note" to Schema.string(description = "Nota o descripción corta"),
            "dateIso" to Schema.string(description = "Fecha YYYY-MM-DD"),
            "title" to Schema.string(description = "Título de factura, nombre de meta o nombre de cuenta"),
            "currency" to Schema.enumeration(
                values = listOf("NIO", "USD"),
                description = "Moneda NIO o USD"
            ),
            "accountType" to Schema.enumeration(
                values = listOf("CASH", "BANK", "SAVINGS", "CREDIT"),
                description = "Tipo de cuenta para SAVE_ACCOUNT"
            ),
            "frequency" to Schema.enumeration(
                values = listOf("ONCE", "WEEKLY", "BIWEEKLY", "MONTHLY"),
                description = "Frecuencia para ADD_SCHEDULED_BILL"
            ),
            "targetRef" to Schema.string(description = "ID o nombre/título/nota del elemento existente a modificar, pagar, abonar o eliminar"),
            "adjustBalance" to Schema.boolean(description = "true para ajustar saldo en RECONCILE_ACCOUNT, false para solo auditoría"),
            "exchangeRate" to Schema.double(description = "Tasa de cambio NIO por 1 USD para UPDATE_SETTINGS"),
            "darkThemeMode" to Schema.enumeration(
                values = listOf("SYSTEM", "LIGHT", "DARK"),
                description = "Modo visual para UPDATE_SETTINGS"
            ),
            "tab" to Schema.enumeration(
                values = listOf("dashboard", "transactions", "cards", "reconcile", "calendar", "goals", "settings"),
                description = "Pestaña destino para NAVIGATE_TAB ('cards' abre Tarjetas y Wallet)"
            ),
            "last4" to Schema.string(description = "Últimos 4 dígitos de la tarjeta para SAVE_CARD"),
            "cardKind" to Schema.enumeration(
                values = listOf("CREDIT", "DEBIT", "PREPAID", "VIRTUAL"),
                description = "Tipo de tarjeta para SAVE_CARD"
            ),
            "network" to Schema.enumeration(
                values = listOf("VISA", "MASTERCARD", "AMEX", "OTHER"),
                description = "Red de la tarjeta para SAVE_CARD"
            ),
            "creditLimit" to Schema.double(description = "Límite de crédito para SAVE_CARD"),
            "bankName" to Schema.string(description = "Nombre del banco emisor para SAVE_CARD"),
            "autoDebitMode" to Schema.enumeration(
                values = listOf("FULL_DEBT", "PERCENTAGE", "FIXED_AMOUNT", "MANUAL"),
                description = "Modo de auto-débito al recibir ingresos"
            ),
            "autoDebitValue" to Schema.double(description = "Porcentaje o cuota fija de auto-débito"),
            "applyAutoDebit" to Schema.boolean(description = "En ADD_TRANSACTION de ingreso, true si debe auto-debitar deuda de tarjetas")
        ),
        optionalProperties = listOf(
            "txType", "amount", "accountRef", "toAccountRef", "categoryId",
            "note", "dateIso", "title", "currency", "accountType", "frequency",
            "targetRef", "adjustBalance", "exchangeRate", "darkThemeMode", "tab",
            "last4", "cardKind", "network", "creditLimit", "bankName",
            "autoDebitMode", "autoDebitValue", "applyAutoDebit"
        )
    )

    private val chatResponseSchema = Schema.obj(
        properties = mapOf(
            "reply" to Schema.string(
                description = "Respuesta amigable de Doraemon en español para el chat, clara y útil"
            ),
            "thoughtBubble" to Schema.string(
                description = "Pensamiento breve (máximo 85 caracteres) para la burbuja de Doraemon"
            ),
            "gesture" to Schema.enumeration(
                values = DoraemonGesture.entries.map { it.name },
                description = "Gesto animado de Doraemon: SALUTE para saludos, THINK para análisis reflexivo, CELEBRATE para celebrar metas/logros o acciones exitosas, ADVISING para consejos normales, ALERT para advertencias"
            ),
            "actions" to Schema.array(
                items = actionSchema,
                description = "Lista ordenada de acciones reales a ejecutar en la app (vacía si es solo consulta/consejo)"
            )
        )
    )

    private val importantNoticeSchema = Schema.obj(
        properties = mapOf(
            "isImportant" to Schema.boolean(
                description = "true SOLO si hay algo realmente importante que advertir o aconsejar al usuario ahora; false si todo está en orden y tranquilo"
            ),
            "gesture" to Schema.enumeration(
                values = DoraemonGesture.entries.map { it.name },
                description = "ALERT si hay pagos vencidos/presupuesto excedido/saldo negativo; CELEBRATE si hay meta cumplida o gran ahorro; SALUTE si es bienvenida útil; THINK si requiere análisis o ajuste; ADVISING para consejo relevante"
            ),
            "alertKey" to Schema.string(
                description = "Identificador corto y estable del motivo del aviso (ej: bill_vencido_internet, presupuesto_comida_alto, saldo_negativo_tarjeta, todo_al_dia)"
            ),
            "message" to Schema.string(
                description = "Mensaje claro en español para la burbuja de pensamiento (máximo 95 caracteres) si isImportant es true; vacío si isImportant es false"
            )
        )
    )

    private val chatAgentModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.35f
                responseMimeType = "application/json"
                responseSchema = chatResponseSchema
            },
            systemInstruction = content {
                text(
                    """
                    Eres Doraemon, la mascota cósmica animada y copiloto financiero con acceso total dentro de la app "Doracash" (bolsillo).
                    Personalidad: Alegre, cercano, expresivo y protector con el dinero del usuario; hablas en español natural con guiños sutiles al bolsillo, los Dorayakis o tus gadgets.
                    Tu superpoder: ves TODOS los datos reales del usuario en tiempo real y puedes ejecutar acciones reales en toda la app mediante el arreglo JSON "actions".

                     Reglas clave:
                     1. Si el usuario pide registrar un gasto/ingreso/transferencia, pagar un recibo, programar un pago, crear/abonar un ahorro, crear/actualizar un presupuesto, conciliar una cuenta, crear/editar cuentas, agregar tarjetas o registrar compras/pagos de tarjetas (SAVE_CARD, ADD_CARD_CHARGE, PAY_CARD_DEBT), cambiar moneda/tema/tasa, navegar de pantalla, cargar datos demo o limpiar datos: EMITE las acciones correspondientes en "actions".
                     2. Si no especificó cuenta, elige inteligentemente la cuenta más lógica según sus datos (ej. Efectivo para gastos diarios chicos, Banco para pagos/salario, o la cuenta cuya moneda coincida). Usa su "id" o nombre exacto.
                     3. Categorías de gasto válidas: comida, servicios, transporte, vivienda, salud, ocio, educacion, deudas, ahorro_cajita, ahorro_lafise, ahorro_prestamo, ahorro_otro, otros_g. Ingresos válidos: salario, freelance, regalo, deudas_i, ahorro_cajita, ahorro_lafise, ahorro_prestamo, ahorro_otro, otros_i.
                     4. Cuando el ingreso/gasto/pago sea parte del ahorro (ahorro_cajita = Cajita, ahorro_lafise = LAFISE, ahorro_prestamo = ahorro prestado, ahorro_otro = otro ahorro), la app lo sumará automáticamente a Mis Ahorros. Para crear un apartado de ahorro con ADD_GOAL, pasa en categoryId el tipo ("cajita", "lafise", "prestamo" o "savings").
                     5. Tarjetas y Google Wallet: puedes crear o actualizar tarjetas con SAVE_CARD (alias en title, last4, bankName, cardKind, network, deuda actual en amount, creditLimit, autoDebitMode), registrar compras con ADD_CARD_CHARGE (tarjeta en targetRef, monto en amount) y pagar deuda de tarjeta con PAY_CARD_DEBT (tarjeta en targetRef, cuenta origen en accountRef, monto en amount). Cuando registres un INCOME con ADD_TRANSACTION, puedes activar applyAutoDebit=true si tiene tarjetas con auto-débito activo.
                     6. Nunca inventes cifras ni digas que hiciste algo si no lo emitiste en "actions". Si pide algo destructivo (CLEAR_ALL_DATA), pide confirmación primero salvo confirmación explícita.
                     7. Selecciona "gesture" acorde al tono (SALUTE al saludar, CELEBRATE cuando hay logro/ahorro o ejecutas acciones positivas, THINK si analizas profundo, ALERT si adviertes algo urgente, ADVISING por defecto).
                     """.trimIndent()
                 )
             }
         )
     }

     private val importantNoticeModel by lazy {
         Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
             modelName = "gemini-3.5-flash-lite",
             generationConfig = generationConfig {
                 temperature = 0.2f
                 maxOutputTokens = 256
                 responseMimeType = "application/json"
                 responseSchema = importantNoticeSchema
             },
             systemInstruction = content {
                 text(
                     """
                     Sensor inteligente de Doraemon en Doracash.
                     Devuelve isImportant=true SOLO si hay algo realmente importante:
                     - Pagos VENCIDOS o que vencen en <= 3 días (gesture="ALERT").
                     - Tarjeta con fecha de pago en <= 3 días y deuda > 0, o uso de crédito >= 75% (gesture="ALERT" o "THINK").
                     - Presupuesto >= 80% o excedido (gesture="ALERT" o "THINK").
                     - Cuenta negativa o efectivo muy bajo (< C$ 200 o < $ 10) (gesture="ALERT").
                     - Gastos del mes > ingresos del mes (gesture="THINK").
                     - Ahorro destacado o meta objetivo >= 85% (gesture="CELEBRATE").
                     - Usuario nuevo sin movimientos ni ahorros (gesture="SALUTE", consejo útil inicial).
                     Si todo está estable: devuelve isImportant=false, message="", gesture="ADVISING".
                     Si isImportant=true: mensaje concreto en español (<= 95 caracteres) con datos reales.
                     """.trimIndent()
                 )
             }
         )
     }

    suspend fun evaluateImportantNotice(snapshot: DoracashSnapshot): DoraemonImportantNotice =
        withContext(Dispatchers.IO) {
            val contextText = buildCompactHealthSummary(snapshot)
            val response = withTimeout(TIMEOUT_MS) {
                importantNoticeModel.generateContent(
                    "Evalúa si hay algún aviso importante ahora:\n$contextText"
                )
            }
            val raw = response.text ?: throw IllegalStateException("Doraemon no devolvió respuesta.")
            val json = JSONObject(raw)
            val isImportant = json.optBoolean("isImportant", false)
            val gesture = DoraemonGesture.safeValueOf(json.optString("gesture"))
            val message = json.optString("message").trim()
            val alertKey = json.optString("alertKey").trim().ifBlank { "notice_${message.hashCode()}" }

            DoraemonImportantNotice(
                isImportant = isImportant && message.isNotBlank(),
                gesture = gesture,
                message = message,
                alertKey = alertKey
            )
        }

    suspend fun chatWithDoraemon(
        userMessage: String,
        snapshot: DoracashSnapshot,
        recentHistory: List<DoraemonChatMessage>
    ): DoraemonResponse = withContext(Dispatchers.IO) {
        val contextText = buildFinanceContextPrompt(snapshot)
        val chatContents = mutableListOf<Content>()
        recentHistory.takeLast(8).forEach { msg ->
            chatContents.add(
                content(role = if (msg.isUser) "user" else "model") {
                    text(
                        if (msg.isUser) msg.text
                        else {
                            val actNote = if (msg.executedActions.isNotEmpty()) {
                                " [Acciones ejecutadas: ${msg.executedActions.joinToString("; ")}]"
                            } else ""
                            msg.text + actNote
                        }
                    )
                }
            )
        }
        chatContents.add(
            content(role = "user") {
                text(
                    """
                    [ESTADO ACTUAL DE DORACASH EN TIEMPO REAL]
                    $contextText

                    [MENSAJE DEL USUARIO]
                    $userMessage
                    """.trimIndent()
                )
            }
        )

        val response = withTimeout(TIMEOUT_MS) {
            chatAgentModel.generateContent(chatContents)
        }
        val raw = response.text ?: throw IllegalStateException("Doraemon no devolvió respuesta.")
        parseResponse(raw)
    }

    private fun parseResponse(rawJson: String): DoraemonResponse {
        val obj = JSONObject(rawJson)
        val reply = obj.optString("reply").trim()
            .ifBlank { "¡Listo! Revisé tu bolsillo." }
        val thought = obj.optString("thoughtBubble").trim()
            .ifBlank { "¡Tu bolsillo está bajo control!" }
        val gesture = DoraemonGesture.safeValueOf(obj.optString("gesture"))
        val actionsJson = obj.optJSONArray("actions") ?: JSONArray()
        val parsedActions = mutableListOf<DoraemonAction>()

        for (i in 0 until actionsJson.length()) {
            val item = actionsJson.optJSONObject(i) ?: continue
            val type = DoraemonActionType.safeValueOf(item.optString("actionType")) ?: continue
            parsedActions.add(
                DoraemonAction(
                    type = type,
                    txType = item.optStringOrNull("txType"),
                    amount = if (item.has("amount") && !item.isNull("amount")) item.optDouble("amount") else null,
                    accountRef = item.optStringOrNull("accountRef"),
                    toAccountRef = item.optStringOrNull("toAccountRef"),
                    categoryId = item.optStringOrNull("categoryId"),
                    note = item.optStringOrNull("note"),
                    dateIso = item.optStringOrNull("dateIso"),
                    title = item.optStringOrNull("title"),
                    currency = item.optStringOrNull("currency"),
                    accountType = item.optStringOrNull("accountType"),
                    frequency = item.optStringOrNull("frequency"),
                    targetRef = item.optStringOrNull("targetRef"),
                    adjustBalance = if (item.has("adjustBalance") && !item.isNull("adjustBalance")) item.optBoolean("adjustBalance") else null,
                    exchangeRate = if (item.has("exchangeRate") && !item.isNull("exchangeRate")) item.optDouble("exchangeRate") else null,
                    darkThemeMode = item.optStringOrNull("darkThemeMode"),
                    tab = item.optStringOrNull("tab"),
                    last4 = item.optStringOrNull("last4"),
                    cardKind = item.optStringOrNull("cardKind"),
                    network = item.optStringOrNull("network"),
                    creditLimit = if (item.has("creditLimit") && !item.isNull("creditLimit")) item.optDouble("creditLimit") else null,
                    bankName = item.optStringOrNull("bankName"),
                    autoDebitMode = item.optStringOrNull("autoDebitMode"),
                    autoDebitValue = if (item.has("autoDebitValue") && !item.isNull("autoDebitValue")) item.optDouble("autoDebitValue") else null,
                    applyAutoDebit = if (item.has("applyAutoDebit") && !item.isNull("applyAutoDebit")) item.optBoolean("applyAutoDebit") else null
                )
            )
        }
        return DoraemonResponse(
            reply = reply,
            thoughtBubble = thought,
            gesture = gesture,
            actions = parsedActions
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    }

    private fun buildFinanceContextPrompt(snapshot: DoracashSnapshot): String {
        val today = FinanceUtils.todayIso()
        val ym = FinanceUtils.currentYearMonth()
        val disp = CurrencyCode.safeValueOf(snapshot.profile.displayCurrency)
        val rate = snapshot.profile.exchangeRate
        val netWorth = snapshot.accounts.sumOf {
            FinanceUtils.convertCurrency(it.balance, it.currencyCode, disp, rate)
        }
        val monthTxs = snapshot.transactions.filter { it.dateIso.startsWith(ym) }
        val monthIncome = monthTxs.filter { it.typeEnum == TxType.INCOME }.sumOf {
            FinanceUtils.convertCurrency(it.amount, it.currencyCode, disp, rate)
        }
        val monthExpense = monthTxs.filter { it.typeEnum == TxType.EXPENSE }.sumOf {
            FinanceUtils.convertCurrency(it.amount, it.currencyCode, disp, rate)
        }

        val now = Calendar.getInstance()
        val monthBills = FinanceUtils.occurrencesForMonth(
            snapshot.scheduledBills,
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH)
        )

        return buildString {
            appendLine("Hoy: $today | Usuario: ${snapshot.profile.displayName} | Moneda visual: ${disp.name} | Tasa: 1 USD = ${fmt(rate)} C$ | Tema: ${snapshot.profile.darkThemeMode}")
            appendLine("Patrimonio neto: ${FinanceUtils.formatMoney(netWorth, disp)} | Ingresos mes ($ym): ${FinanceUtils.formatMoney(monthIncome, disp)} | Gastos mes ($ym): ${FinanceUtils.formatMoney(monthExpense, disp)}")
            appendLine("Cuentas (${snapshot.accounts.size}):")
            snapshot.accounts.forEach { acc ->
                appendLine("- id=${acc.id} | nombre='${acc.name}' | tipo=${acc.accountType} | moneda=${acc.currency} | saldo=${fmt(acc.balance)}")
            }
            if (snapshot.budgets.isNotEmpty()) {
                appendLine("Presupuestos mensuales (en C$):")
                snapshot.budgets.forEach { b ->
                    val spentNio = monthTxs
                        .filter { it.typeEnum == TxType.EXPENSE && it.categoryId == b.categoryId }
                        .sumOf { FinanceUtils.convertCurrency(it.amount, it.currencyCode, CurrencyCode.NIO, rate) }
                    val cat = CategoryCatalog.find(false, b.categoryId)
                    appendLine("- id=${b.id} | catId=${b.categoryId} (${cat.name}) | gastado=C$${fmt(spentNio)} / limite=C$${fmt(b.limitNio)}")
                }
            }
            if (snapshot.goals.isNotEmpty()) {
                appendLine("Mis Ahorros (${snapshot.goals.size}):")
                snapshot.goals.forEach { g ->
                    val targetInfo = if (g.hasExplicitTarget) {
                        val pct = ((g.currentNio / g.targetNio) * 100).toInt()
                        " / objetivo=C$${fmt(g.targetNio)} ($pct%)"
                    } else ""
                    appendLine("- id=${g.id} | tipo=${g.savingsType.shortLabel} | nombre='${g.name}' | monto=C$${fmt(g.currentNio)}$targetInfo")
                }
            }
            if (monthBills.isNotEmpty()) {
                appendLine("Pagos programados este mes:")
                monthBills.forEach { occ ->
                    val urg = if (occ.status == BillStatus.OVERDUE) "VENCIDO" else occ.status.name
                    appendLine("- billId=${occ.bill.id} | titulo='${occ.bill.title}' | monto=${fmt(occ.bill.amount)} ${occ.bill.currency} | fecha=${occ.dateIso} | estado=$urg | frec=${occ.bill.frequency} | cuentaId=${occ.bill.accountId}")
                }
            }
            if (snapshot.cards.isNotEmpty()) {
                appendLine("Tarjetas y Wallet (${snapshot.cards.size}):")
                snapshot.cards.forEach { c ->
                    if (c.cardKindEnum == CardKind.DEBIT) {
                        appendLine("- id=${c.id} | alias='${c.alias}' | tipo=DEBIT | moneda=${c.currency} | cuentaId=${c.linkedAccountId}")
                        return@forEach
                    }
                    val dueDays = FinanceUtils.daysUntilMonthDay(c.dueDay)
                    val utilPct = if (c.creditLimit > 0.0) ((c.debtBalance / c.creditLimit) * 100).toInt() else 0
                    appendLine(
                        "- id=${c.id} | alias='${c.alias}' | banco='${c.bankName}' | tipo=${c.cardKind} | red=${c.network} | ••••${c.last4} | moneda=${c.currency} | deuda=${fmt(c.debtBalance)} | limite=${fmt(c.creditLimit)} ($utilPct%) | autoDebito=${c.autoDebitEnabled}(${c.autoDebitMode},${fmt(c.autoDebitValue)}) | gWallet=${c.googleWalletLinked} | congelada=${c.cardFrozen} | venceEn=${dueDays}d"
                    )
                }
            }
            if (snapshot.transactions.isNotEmpty()) {
                appendLine("Últimos movimientos (máx 20):")
                snapshot.transactions.take(20).forEach { tx ->
                    appendLine("- id=${tx.id} | ${tx.dateIso} | ${tx.txType} | ${fmt(tx.amount)} ${tx.currency} | cat=${tx.categoryId} | cuentaId=${tx.accountId} | nota='${tx.note}'")
                }
            }
            if (snapshot.reconciliations.isNotEmpty()) {
                appendLine("Últimos arqueos:")
                snapshot.reconciliations.take(5).forEach { r ->
                    appendLine("- ${r.dateIso} | cuentaId=${r.accountId} | sistema=${fmt(r.systemBalance)} | contado=${fmt(r.countedBalance)} | dif=${fmt(r.difference)} | ajustado=${r.balanceAdjusted}")
                }
            }
        }
    }

    private fun buildCompactHealthSummary(snapshot: DoracashSnapshot): String {
        val today = FinanceUtils.todayIso()
        val ym = FinanceUtils.currentYearMonth()
        val disp = CurrencyCode.safeValueOf(snapshot.profile.displayCurrency)
        val rate = snapshot.profile.exchangeRate
        val monthTxs = snapshot.transactions.filter { it.dateIso.startsWith(ym) }
        val monthIncome = monthTxs.filter { it.typeEnum == TxType.INCOME }
            .sumOf { FinanceUtils.convertCurrency(it.amount, it.currencyCode, disp, rate) }
        val monthExpense = monthTxs.filter { it.typeEnum == TxType.EXPENSE }
            .sumOf { FinanceUtils.convertCurrency(it.amount, it.currencyCode, disp, rate) }
        val now = Calendar.getInstance()
        val bills = FinanceUtils.occurrencesForMonth(
            snapshot.scheduledBills,
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH)
        ).filter { it.status != BillStatus.PAID }

        return buildString {
            appendLine("Hoy: $today | Ingresos mes: ${FinanceUtils.formatMoney(monthIncome, disp)} | Gastos mes: ${FinanceUtils.formatMoney(monthExpense, disp)} | Movimientos totales: ${snapshot.transactions.size}")
            appendLine("Cuentas: " + snapshot.accounts.joinToString("; ") { "${it.name}=${FinanceUtils.formatMoney(it.balance, it.currencyCode)}" })
            if (snapshot.cards.isNotEmpty()) {
                appendLine("Tarjetas: " + snapshot.cards.joinToString("; ") { c ->
                    if (c.cardKindEnum == CardKind.DEBIT) return@joinToString "${c.alias} (débito)"
                    val dueDays = FinanceUtils.daysUntilMonthDay(c.dueDay)
                    val util = if (c.creditLimit > 0.0) " uso=${((c.debtBalance / c.creditLimit) * 100).toInt()}%" else ""
                    "${c.alias}(••••${c.last4}) deuda=${FinanceUtils.formatMoney(c.debtBalance, c.currencyCode)}$util venceEn=${dueDays}d"
                })
            }
            if (bills.isNotEmpty()) {
                appendLine("Pagos pendientes: " + bills.joinToString("; ") { "${it.bill.title}(${it.dateIso},${it.status.name},${FinanceUtils.formatMoney(it.bill.amount, it.bill.currencyCode)})" })
            }
            if (snapshot.budgets.isNotEmpty()) {
                appendLine("Presupuestos: " + snapshot.budgets.joinToString("; ") { b ->
                    val spent = monthTxs.filter { it.typeEnum == TxType.EXPENSE && it.categoryId == b.categoryId }
                        .sumOf { FinanceUtils.convertCurrency(it.amount, it.currencyCode, CurrencyCode.NIO, rate) }
                    val pct = if (b.limitNio > 0) ((spent / b.limitNio) * 100).toInt() else 0
                    "${b.categoryId}=$pct%(C$${fmt(spent)}/C$${fmt(b.limitNio)})"
                })
            }
            if (snapshot.goals.isNotEmpty()) {
                appendLine("Ahorros: " + snapshot.goals.joinToString("; ") { g ->
                    val targetTxt = if (g.hasExplicitTarget) " (${((g.currentNio / g.targetNio) * 100).toInt()}%)" else ""
                    "${g.name}[${g.savingsType.shortLabel}]=C$${fmt(g.currentNio)}$targetTxt"
                })
            }
        }
    }

    private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)

    private companion object {
        const val TIMEOUT_MS = 60_000L
    }
}
