package com.example

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
    fun parseGooglePayPaymentData_extractsNetworkFundingBankAndLast4() {
        val sampleVisaCredit = """
            {
              "apiVersion": 2,
              "apiVersionMinor": 0,
              "paymentMethodData": {
                "type": "CARD",
                "description": "BAC Credomatic Visa •••• 4821",
                "info": {
                  "cardNetwork": "VISA",
                  "cardDetails": "4821",
                  "cardFundingSource": "CREDIT"
                }
              }
            }
        """.trimIndent()

        val visaResult = CardDetector.parseGooglePayPaymentData(sampleVisaCredit)
        assertEquals(CardNetwork.VISA, visaResult.network)
        assertEquals(CardKind.CREDIT, visaResult.cardKind)
        assertEquals("BAC Credomatic", visaResult.bankName)
        assertEquals("4821", visaResult.last4)
        assertTrue(visaResult.googleWalletLinked)

        val sampleMasterDebit = """
            {
              "apiVersion": 2,
              "apiVersionMinor": 0,
              "paymentMethodData": {
                "type": "CARD",
                "description": "Mastercard •••• 9134",
                "info": {
                  "cardNetwork": "MASTERCARD",
                  "cardDetails": "9134",
                  "cardFundingSource": "DEBIT"
                }
              }
            }
        """.trimIndent()

        val mcResult = CardDetector.parseGooglePayPaymentData(sampleMasterDebit)
        assertEquals(CardNetwork.MASTERCARD, mcResult.network)
        assertEquals(CardKind.DEBIT, mcResult.cardKind)
        assertEquals("9134", mcResult.last4)
        assertTrue(mcResult.googleWalletLinked)
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
