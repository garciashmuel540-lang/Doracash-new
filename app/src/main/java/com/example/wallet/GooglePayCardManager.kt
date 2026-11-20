package com.example.wallet

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentCardRecognitionIntentRequest
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

sealed interface GooglePayReadyStatus {
    data object Checking : GooglePayReadyStatus
    data object Ready : GooglePayReadyStatus
    data class NotReady(val reason: String) : GooglePayReadyStatus
}

data class ScannedCardSummary(
    val binPrefix: String,
    val last4: String,
    val expirationLabel: String?
)

object GooglePayCardManager {

    private val allowedCardNetworks = listOf("AMEX", "DISCOVER", "MASTERCARD", "VISA")
    private val allowedCardAuthMethods = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")

    fun createPaymentsClient(context: Context): PaymentsClient {
        val options = Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
            .build()
        return Wallet.getPaymentsClient(context.applicationContext, options)
    }

    private fun baseCardPaymentMethodJson(forPaymentRequest: Boolean): JSONObject {
        val parameters = JSONObject().apply {
            put("allowedAuthMethods", JSONArray(allowedCardAuthMethods))
            put("allowedCardNetworks", JSONArray(allowedCardNetworks))
            if (forPaymentRequest) {
                put("assuranceDetailsRequired", true)
            }
        }
        return JSONObject().apply {
            put("type", "CARD")
            put("parameters", parameters)
            if (forPaymentRequest) {
                put(
                    "tokenizationSpecification",
                    JSONObject().apply {
                        put("type", "PAYMENT_GATEWAY")
                        put(
                            "parameters",
                            JSONObject().apply {
                                put("gateway", "example")
                                put("gatewayMerchantId", "exampleGatewayMerchantId")
                            }
                        )
                    }
                )
            }
        }
    }

    private fun buildIsReadyToPayJson(): String {
        return JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
            put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethodJson(forPaymentRequest = false)))
        }.toString()
    }

    private fun buildPaymentDataRequestJson(): String {
        return JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
            put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethodJson(forPaymentRequest = true)))
            put(
                "transactionInfo",
                JSONObject().apply {
                    put("totalPriceStatus", "ESTIMATED")
                    put("totalPrice", "0.00")
                    put("currencyCode", "USD")
                }
            )
            put(
                "merchantInfo",
                JSONObject().apply {
                    put("merchantName", "Doracash Bolsillo 4D (Verificación)")
                }
            )
        }.toString()
    }

    suspend fun checkIsReadyToPay(context: Context): GooglePayReadyStatus {
        return suspendCancellableCoroutine { cont ->
            try {
                val client = createPaymentsClient(context)
                val request = IsReadyToPayRequest.fromJson(buildIsReadyToPayJson())
                client.isReadyToPay(request)
                    .addOnCompleteListener { task ->
                        if (!cont.isActive) return@addOnCompleteListener
                        val ready = runCatching { task.getResult(ApiException::class.java) }.getOrNull()
                        when (ready) {
                            true -> cont.resume(GooglePayReadyStatus.Ready)
                            false -> cont.resume(
                                GooglePayReadyStatus.NotReady("Agrega o activa una tarjeta en Google Pay / Wallet")
                            )
                            null -> {
                                val ex = task.exception
                                cont.resume(
                                    GooglePayReadyStatus.NotReady(
                                        ex?.localizedMessage ?: "Google Play Services Wallet no disponible en este dispositivo"
                                    )
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                if (cont.isActive) {
                    cont.resume(
                        GooglePayReadyStatus.NotReady(e.localizedMessage ?: "Google Pay no disponible")
                    )
                }
            }
        }
    }

    fun loadPaymentDataTask(context: Context): Task<PaymentData> {
        val client = createPaymentsClient(context)
        val request = PaymentDataRequest.fromJson(buildPaymentDataRequestJson())
        return client.loadPaymentData(request)
    }

    suspend fun requestCardScanPendingIntent(context: Context): Result<PendingIntent> {
        return suspendCancellableCoroutine { cont ->
            try {
                val client = createPaymentsClient(context)
                val req = PaymentCardRecognitionIntentRequest.getDefaultInstance()
                client.getPaymentCardRecognitionIntent(req)
                    .addOnSuccessListener { response ->
                        if (cont.isActive) {
                            cont.resume(Result.success(response.paymentCardRecognitionPendingIntent))
                        }
                    }
                    .addOnFailureListener { error ->
                        if (cont.isActive) {
                            cont.resume(Result.failure(error))
                        }
                    }
            } catch (e: Exception) {
                if (cont.isActive) {
                    cont.resume(Result.failure(e))
                }
            }
        }
    }

    fun extractScannedCardSummary(data: Intent?): ScannedCardSummary? {
        data ?: return null
        val result = PaymentCardRecognitionResult.getFromIntent(data) ?: return null
        val digits = result.pan.filter { it.isDigit() }
        if (digits.length < 10) return null
        val bin = digits.take(8)
        val last4 = digits.takeLast(4)
        val exp = result.creditCardExpirationDate?.let {
            String.format(java.util.Locale.US, "%02d/%02d", it.month, it.year % 100)
        }
        return ScannedCardSummary(
            binPrefix = bin,
            last4 = last4,
            expirationLabel = exp
        )
    }
}
