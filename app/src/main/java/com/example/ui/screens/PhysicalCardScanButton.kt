package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.wallet.DetectedCardMetadata
import com.example.wallet.GooglePayCardManager
import kotlinx.coroutines.launch

@Composable
internal fun PhysicalCardScanButton(onDetected: (DetectedCardMetadata) -> Unit, onUnavailable: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hint = "Podés registrar la tarjeta a mano en + Tarjeta con sus últimos cuatro dígitos."
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val card = GooglePayCardManager.extractScannedCard(result.data)
            if (card == null) onUnavailable("No se pudo leer la tarjeta. $hint") else onDetected(card)
        }
    }
    OutlinedButton(
        onClick = {
            scope.launch {
                GooglePayCardManager.requestCardScanPendingIntent(context)
                    .mapCatching { scanner.launch(IntentSenderRequest.Builder(it).build()) }
                    .onFailure { onUnavailable("El escáner no está disponible. $hint") }
            }
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) { Text("Escanear mi tarjeta física") }
}
