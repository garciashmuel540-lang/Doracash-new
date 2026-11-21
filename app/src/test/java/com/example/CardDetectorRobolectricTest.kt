package com.example

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.wallet.WalletPlatformBridge
import org.robolectric.Shadows.shadowOf
import com.example.data.CardKind
import com.example.data.CardNetwork
import com.example.data.CurrencyCode
import com.example.wallet.CardDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CardDetectorRobolectricTest {

    @Test
    fun walletWithoutNativeAppOpensTheRealGooglePaymentMethodsPage() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        WalletPlatformBridge.openGoogleWallet(app).getOrThrow()
        val intent = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("pay.google.com", intent.data?.host)
        assertTrue(intent.data?.path.orEmpty().endsWith("/paymentmethods"))
    }

    @Test
    fun physicalCardScanKeepsNetworkAndLastFourDigits() {
        val result = requireNotNull(CardDetector.fromPhysicalCard("4111 1111 1111 1111"))
        assertEquals(CardNetwork.VISA, result.network)
        assertEquals("1111", result.last4)
        assertTrue(!result.toString().contains("4111"))
        assertEquals(null, CardDetector.fromPhysicalCard("4445"))
        assertEquals(null, CardDetector.fromPhysicalCard("not-a-card"))
    }

    @Test
    fun parseBinlistJson_resolvesBankTypeNetworkAndCurrency() {
        val bacBinJson = """
            {
              "scheme": "visa",
              "type": "credit",
              "brand": "Cashback",
              "country": {
                "name": "Nicaragua",
                "currency": "NIO"
              },
              "bank": {
                "name": "BAC NICARAGUA"
              }
            }
        """.trimIndent()

        val bacResult = CardDetector.parseBinlistJson(bacBinJson, "453211")
        assertEquals(CardNetwork.VISA, bacResult.network)
        assertEquals(CardKind.CREDIT, bacResult.cardKind)
        assertEquals("BAC Credomatic", bacResult.bankName)
        assertEquals(CurrencyCode.NIO, bacResult.suggestedCurrency)
        assertEquals("Nicaragua", bacResult.countryName)

        val lafiseBinJson = """
            {
              "scheme": "mastercard",
              "type": "debit",
              "brand": "Standard",
              "country": {
                "name": "Nicaragua",
                "currency": "NIO"
              },
              "bank": {
                "name": "BANCO LAFISE BANCENTRO"
              }
            }
        """.trimIndent()

        val lafiseResult = CardDetector.parseBinlistJson(lafiseBinJson, "524412")
        assertEquals(CardNetwork.MASTERCARD, lafiseResult.network)
        assertEquals(CardKind.DEBIT, lafiseResult.cardKind)
        assertEquals("Banco LAFISE", lafiseResult.bankName)
        assertEquals(CurrencyCode.NIO, lafiseResult.suggestedCurrency)
    }
}
