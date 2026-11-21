package com.example.wallet

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.wallet.PaymentCardRecognitionIntentRequest
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object GooglePayCardManager {

    suspend fun requestCardScanPendingIntent(context: Context): Result<PendingIntent> {
        return suspendCancellableCoroutine { cont ->
            try {
                val options = Wallet.WalletOptions.Builder().setEnvironment(WalletConstants.ENVIRONMENT_PRODUCTION).build()
                val client = Wallet.getPaymentsClient(context.applicationContext, options)
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

    fun extractScannedCard(data: Intent?): DetectedCardMetadata? {
        val result = data?.let { PaymentCardRecognitionResult.getFromIntent(it) } ?: return null
        return CardDetector.fromPhysicalCard(result.pan)
    }
}
