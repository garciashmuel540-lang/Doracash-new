package com.example.data

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

enum class CurrencyCode(val symbol: String, val label: String) {
    NIO("C$", "Córdobas (C$)"),
    USD("$", "Dólares ($)");

    companion object {
        fun safeValueOf(raw: String?): CurrencyCode =
            entries.firstOrNull { it.name == raw } ?: NIO
    }
}

enum class AccountType(val label: String, val defaultIconKey: String) {
    CASH("Efectivo", "cash"),
    BANK("Banco", "bank"),
    SAVINGS("Ahorro", "savings"),
    CREDIT("Tarjeta Crédito", "credit");

    companion object {
        fun safeValueOf(raw: String?): AccountType =
            entries.firstOrNull { it.name == raw } ?: CASH
    }
}

enum class TxType(val label: String) {
    INCOME("Ingreso"),
    EXPENSE("Gasto"),
    TRANSFER("Transferencia");

    companion object {
        fun safeValueOf(raw: String?): TxType =
            entries.firstOrNull { it.name == raw } ?: EXPENSE
    }
}

enum class BillFrequency(val label: String) {
    ONCE("Una sola vez"),
    WEEKLY("Semanal"),
    BIWEEKLY("Quincenal"),
    MONTHLY("Mensual");

    companion object {
        fun safeValueOf(raw: String?): BillFrequency =
            entries.firstOrNull { it.name == raw } ?: MONTHLY
    }
}

enum class DarkThemeMode(val label: String) {
    SYSTEM("Sistema"),
    LIGHT("Día Claro"),
    DARK("Noche Tokio");

