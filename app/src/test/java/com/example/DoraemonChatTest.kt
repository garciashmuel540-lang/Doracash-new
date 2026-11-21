package com.example

import com.example.ai.ChatVerificationException
import com.example.ai.localChatReply
import com.example.ai.withVerifiedAppCheck
import com.example.data.BankCard
import com.example.data.DoracashSnapshot
import com.example.data.ScheduledBill
import com.example.data.UserProfile
import com.example.ui.components.DoraemonGesture
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class DoraemonChatTest {
    private val snapshot = DoracashSnapshot(UserProfile(), emptyList(), emptyList(), emptyList(),
        emptyList(), emptyList(), emptyList())

    @Test fun greetingsAreLocalAndMixedOrOpenQuestionsReachTheModel() {
        assertEquals(DoraemonGesture.SALUTE, localChatReply("¡HOLÁ, Doraemon!", snapshot)?.gesture)
        assertNotNull(localChatReply("¿Cómo van mis finanzas?", snapshot))
        assertNull(localChatReply("Hola, registrá un gasto de 150", snapshot))
        assertNull(localChatReply("¿Cómo puedo ahorrar para una moto?", snapshot))
    }

    @Test fun upcomingIncludesTodayNextMonthAndCreditButExcludesPaidAndLater() {
        fun bill(name: String, date: String) = ScheduledBill(title = name, amount = 20.0, startDateIso = date)
        val data = snapshot.copy(scheduledBills = listOf(bill("Internet", "2026-12-31"),
            bill("Luz", "2026-12-02"), bill("Alquiler", "2027-01-08"),
            bill("Agua", "2026-12-01").copy(paidDates = listOf("2027-01-01"))),
            cards = listOf(BankCard(alias = "Visa", debtBalance = 50.0, dueDay = 3)))
        val reply = localChatReply("¿Qué pagos vencen pronto?", data, "2026-12-31")!!.text
        assertTrue(reply.contains("Hoy: Internet"))
        assertTrue(reply.contains("2027-01-02: Luz"))
        assertTrue(reply.contains("2027-01-03: Visa"))
        assertFalse(reply.contains("Alquiler"))
        assertFalse(reply.contains("Agua"))
    }

    @Test fun rejectedRequestRefreshesOnceBeforeReturning() = runBlocking {
        val calls = mutableListOf<Boolean>()
        var attempts = 0
        val value = withVerifiedAppCheck({ calls += it }) {
            attempts++
            if (attempts == 1) throw IllegalStateException("Firebase App Check token is invalid")
            "respuesta"
        }
        assertEquals(listOf(false, true), calls)
        assertEquals(2, attempts)
        assertEquals("respuesta", value)
    }

    @Test fun failedAttestationStopsTheModelAndOtherErrorsAreNotRetried() = runBlocking {
        var requests = 0
        val failed = runCatching { withVerifiedAppCheck({ error("sin conexión") }) { requests++ } }
        assertTrue(failed.exceptionOrNull() is ChatVerificationException)
        assertEquals(0, requests)
        val calls = mutableListOf<Boolean>()
        val quota = runCatching { withVerifiedAppCheck({ calls += it }) { error("quota") } }
        assertEquals("quota", quota.exceptionOrNull()?.message)
        assertEquals(listOf(false), calls)
        val cancel = runCatching { withVerifiedAppCheck({ throw CancellationException() }) { requests++ } }
        assertTrue(cancel.exceptionOrNull() is CancellationException)
    }
}
