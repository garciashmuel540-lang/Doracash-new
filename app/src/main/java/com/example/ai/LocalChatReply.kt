package com.example.ai

import com.example.data.CardKind
import com.example.data.CurrencyCode
import com.example.data.DoracashSnapshot
import com.example.data.FinanceUtils
import com.example.data.TxType
import com.example.ui.components.DoraemonGesture
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import java.util.UUID

private val accents = Regex("\\p{M}+")
private val separators = Regex("[^a-z0-9]+")

internal fun localChatReply(
    message: String,
    snapshot: DoracashSnapshot,
    today: String = FinanceUtils.todayIso()
): DoraemonChatMessage? {
    val normalized = Normalizer.normalize(message.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(accents, "").replace(separators, " ").trim()
    val (reply, gesture) = when (normalized) {
        "hola", "hola doraemon", "buenas", "buenos dias", "buenas tardes", "buenas noches" ->
            "¡Hola! ¿Querés revisar tu saldo, ver qué pagos vienen o preguntarme otra cosa?" to DoraemonGesture.SALUTE
        "como van mis finanzas", "cual es mi saldo", "cuanto saldo tengo", "mi saldo" ->
            financeSummary(snapshot, today) to DoraemonGesture.ADVISING
        "que pagos vencen pronto", "que pagos vienen", "cuales son mis proximos pagos", "mis proximos pagos" ->
            upcomingPayments(snapshot, today) to DoraemonGesture.ADVISING
        else -> return null
    }
    return DoraemonChatMessage(UUID.randomUUID().toString(), false, reply, gesture)
}

private fun financeSummary(snapshot: DoracashSnapshot, today: String): String {
    val currency = CurrencyCode.safeValueOf(snapshot.profile.displayCurrency)
    val rate = snapshot.profile.exchangeRate
    val balance = snapshot.accounts.sumOf { FinanceUtils.convertCurrency(it.balance, it.currencyCode, currency, rate) }
    val month = today.take(7)
    fun monthly(type: TxType) = snapshot.transactions.filter { it.typeEnum == type && it.dateIso.startsWith(month) }
        .sumOf { FinanceUtils.convertCurrency(it.amount, it.currencyCode, currency, rate) }
    return "Saldo en tus cuentas: ${FinanceUtils.formatMoney(balance, currency)}. " +
        "Este mes ingresaron ${FinanceUtils.formatMoney(monthly(TxType.INCOME), currency)} y gastaste ${FinanceUtils.formatMoney(monthly(TxType.EXPENSE), currency)}. " +
        "Movimientos pendientes: ${snapshot.pending.size}."
}

private fun upcomingPayments(snapshot: DoracashSnapshot, today: String): String {
    val end = FinanceUtils.addDaysIso(today, 7)
    val payments = mutableListOf<Pair<String, String>>()
    for (month in listOf(today.take(7), end.take(7)).distinct()) {
        val date = FinanceUtils.parseIsoToCalendar("$month-01")
        val occurrences = FinanceUtils.occurrencesForMonth(snapshot.scheduledBills, date.get(Calendar.YEAR), date.get(Calendar.MONTH))
        for ((bill, due) in occurrences) {
            if (due in today..end && due !in bill.paidDates) {
                payments += due to "${bill.title} · ${FinanceUtils.formatMoney(bill.amount, bill.currencyCode)}"
            }
        }
    }
    val reference = FinanceUtils.parseIsoToCalendar(today)
    for (card in snapshot.cards) {
        if (card.cardKindEnum != CardKind.CREDIT || card.debtBalance <= 0) continue
        val days = FinanceUtils.daysUntilMonthDay(card.dueDay, reference)
        if (days <= 7) {
            val name = card.alias.ifBlank { card.bankName.ifBlank { "Tarjeta de crédito" } }
            payments += FinanceUtils.addDaysIso(today, days) to "$name · saldo adeudado ${FinanceUtils.formatMoney(card.debtBalance, card.currencyCode)}"
        }
    }
    if (payments.isEmpty()) return "No tenés pagos programados ni tarjetas con deuda que venzan entre hoy y el $end."
    return payments.sortedBy { it.first }.joinToString("\n", prefix = "Tus vencimientos de hoy al $end:\n") { (due, detail) ->
        "• ${if (due == today) "Hoy" else due}: $detail"
    }
}