    companion object {
        fun safeValueOf(raw: String?): DarkThemeMode =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

enum class BillStatus(val label: String) {
    PAID("Pagado"),
    OVERDUE("Vencido"),
    SOON("Por vencer"),
    UPCOMING("Programado")
}

enum class SavingsType(
    val key: String,
    val label: String,
    val shortLabel: String,
    val description: String,
    val color: Color
) {
    CAJITA(
        key = "cajita",
        label = "Ahorro de Cajita",
        shortLabel = "Cajita",
        description = "Efectivo guardado en cajita o alcancía",
        color = Color(0xFFD97706)
    ),
    LAFISE(
        key = "lafise",
        label = "Ahorro LAFISE",
        shortLabel = "LAFISE",
        description = "Reserva o cuenta de ahorro en LAFISE",
        color = Color(0xFF0096D6)
    ),
    PRESTAMO(
        key = "prestamo",
        label = "Ahorro por Préstamo",
        shortLabel = "Prestado",
        description = "Ahorros que prestaste y te deben devolver",
        color = Color(0xFF8B5CF6)
    ),
    OTRO(
        key = "savings",
        label = "Otro Ahorro",
        shortLabel = "Otro",
        description = "Ahorro o reserva general",
        color = Color(0xFF10B981)
    );

    companion object {
        fun fromKey(key: String?): SavingsType = when (key?.trim()?.lowercase()) {
            "cajita" -> CAJITA
            "lafise" -> LAFISE
            "prestamo" -> PRESTAMO
            else -> OTRO
        }
    }
}

enum class EntryClassification(val label: String) {
    NORMAL("Normal"),
    DEBT("Deuda"),
    SAVINGS("Parte del ahorro")
}

data class FinanceCategory(
    val id: String,
    val name: String,
    val color: Color,
    val iconKey: String,
    val isIncome: Boolean = false
)

object CategoryCatalog {
    val expenseCategories = listOf(
        FinanceCategory("comida", "Comida y Súper", Color(0xFFF59E0B), "food"),
        FinanceCategory("servicios", "Servicios (Luz/Agua/Net)", Color(0xFF0096D6), "bolt"),
        FinanceCategory("transporte", "Transporte y Gasolina", Color(0xFF6366F1), "bus"),
        FinanceCategory("vivienda", "Vivienda / Alquiler", Color(0xFFEC4899), "home"),
        FinanceCategory("salud", "Salud y Farmacia", Color(0xFF10B981), "health"),
        FinanceCategory("ocio", "Entretenimiento", Color(0xFF8B5CF6), "game"),
        FinanceCategory("educacion", "Educación", Color(0xFF0EA5E9), "school"),
        FinanceCategory("deudas", "Deuda / Préstamo", Color(0xFFE6002D), "card"),
        FinanceCategory("ahorro_cajita", "Ahorro Cajita", Color(0xFFD97706), "cajita"),
        FinanceCategory("ahorro_lafise", "Ahorro LAFISE", Color(0xFF0096D6), "lafise"),
        FinanceCategory("ahorro_prestamo", "Ahorro por Préstamo", Color(0xFF8B5CF6), "prestamo"),
        FinanceCategory("ahorro_otro", "Otro Ahorro", Color(0xFF10B981), "savings"),
        FinanceCategory("ahorro_meta", "Abono a Ahorro", Color(0xFFD97706), "savings"),
        FinanceCategory("ajuste", "Ajuste Conciliación", Color(0xFF64748B), "calc"),
        FinanceCategory("otros_g", "Otros Gastos", Color(0xFF94A3B8), "box")
    )

    val incomeCategories = listOf(
        FinanceCategory("salario", "Salario / Nómina", Color(0xFF10B981), "work", isIncome = true),
        FinanceCategory("freelance", "Negocio / Extra", Color(0xFF0096D6), "rocket", isIncome = true),
        FinanceCategory("regalo", "Regalo / Reembolso", Color(0xFFFFC700), "gift", isIncome = true),
        FinanceCategory("deudas_i", "Préstamo / Deuda recibida", Color(0xFFE6002D), "card", isIncome = true),
        FinanceCategory("ahorro_cajita", "Ingreso a Ahorro Cajita", Color(0xFFD97706), "cajita", isIncome = true),
        FinanceCategory("ahorro_lafise", "Ingreso a Ahorro LAFISE", Color(0xFF0096D6), "lafise", isIncome = true),
        FinanceCategory("ahorro_prestamo", "Recuperación Ahorro Prestado", Color(0xFF8B5CF6), "prestamo", isIncome = true),
        FinanceCategory("ahorro_otro", "Ingreso a Ahorro", Color(0xFF10B981), "savings", isIncome = true),
        FinanceCategory("ajuste_i", "Ajuste Conciliación", Color(0xFF64748B), "calc", isIncome = true),
        FinanceCategory("otros_i", "Otros Ingresos", Color(0xFF8B5CF6), "sparkle", isIncome = true)
    )

    fun baseCategoryId(categoryId: String): String = categoryId.substringBefore(":")

    fun linkedGoalId(categoryId: String): String? =
        categoryId.substringAfter(":", "").takeIf { it.isNotBlank() }

    fun classificationOf(categoryId: String): EntryClassification {
        val base = baseCategoryId(categoryId).lowercase()
        return when {
            base == "deudas" || base == "deudas_i" -> EntryClassification.DEBT
            base.startsWith("ahorro_") -> EntryClassification.SAVINGS
            else -> EntryClassification.NORMAL
        }
    }

    fun savingsTypeOf(categoryId: String): SavingsType? {
        return when (baseCategoryId(categoryId).lowercase()) {
            "ahorro_cajita" -> SavingsType.CAJITA
            "ahorro_lafise" -> SavingsType.LAFISE
            "ahorro_prestamo" -> SavingsType.PRESTAMO
            "ahorro_otro", "ahorro_meta" -> SavingsType.OTRO
            else -> null
        }
    }

    fun buildSavingsCategoryId(type: SavingsType, goalId: String? = null): String {
        val prefix = when (type) {
            SavingsType.CAJITA -> "ahorro_cajita"
            SavingsType.LAFISE -> "ahorro_lafise"
            SavingsType.PRESTAMO -> "ahorro_prestamo"
            SavingsType.OTRO -> "ahorro_otro"
        }
        val cleanId = goalId?.trim().orEmpty()
        return if (cleanId.isNotEmpty() && prefix.length + 1 + cleanId.length <= 40) {
            "$prefix:$cleanId"
        } else {
            prefix
        }
    }

    fun find(isIncome: Boolean, categoryId: String): FinanceCategory {
        val base = baseCategoryId(categoryId)
        val list = if (isIncome) incomeCategories else expenseCategories
        return list.firstOrNull { it.id == base }
            ?: expenseCategories.firstOrNull { it.id == base }
            ?: incomeCategories.firstOrNull { it.id == base }
            ?: FinanceCategory(base, "General", Color(0xFF64748B), "box", isIncome)
    }
}

object DenominationCatalog {
    val nioDenominations = listOf(1000.0, 500.0, 200.0, 100.0, 50.0, 20.0, 10.0, 5.0, 1.0, 0.5)
    val usdDenominations = listOf(100.0, 50.0, 20.0, 10.0, 5.0, 1.0, 0.25)

    fun forCurrency(currency: CurrencyCode): List<Double> =
        if (currency == CurrencyCode.USD) usdDenominations else nioDenominations
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val displayCurrency: String = CurrencyCode.NIO.name,
    val exchangeRate: Double = 36.62,
    val darkThemeMode: String = DarkThemeMode.SYSTEM.name,
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.take(80),
        "displayCurrency" to displayCurrency,
        "exchangeRate" to exchangeRate,
        "darkThemeMode" to darkThemeMode,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class WalletAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val accountType: String = AccountType.CASH.name,
    val currency: String = CurrencyCode.NIO.name,
    val balance: Double = 0.0,
    val iconKey: String = "cash",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    val currencyCode: CurrencyCode get() = CurrencyCode.safeValueOf(currency)
    val typeEnum: AccountType get() = AccountType.safeValueOf(accountType)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "name" to name.take(60),
        "accountType" to accountType,
        "currency" to currency,
        "balance" to balance,
        "iconKey" to iconKey.take(30),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class FinanceTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val txType: String = TxType.EXPENSE.name,
    val amount: Double = 0.0,
    val currency: String = CurrencyCode.NIO.name,
    val accountId: String = "",
    val toAccountId: String? = null,
    val categoryId: String = "comida",
    val note: String = "",
    val dateIso: String = "",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    val typeEnum: TxType get() = TxType.safeValueOf(txType)
    val currencyCode: CurrencyCode get() = CurrencyCode.safeValueOf(currency)

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "txType" to txType,
            "amount" to amount,
            "currency" to currency,
            "accountId" to accountId,
            "toAccountId" to toAccountId?.takeIf { it.isNotBlank() },
            "categoryId" to categoryId,
            "note" to note.take(160),
            "dateIso" to dateIso,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class ScheduledBill(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val currency: String = CurrencyCode.NIO.name,
    val accountId: String = "",
    val categoryId: String = "servicios",
    val frequency: String = BillFrequency.MONTHLY.name,
    val startDateIso: String = "",
    val paidDates: List<String> = emptyList(),
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    val currencyCode: CurrencyCode get() = CurrencyCode.safeValueOf(currency)
    val frequencyEnum: BillFrequency get() = BillFrequency.safeValueOf(frequency)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.take(80),
        "amount" to amount,
        "currency" to currency,
        "accountId" to accountId,
        "categoryId" to categoryId,
        "frequency" to frequency,
        "startDateIso" to startDateIso,
        "paidDates" to paidDates.take(120),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CategoryBudget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val categoryId: String = "comida",
    val limitNio: Double = 0.0,
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "categoryId" to categoryId,
        "limitNio" to limitNio,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class SavingsGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val targetNio: Double = 0.0,
    val currentNio: Double = 0.0,
    val iconKey: String = "savings",
    val deadlineIso: String = "",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    val savingsType: SavingsType get() = SavingsType.fromKey(iconKey)
    val hasExplicitTarget: Boolean get() = targetNio > 1.0

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "name" to name.take(80),
        "targetNio" to targetNio,
        "currentNio" to currentNio,
        "iconKey" to iconKey.take(30),
        "deadlineIso" to deadlineIso.take(10),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CashReconciliation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateIso: String = "",
    val accountId: String = "",
    val currency: String = CurrencyCode.NIO.name,
    val systemBalance: Double = 0.0,
    val countedBalance: Double = 0.0,
    val difference: Double = 0.0,
    val balanceAdjusted: Boolean = false,
    val note: String = "",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    val currencyCode: CurrencyCode get() = CurrencyCode.safeValueOf(currency)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "dateIso" to dateIso,
        "accountId" to accountId,
        "currency" to currency,
        "systemBalance" to systemBalance,
        "countedBalance" to countedBalance,
        "difference" to difference,
        "balanceAdjusted" to balanceAdjusted,
        "note" to note.take(160),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

enum class CardKind(val label: String) {
    CREDIT("Crédito"),
    DEBIT("Débito"),
    PREPAID("Prepago"),
    VIRTUAL("Virtual");

    companion object {
        fun safeValueOf(raw: String?): CardKind =
            entries.firstOrNull { it.name == raw } ?: CREDIT
    }
}

enum class CardNetwork(val label: String, val badgeText: String) {
    VISA("Visa", "VISA"),
    MASTERCARD("Mastercard", "MC"),
    AMEX("American Express", "AMEX"),
    OTHER("Otra", "CARD");

    companion object {
        fun safeValueOf(raw: String?): CardNetwork =
            entries.firstOrNull { it.name == raw } ?: VISA
    }
}

enum class AutoDebitMode(val label: String, val description: String) {
    FULL_DEBT("Saldar deuda", "Descuenta de tu ingreso hasta cubrir lo que debes"),
    PERCENTAGE("% del ingreso", "Destina un porcentaje automático de cada ingreso"),
    FIXED_AMOUNT("Cuota fija", "Descuenta un monto fijo en cada ingreso"),
    MANUAL("Solo manual", "No debita automáticamente al registrar ingresos");

    companion object {
        fun safeValueOf(raw: String?): AutoDebitMode =
            entries.firstOrNull { it.name == raw } ?: FULL_DEBT
    }
}

enum class CardColorTheme(
    val key: String,
    val label: String,
    val startColor: Color,
    val endColor: Color
) {
    DORA_BLUE("dora_blue", "Azul Doraemon", Color(0xFF0096D6), Color(0xFF0057A8)),
    OBSIDIAN("obsidian", "Negro Titanio", Color(0xFF1E293B), Color(0xFF0F172A)),
    GOLD("gold", "Dorado Bell", Color(0xFFD97706), Color(0xFF78350F)),
    CRIMSON("crimson", "Rojo Collar", Color(0xFFE6002D), Color(0xFF881337)),
    EMERALD("emerald", "Verde Menta", Color(0xFF059669), Color(0xFF064E3B)),
    PURPLE("purple", "Violeta Cósmico", Color(0xFF7C3AED), Color(0xFF4C1D95));

    companion object {
        fun fromKey(key: String?): CardColorTheme =
            entries.firstOrNull { it.key == key } ?: DORA_BLUE
    }
}

@Keep
@IgnoreExtraProperties
data class BankCard(
    @DocumentId val id: String = "",
    val userId: String = "",
    val alias: String = "",
    val bankName: String = "",
    val cardKind: String = CardKind.CREDIT.name,
    val network: String = CardNetwork.VISA.name,
    val last4: String = "0000",
    val currency: String = CurrencyCode.NIO.name,
    val debtBalance: Double = 0.0,
    val creditLimit: Double = 0.0,
    val linkedAccountId: String = "",
    val autoDebitEnabled: Boolean = true,
    val autoDebitMode: String = AutoDebitMode.FULL_DEBT.name,
    val autoDebitValue: Double = 100.0,
    val googleWalletLinked: Boolean = false,
    val cardFrozen: Boolean = false,
    val cutoffDay: Int = 15,
    val dueDay: Int = 5,
    val colorTheme: String = CardColorTheme.DORA_BLUE.key,
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    val cardKindEnum: CardKind get() = CardKind.safeValueOf(cardKind)
    val networkEnum: CardNetwork get() = CardNetwork.safeValueOf(network)
    val currencyCode: CurrencyCode get() = CurrencyCode.safeValueOf(currency)
    val autoDebitModeEnum: AutoDebitMode get() = AutoDebitMode.safeValueOf(autoDebitMode)
    val colorThemeEnum: CardColorTheme get() = CardColorTheme.fromKey(colorTheme)
    val availableCredit: Double get() = if (creditLimit > 0.0) (creditLimit - debtBalance).coerceAtLeast(0.0) else 0.0

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "alias" to alias.take(60),
        "bankName" to bankName.take(40),
        "cardKind" to cardKindEnum.name,
        "network" to networkEnum.name,
        "last4" to last4.filter { it.isDigit() }.takeLast(4).padStart(4, '0'),
        "currency" to currencyCode.name,
        "debtBalance" to FinanceUtils.round2(debtBalance.coerceAtLeast(0.0)),
        "creditLimit" to FinanceUtils.round2(creditLimit.coerceAtLeast(0.0)),
        "linkedAccountId" to linkedAccountId.take(128),
        "autoDebitEnabled" to autoDebitEnabled,
        "autoDebitMode" to autoDebitModeEnum.name,
        "autoDebitValue" to FinanceUtils.round2(autoDebitValue.coerceAtLeast(0.0)),
        "googleWalletLinked" to googleWalletLinked,
        "cardFrozen" to cardFrozen,
        "cutoffDay" to cutoffDay.coerceIn(1, 31),
        "dueDay" to dueDay.coerceIn(1, 31),
        "colorTheme" to colorThemeEnum.key,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class PlannedCardDebit(
    val card: BankCard,
    val paymentInCardCurrency: Double,
    val paymentInIncomeCurrency: Double,
    val remainingDebtAfter: Double
)

data class BillOccurrence(
    val bill: ScheduledBill,
    val dateIso: String,
    val status: BillStatus
)

@Keep
@IgnoreExtraProperties
data class SyncedDevice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceName: String = "",
    val versionName: String = "1.0",
    val versionCode: Int = 1,
    val platform: String = "ANDROID",
    val lastSeenIso: String = "",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "deviceName" to deviceName.ifBlank { "Android" }.take(80),
        "versionName" to versionName.ifBlank { "1.0" }.take(24),
        "versionCode" to versionCode.coerceIn(1, 1_000_000),
        "platform" to "ANDROID",
        "lastSeenIso" to lastSeenIso.ifBlank { FinanceUtils.todayIso() }.take(10),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class AppReleaseInfo(
    @DocumentId val id: String = "",
    val userId: String = "",
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0",
    val downloadUrl: String = "",
    val releaseNotes: String = "",
    val forceNotice: Boolean = false,
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "latestVersionCode" to latestVersionCode.coerceIn(1, 1_000_000),
        "latestVersionName" to latestVersionName.ifBlank { "1.0" }.take(24),
        "downloadUrl" to downloadUrl.trim().take(400),
        "releaseNotes" to releaseNotes.trim().take(240),
        "forceNotice" to forceNotice,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class DoracashSnapshot(
    val profile: UserProfile,
    val accounts: List<WalletAccount>,
    val transactions: List<FinanceTransaction>,
    val scheduledBills: List<ScheduledBill>,
    val budgets: List<CategoryBudget>,
    val goals: List<SavingsGoal>,
    val reconciliations: List<CashReconciliation>,
    val cards: List<BankCard> = emptyList(),
    val devices: List<SyncedDevice> = emptyList(),
    val releaseInfo: AppReleaseInfo? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

object FinanceUtils {
    fun todayIso(): String {
        val cal = Calendar.getInstance()
        return formatCal(cal)
    }

    fun currentYearMonth(): String = todayIso().take(7)

    fun addDaysIso(baseIso: String, deltaDays: Int): String {
        val cal = parseIsoToCalendar(baseIso)
        cal.add(Calendar.DAY_OF_MONTH, deltaDays)
        return formatCal(cal)
    }

    fun formatCal(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
    }

    fun parseIsoToCalendar(iso: String): Calendar {
        val parts = iso.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val m = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
        val d = parts.getOrNull(2)?.toIntOrNull() ?: 1
        return Calendar.getInstance().apply {
            set(y, m, d, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun convertCurrency(
        amount: Double,
        from: CurrencyCode,
        to: CurrencyCode,
        exchangeRate: Double
    ): Double {
        if (from == to) return amount
        val rate = if (exchangeRate > 0.0) exchangeRate else 36.62
        return when {
            from == CurrencyCode.USD && to == CurrencyCode.NIO -> amount * rate
            from == CurrencyCode.NIO && to == CurrencyCode.USD -> amount / rate
            else -> amount
        }
    }

    fun formatMoney(amount: Double, currency: CurrencyCode): String {
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val prefix = if (amount < -0.004) "-" else ""
        return "$prefix${currency.symbol} ${nf.format(abs(amount))}"
    }

    fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0

    fun billStatus(bill: ScheduledBill, occurrenceIso: String): BillStatus {
        if (bill.paidDates.contains(occurrenceIso)) return BillStatus.PAID
        val today = todayIso()
        if (occurrenceIso < today) return BillStatus.OVERDUE
        val soonLimit = addDaysIso(today, 4)
        if (occurrenceIso <= soonLimit) return BillStatus.SOON
        return BillStatus.UPCOMING
    }

    fun occurrencesForMonth(
        bills: List<ScheduledBill>,
        year: Int,
        monthZeroIndexed: Int
    ): List<BillOccurrence> {
        val cal = Calendar.getInstance().apply { set(year, monthZeroIndexed, 1, 0, 0, 0) }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val ym = String.format(Locale.US, "%04d-%02d", year, monthZeroIndexed + 1)
        val monthStart = Calendar.getInstance().apply {
            set(year, monthZeroIndexed, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthEnd = Calendar.getInstance().apply {
            set(year, monthZeroIndexed, daysInMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val out = mutableListOf<BillOccurrence>()
        for (bill in bills) {
            if (bill.startDateIso.length != 10) continue
            val parts = bill.startDateIso.split("-")
            val sy = parts.getOrNull(0)?.toIntOrNull() ?: continue
            val sm = (parts.getOrNull(1)?.toIntOrNull() ?: continue) - 1
            val sd = parts.getOrNull(2)?.toIntOrNull() ?: continue

            when (bill.frequencyEnum) {
                BillFrequency.ONCE -> {
                    if (sy == year && sm == monthZeroIndexed) {
                        out.add(BillOccurrence(bill, bill.startDateIso, billStatus(bill, bill.startDateIso)))
                    }
                }
                BillFrequency.MONTHLY -> {
                    val startMonth = Calendar.getInstance().apply {
                        set(sy, sm, 1, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    if (!monthStart.before(startMonth)) {
                        val day = sd.coerceIn(1, daysInMonth)
                        val iso = String.format(Locale.US, "%s-%02d", ym, day)
                        out.add(BillOccurrence(bill, iso, billStatus(bill, iso)))
                    }
                }
                BillFrequency.WEEKLY, BillFrequency.BIWEEKLY -> {
                    val step = if (bill.frequencyEnum == BillFrequency.WEEKLY) 7 else 14
                    val cur = parseIsoToCalendar(bill.startDateIso)
                    var guard = 0
                    while (!cur.after(monthEnd) && guard < 1000) {
                        if (!cur.before(monthStart)) {
                            val iso = formatCal(cur)
                            out.add(BillOccurrence(bill, iso, billStatus(bill, iso)))
                        }
                        cur.add(Calendar.DAY_OF_MONTH, step)
                        guard++
                    }
                }
            }
        }
        return out.sortedBy { it.dateIso }
    }

    fun daysUntilMonthDay(
        dayOfMonth: Int,
        reference: Calendar = Calendar.getInstance()
    ): Int {
        val today = (reference.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentMonthMax = today.getActualMaximum(Calendar.DAY_OF_MONTH)
        val targetThisMonth = dayOfMonth.coerceIn(1, currentMonthMax)
        val targetCal = (today.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, targetThisMonth)
        }
        if (targetCal.before(today)) {
            targetCal.add(Calendar.MONTH, 1)
            val nextMonthMax = targetCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            targetCal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, nextMonthMax))
        }
        val diffMs = targetCal.timeInMillis - today.timeInMillis
        return (diffMs / 86_400_000L).toInt().coerceAtLeast(0)
    }

    fun planAutoCardDebits(
        incomeAmount: Double,
        incomeCurrency: CurrencyCode,
        cards: List<BankCard>,
        exchangeRate: Double
    ): List<PlannedCardDebit> {
        if (incomeAmount <= 0.0 || cards.isEmpty()) return emptyList()
        var remainingIncome = round2(incomeAmount)
        val eligible = cards
            .filter {
                it.autoDebitEnabled &&
                    !it.cardFrozen &&
                    it.debtBalance > 0.005 &&
                    it.autoDebitModeEnum != AutoDebitMode.MANUAL
            }
            .sortedWith(
                compareBy<BankCard> { daysUntilMonthDay(it.dueDay) }
                    .thenByDescending { it.debtBalance }
            )

        val planned = mutableListOf<PlannedCardDebit>()
        for (card in eligible) {
            if (remainingIncome <= 0.005) break
            val desiredInCardCurrency = when (card.autoDebitModeEnum) {
                AutoDebitMode.FULL_DEBT -> card.debtBalance
                AutoDebitMode.PERCENTAGE -> {
                    val pct = card.autoDebitValue.coerceIn(0.0, 100.0)
                    val sliceInIncome = incomeAmount * (pct / 100.0)
                    convertCurrency(sliceInIncome, incomeCurrency, card.currencyCode, exchangeRate)
                        .coerceAtMost(card.debtBalance)
                }
                AutoDebitMode.FIXED_AMOUNT -> card.autoDebitValue.coerceAtMost(card.debtBalance)
                AutoDebitMode.MANUAL -> 0.0
            }
            if (desiredInCardCurrency <= 0.005) continue

            val desiredInIncomeCurrency = convertCurrency(
                desiredInCardCurrency,
                card.currencyCode,
                incomeCurrency,
                exchangeRate
            )
            val actualInIncomeCurrency = round2(desiredInIncomeCurrency.coerceAtMost(remainingIncome))
            if (actualInIncomeCurrency <= 0.005) continue

            val actualInCardCurrency = if (card.currencyCode == incomeCurrency) {
                actualInIncomeCurrency.coerceAtMost(card.debtBalance)
            } else {
                round2(
                    convertCurrency(actualInIncomeCurrency, incomeCurrency, card.currencyCode, exchangeRate)
                ).coerceAtMost(card.debtBalance)
            }
            if (actualInCardCurrency <= 0.005) continue

            val remainingDebtAfter = round2((card.debtBalance - actualInCardCurrency).coerceAtLeast(0.0))
            remainingIncome = round2((remainingIncome - actualInIncomeCurrency).coerceAtLeast(0.0))

            planned.add(
                PlannedCardDebit(
                    card = card,
                    paymentInCardCurrency = actualInCardCurrency,
                    paymentInIncomeCurrency = actualInIncomeCurrency,
                    remainingDebtAfter = remainingDebtAfter
                )
            )
        }
        return planned
    }
}
