package com.example

import com.example.data.AutoDebitMode
import com.example.data.BankCard
import com.example.data.CardKind
import com.example.data.CardNetwork
import com.example.data.CategoryCatalog
import com.example.data.CurrencyCode
import com.example.data.EntryClassification
import com.example.data.FinanceUtils
import com.example.data.SavingsGoal
import com.example.data.SavingsType
import com.example.wallet.CardDetector
import com.example.wallet.DetectedCardMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun categoryCatalog_classifiesDebtAndSavingsCorrectly() {
        assertEquals(EntryClassification.DEBT, CategoryCatalog.classificationOf("deudas"))
        assertEquals(EntryClassification.DEBT, CategoryCatalog.classificationOf("deudas_i"))
        assertEquals(EntryClassification.NORMAL, CategoryCatalog.classificationOf("comida"))
        assertEquals(EntryClassification.NORMAL, CategoryCatalog.classificationOf("salario"))

        val encoded = CategoryCatalog.buildSavingsCategoryId(SavingsType.LAFISE, "goal1234567890123456")
        assertTrue(encoded.length <= 40)
        assertEquals(EntryClassification.SAVINGS, CategoryCatalog.classificationOf(encoded))
        assertEquals(SavingsType.LAFISE, CategoryCatalog.savingsTypeOf(encoded))
        assertEquals("goal1234567890123456", CategoryCatalog.linkedGoalId(encoded))
        assertEquals("ahorro_lafise", CategoryCatalog.baseCategoryId(encoded))
    }

    @Test
    fun categoryCatalog_encodesAllSavingsTypes() {
        assertEquals("ahorro_cajita", CategoryCatalog.buildSavingsCategoryId(SavingsType.CAJITA))
        assertEquals("ahorro_lafise", CategoryCatalog.buildSavingsCategoryId(SavingsType.LAFISE))
        assertEquals("ahorro_prestamo", CategoryCatalog.buildSavingsCategoryId(SavingsType.PRESTAMO))
        assertEquals("ahorro_otro", CategoryCatalog.buildSavingsCategoryId(SavingsType.OTRO))
        assertNull(CategoryCatalog.savingsTypeOf("comida"))
        assertNull(CategoryCatalog.linkedGoalId("ahorro_cajita"))
    }

    @Test
    fun savingsGoal_resolvesTypeAndExplicitTarget() {
        val cajita = SavingsGoal(name = "Cajita", targetNio = 1.0, currentNio = 500.0, iconKey = "cajita")
        val lafise = SavingsGoal(name = "LAFISE", targetNio = 10000.0, currentNio = 2500.0, iconKey = "lafise")
        val prestamo = SavingsGoal(name = "Préstamo Ana", targetNio = 1.0, currentNio = 1200.0, iconKey = "prestamo")

        assertEquals(SavingsType.CAJITA, cajita.savingsType)
        assertFalse(cajita.hasExplicitTarget)
        assertEquals(SavingsType.LAFISE, lafise.savingsType)
        assertTrue(lafise.hasExplicitTarget)
        assertEquals(SavingsType.PRESTAMO, prestamo.savingsType)
        assertFalse(prestamo.hasExplicitTarget)
    }

    @Test
    fun planAutoCardDebits_handlesModesFrozenAndIncomeCap() {
        val fullCard = BankCard(
            id = "c1",
            alias = "BAC Oro",
            last4 = "4821",
            currency = CurrencyCode.NIO.name,
            debtBalance = 2100.0,
            autoDebitEnabled = true,
            autoDebitMode = AutoDebitMode.FULL_DEBT.name,
            dueDay = 5
        )
        val pctCard = BankCard(
            id = "c2",
            alias = "LAFISE Black",
            last4 = "9134",
            currency = CurrencyCode.NIO.name,
            debtBalance = 5000.0,
            autoDebitEnabled = true,
            autoDebitMode = AutoDebitMode.PERCENTAGE.name,
            autoDebitValue = 20.0,
            dueDay = 10
        )
        val frozenCard = BankCard(
            id = "c3",
            alias = "Congelada",
            last4 = "0001",
            currency = CurrencyCode.NIO.name,
            debtBalance = 900.0,
            autoDebitEnabled = true,
            autoDebitMode = AutoDebitMode.FULL_DEBT.name,
            cardFrozen = true
        )
        val manualCard = BankCard(
            id = "c4",
            alias = "Manual",
            last4 = "0002",
            currency = CurrencyCode.NIO.name,
            debtBalance = 900.0,
            autoDebitEnabled = true,
            autoDebitMode = AutoDebitMode.MANUAL.name
        )

        val plans = FinanceUtils.planAutoCardDebits(
            incomeAmount = 10000.0,
            incomeCurrency = CurrencyCode.NIO,
            cards = listOf(frozenCard, manualCard, pctCard, fullCard),
            exchangeRate = 36.62
        )

        assertEquals(2, plans.size)
        val p1 = plans.first { it.card.id == "c1" }
        val p2 = plans.first { it.card.id == "c2" }
        assertEquals(2100.0, p1.paymentInCardCurrency, 0.01)
        assertEquals(0.0, p1.remainingDebtAfter, 0.01)
        assertEquals(2000.0, p2.paymentInCardCurrency, 0.01)
        assertEquals(3000.0, p2.remainingDebtAfter, 0.01)
    }

    @Test
    fun planAutoCardDebits_convertsCrossCurrencyAndClampsRemaining() {
        val usdCard = BankCard(
            id = "c_usd",
            alias = "Travel USD",
            last4 = "7777",
            currency = CurrencyCode.USD.name,
            debtBalance = 100.0,
            autoDebitEnabled = true,
            autoDebitMode = AutoDebitMode.FULL_DEBT.name
        )
        val plans = FinanceUtils.planAutoCardDebits(
            incomeAmount = 1831.0, // Half of 3662.0 -> $50.00
            incomeCurrency = CurrencyCode.NIO,
            cards = listOf(usdCard),
            exchangeRate = 36.62
        )

        assertEquals(1, plans.size)
        assertEquals(50.0, plans[0].paymentInCardCurrency, 0.01)
        assertEquals(1831.0, plans[0].paymentInIncomeCurrency, 0.01)
        assertEquals(50.0, plans[0].remainingDebtAfter, 0.01)
    }

    @Test
    fun daysUntilMonthDay_andBankCardMap_stayValid() {
        val ref = Calendar.getInstance().apply { set(2026, Calendar.MAY, 10, 0, 0, 0) }
        assertEquals(0, FinanceUtils.daysUntilMonthDay(10, ref))
        assertEquals(5, FinanceUtils.daysUntilMonthDay(15, ref))
        assertTrue(FinanceUtils.daysUntilMonthDay(5, ref) in 25..27)

        val card = BankCard(
            userId = "u1",
            alias = "Mi Visa",
            bankName = "BAC",
            last4 = "12ab3456",
            creditLimit = 10000.0,
            debtBalance = 2500.0
        )
        assertEquals(7500.0, card.availableCredit, 0.01)
        val map = card.toCreateMap()
        assertEquals("3456", map["last4"])
        assertEquals(20, map.size)
    }

    @Test
    fun cardDetector_detectsNetworkPrefixesFromDigits() {
        assertEquals(CardNetwork.VISA, CardDetector.detectNetworkFromDigits("4532 1100"))
        assertEquals(CardNetwork.MASTERCARD, CardDetector.detectNetworkFromDigits("524412"))
        assertEquals(CardNetwork.MASTERCARD, CardDetector.detectNetworkFromDigits("222500"))
        assertEquals(CardNetwork.AMEX, CardDetector.detectNetworkFromDigits("341299"))
        assertEquals(CardNetwork.AMEX, CardDetector.detectNetworkFromDigits("378282"))
        assertEquals(CardNetwork.OTHER, CardDetector.detectNetworkFromDigits("601155"))
        assertNull(CardDetector.detectNetworkFromDigits("123"))
    }

    @Test
    fun cardDetector_detectsRegionalBanksAndKindsFromText_andDuplicateMatch() {
        val bac = CardDetector.detectFromText("BAC Cashback Oro")
        assertEquals("BAC Credomatic", bac.bankName)
        assertEquals(CardKind.CREDIT, bac.cardKind)

        val lafise = CardDetector.detectFromText("LAFISE Débito")
        assertEquals("Banco LAFISE", lafise.bankName)
        assertEquals(CardKind.DEBIT, lafise.cardKind)

        val zinli = CardDetector.detectFromText("Zinli Prepago")
        assertEquals("Zinli", zinli.bankName)
        assertEquals(CardKind.PREPAID, zinli.cardKind)
        assertEquals(CardNetwork.VISA, zinli.network)

        val existing = listOf(
            BankCard(id = "c1", last4 = "4821", network = CardNetwork.VISA.name),
            BankCard(id = "c2", last4 = "9999", network = CardNetwork.MASTERCARD.name)
        )
        assertEquals(
            "c1",
            CardDetector.isDuplicateCard(existing, DetectedCardMetadata(network = CardNetwork.VISA, last4 = "4821"))?.id
        )
        assertNull(
            CardDetector.isDuplicateCard(existing, DetectedCardMetadata(network = CardNetwork.MASTERCARD, last4 = "4821"))
        )
    }

    @Test
    fun paydayCountdown_computesQuincenaAndFinDeMesCorrectly() {
        val on15th = Calendar.getInstance().apply { set(2026, Calendar.MAY, 15, 9, 0, 0) }
        assertEquals(0, FinanceUtils.daysUntilPayday(on15th))
        assertEquals("Hoy es quincena", FinanceUtils.paydayBannerText(on15th))

        val on14th = Calendar.getInstance().apply { set(2026, Calendar.MAY, 14, 9, 0, 0) }
        assertEquals(1, FinanceUtils.daysUntilPayday(on14th))
        assertEquals("Falta 1 día para la quincena", FinanceUtils.paydayBannerText(on14th))

        val on20thMay = Calendar.getInstance().apply { set(2026, Calendar.MAY, 20, 9, 0, 0) }
        assertEquals(11, FinanceUtils.daysUntilPayday(on20thMay))
        assertEquals("Faltan 11 días para la quincena", FinanceUtils.paydayBannerText(on20thMay))

        val on30thMay = Calendar.getInstance().apply { set(2026, Calendar.MAY, 30, 9, 0, 0) }
        assertEquals(1, FinanceUtils.daysUntilPayday(on30thMay))
        assertEquals("Falta 1 día para la quincena", FinanceUtils.paydayBannerText(on30thMay))

        val on31stMay = Calendar.getInstance().apply { set(2026, Calendar.MAY, 31, 9, 0, 0) }
        assertEquals(0, FinanceUtils.daysUntilPayday(on31stMay))
        assertEquals("Hoy es quincena", FinanceUtils.paydayBannerText(on31stMay))
    }
}
