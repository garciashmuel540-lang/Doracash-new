package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.AutoDebitMode
import com.example.data.BankCard
import com.example.data.CardKind
import com.example.data.CurrencyCode
import com.example.data.DoracashSnapshot
import com.example.data.FinanceUtils
import com.example.ui.theme.DoraBellGold
import com.example.ui.theme.DoraCollarRed
import com.example.ui.theme.DoraDoorPink
import com.example.ui.theme.DoraMintGreen
import com.example.ui.theme.DoraSkyBlue
import com.example.wallet.CardDetector
import com.example.wallet.DetectedCardMetadata
import com.example.wallet.GooglePayCardManager
import com.example.wallet.GooglePayReadyStatus
import com.example.wallet.WalletPlatformBridge
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.contract.TaskResultContracts

@Composable
fun CardsWalletScreen(
    snapshot: DoracashSnapshot,
    onOpenNewCard: () -> Unit,
    onImportDetectedCard: (DetectedCardMetadata) -> Unit,
    onOpenEditCard: (BankCard) -> Unit,
    onOpenChargeCard: (BankCard) -> Unit,
    onOpenPayCard: (BankCard) -> Unit,
    onToggleFrozen: (BankCard) -> Unit,
    onToggleAutoDebit: (BankCard) -> Unit,
    onToggleWalletLink: (BankCard) -> Unit,
    onDeleteCard: (BankCard) -> Unit,
    onFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val disp = CurrencyCode.safeValueOf(snapshot.profile.displayCurrency)
    val rate = snapshot.profile.exchangeRate

    var walletStatus by remember {
        mutableStateOf(WalletPlatformBridge.inspectDeviceWalletStatus(context))
    }
    var googlePayStatus by remember {
        mutableStateOf<GooglePayReadyStatus>(GooglePayReadyStatus.Checking)
    }

    LaunchedEffect(context) {
        googlePayStatus = GooglePayCardManager.checkIsReadyToPay(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                walletStatus = WalletPlatformBridge.inspectDeviceWalletStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var privacyMasked by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<BankCard?>(null) }

    val deviceUnlockLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            privacyMasked = false
            onFeedback("Tarjetas desbloqueadas")
        } else {
            onFeedback("Verificación cancelada")
        }
    }

    val googlePayLauncher = rememberLauncherForActivityResult(
        contract = TaskResultContracts.GetPaymentDataResult()
    ) { taskResult ->
        if (taskResult.status.isSuccess) {
            val rawJson = taskResult.result?.toJson()
            if (!rawJson.isNullOrBlank()) {
                val detected = CardDetector.parseGooglePayPaymentData(rawJson)
                val netTxt = detected.network?.label ?: ""
                val kindTxt = detected.cardKind?.label ?: ""
                val l4Txt = detected.last4?.let { "•••• $it" } ?: ""
                onFeedback("Google Pay detectó: $netTxt $kindTxt $l4Txt".trim())
                onImportDetectedCard(detected)
            } else {
                onFeedback("Google Pay no devolvió detalles de tarjeta")
            }
        } else if (taskResult.status.statusCode == CommonStatusCodes.CANCELED) {
            onFeedback("Selección de Google Pay cancelada")
        } else {
            onFeedback("Google Pay no disponible aquí (${taskResult.status.statusCode}). Usa Escáner o BIN en + Tarjeta.")
        }
    }

    val billingCards = remember(snapshot.cards) { snapshot.cards.filter { it.cardKindEnum != CardKind.DEBIT } }
    val totalDebtDisp = remember(snapshot.cards, disp, rate) {
        billingCards.sumOf {
            FinanceUtils.convertCurrency(it.debtBalance, it.currencyCode, disp, rate)
        }
    }
    val totalLimitDisp = remember(snapshot.cards, disp, rate) {
        billingCards.filter { it.creditLimit > 0.0 }.sumOf {
            FinanceUtils.convertCurrency(it.creditLimit, it.currencyCode, disp, rate)
        }
    }
    val totalAvailDisp = (totalLimitDisp - totalDebtDisp).coerceAtLeast(0.0)
    val utilizationRatio = if (totalLimitDisp > 0.0) {
        (totalDebtDisp / totalLimitDisp).toFloat().coerceIn(0f, 1f)
    } else 0f
    val autoDebitActiveCount = remember(snapshot.cards) {
        billingCards.count { it.autoDebitEnabled && !it.cardFrozen && it.autoDebitModeEnum != AutoDebitMode.MANUAL }
    }

    var simIncomeText by remember { mutableStateOf("15000") }
    var simCurrency by remember { mutableStateOf(disp) }
    val simAmount = simIncomeText.toDoubleOrNull() ?: 0.0
    val simulatedDebits = remember(simAmount, simCurrency, snapshot.cards, rate) {
        FinanceUtils.planAutoCardDebits(simAmount, simCurrency, snapshot.cards, rate)
    }
    val simTotalDebited = simulatedDebits.sumOf { it.paymentInIncomeCurrency }
    val simRemainingCash = (simAmount - simTotalDebited).coerceAtLeast(0.0)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Wallet & Debt Summary Card
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tarjetas y Wallet",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "${snapshot.cards.size} tarjeta(s) · $autoDebitActiveCount con auto-débito",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (!privacyMasked) {
                                        privacyMasked = true
                                    } else {
                                        val unlockIntent = WalletPlatformBridge.createDeviceUnlockIntent(
                                            context = context,
                                            title = "Desbloquear Tarjetas",
                                            description = "Confirma tu identidad para mostrar saldos y deuda"
                                        )
                                        if (unlockIntent != null) {
                                            deviceUnlockLauncher.launch(unlockIntent)
                                        } else {
                                            privacyMasked = false
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("cards_privacy_toggle")
                            ) {
                                Icon(
                                    imageVector = if (privacyMasked) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (privacyMasked) "Mostrar montos" else "Ocultar montos",
                                    tint = DoraSkyBlue
                                )
                            }
                            Button(
                                onClick = onOpenNewCard,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("add_card_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tarjeta")
                            }
                        }
                    }

                    if (billingCards.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = DoraCollarRed.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Deuda en Tarjetas",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DoraCollarRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (privacyMasked) "••••••" else FinanceUtils.formatMoney(totalDebtDisp, disp),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = DoraCollarRed,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Surface(
                                color = DoraMintGreen.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Crédito Libre",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DoraMintGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (privacyMasked) "••••••" else FinanceUtils.formatMoney(totalAvailDisp, disp),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = DoraMintGreen,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    if (totalLimitDisp > 0.0) {
                        val utilColor = when {
                            utilizationRatio >= 0.7f -> DoraCollarRed
                            utilizationRatio >= 0.35f -> DoraBellGold
                            else -> DoraMintGreen
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Uso de línea de crédito",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(utilizationRatio * 100).toInt()}% (Ideal < 30%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = utilColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { utilizationRatio },
                                color = utilColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }
                    }
                }
            }
        }

        // 2. Google Wallet & NFC Hardware Bridge Card
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = DoraSkyBlue.copy(alpha = 0.14f),
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Wallet,
                                        contentDescription = "Google Wallet",
                                        tint = DoraSkyBlue
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Google Wallet y Pagos NFC", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (walletStatus.readyForTapToPay) {
                                        "Listo para pagar sin contacto"
                                    } else {
                                        "Acceso directo a Wallet, Google Pay y NFC"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (walletStatus.readyForTapToPay) DoraMintGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        StatusPill(
                            label = when (googlePayStatus) {
                                GooglePayReadyStatus.Checking -> "GPay: ..."
                                GooglePayReadyStatus.Ready -> "GPay API: Listo"
                                is GooglePayReadyStatus.NotReady -> "GPay: Config"
                            },
                            active = googlePayStatus is GooglePayReadyStatus.Ready,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(
                            label = if (walletStatus.isGoogleWalletInstalled || walletStatus.isGooglePayInstalled) {
                                "Wallet: OK"
                            } else {
                                "Wallet: Obtener"
                            },
                            active = walletStatus.isGoogleWalletInstalled || walletStatus.isGooglePayInstalled,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(
                            label = when {
                                !walletStatus.hasNfcHardware -> "NFC: N/D"
                                walletStatus.isNfcEnabled -> "NFC: Activo"
                                else -> "NFC: Off"
                            },
                            active = walletStatus.isNfcEnabled,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(
                            label = if (walletStatus.isDeviceSecure) "PIN: OK" else "Sin PIN",
                            active = walletStatus.isDeviceSecure,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            GooglePayCardManager.loadPaymentDataTask(context)
                                .addOnCompleteListener { task -> googlePayLauncher.launch(task) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DoraSkyBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("detect_google_pay_button")
                    ) {
                        Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importar tarjeta con Google Pay")
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        FilledTonalButton(
                            onClick = {
                                WalletPlatformBridge.openGoogleWallet(context)
                                    .onSuccess(onFeedback)
                                    .onFailure { onFeedback("No se pudo abrir Google Wallet") }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("open_google_wallet_button")
                        ) {
                            Icon(Icons.Filled.Contactless, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Google Wallet", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = {
                                WalletPlatformBridge.openGooglePayMethods(context)
                                    .onSuccess(onFeedback)
                                    .onFailure { onFeedback("No se pudo abrir Google Pay") }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("open_google_pay_button")
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Google Pay", maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            WalletPlatformBridge.openNfcPaymentSettings(context)
                                .onSuccess(onFeedback)
                                .onFailure { onFeedback("No se pudo abrir ajustes NFC") }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("open_nfc_settings_button")
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Configurar pagos sin contacto")
                    }
                }
            }
        }

        // 3. Auto-Debit Income Simulator
        if (billingCards.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Autorenew, contentDescription = null, tint = DoraMintGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Simulador de Auto-Débito al Cobrar", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Mira cuánto se descuenta automáticamente al ingresar tu salario",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = simIncomeText,
                                onValueChange = { simIncomeText = it },
                                label = { Text("Próximo ingreso (${simCurrency.symbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                            CurrencyCode.entries.forEach { c ->
                                FilterChip(
                                    selected = simCurrency == c,
                                    onClick = { simCurrency = c },
                                    label = { Text(c.name) }
                                )
                            }
                        }

                        if (simulatedDebits.isEmpty()) {
                            Text(
                                text = "Con este monto no hay débitos automáticos pendientes (o las tarjetas no tienen deuda/auto-débito activo).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Surface(
                                color = DoraMintGreen.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    simulatedDebits.forEach { plan ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "• ${plan.card.alias} (••••${plan.card.last4})",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "-${FinanceUtils.formatMoney(plan.paymentInIncomeCurrency, simCurrency)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DoraCollarRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total débito tarjetas:", style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            text = FinanceUtils.formatMoney(simTotalDebited, simCurrency),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = DoraCollarRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Libre en tu cuenta:", style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            text = FinanceUtils.formatMoney(simRemainingCash, simCurrency),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = DoraMintGreen,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Cards List
        if (snapshot.cards.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CreditCard,
                            contentDescription = null,
                            tint = DoraSkyBlue,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Sin tarjetas registradas", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Agrega tus tarjetas de crédito o débito, registra lo que debes y activa el auto-débito cuando recibas ingresos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = onOpenNewCard, shape = RoundedCornerShape(14.dp)) {
                            Text("Agregar primera tarjeta")
                        }
                    }
                }
            }
        } else {
            items(snapshot.cards, key = { it.id }) { card ->
                BankCardDeckItem(
                    card = card,
                    linkedAccountName = snapshot.accounts.firstOrNull { it.id == card.linkedAccountId }?.name,
                    privacyMasked = privacyMasked,
                    onCharge = { onOpenChargeCard(card) },
                    onPay = { onOpenPayCard(card) },
                    onToggleFrozen = { onToggleFrozen(card) },
                    onToggleAutoDebit = { onToggleAutoDebit(card) },
                    onToggleWalletLink = { onToggleWalletLink(card) },
                    onEdit = { onOpenEditCard(card) },
                    onDelete = { deleteCandidate = card }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    deleteCandidate?.let { card ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Eliminar tarjeta") },
            text = { Text("¿Eliminar '${card.alias}' (•••• ${card.last4}) de tu Wallet?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCard(card)
                        deleteCandidate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DoraCollarRed)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun StatusPill(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (active) DoraMintGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) DoraMintGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun BankCardDeckItem(
    card: BankCard,
    linkedAccountName: String?,
    privacyMasked: Boolean,
    onCharge: () -> Unit,
    onPay: () -> Unit,
    onToggleFrozen: () -> Unit,
    onToggleAutoDebit: () -> Unit,
    onToggleWalletLink: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val theme = card.colorThemeEnum
    val gradientColors = if (card.cardFrozen) {
        listOf(Color(0xFF64748B), Color(0xFF334155))
    } else {
        listOf(theme.startColor, theme.endColor)
    }
    val cutoffDays = remember(card.cutoffDay) { FinanceUtils.daysUntilMonthDay(card.cutoffDay) }
    val dueDays = remember(card.dueDay) { FinanceUtils.daysUntilMonthDay(card.dueDay) }
    val cardUtil = if (card.creditLimit > 0.0) (card.debtBalance / card.creditLimit).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bank_card_item_${card.last4}")
    ) {
        Column {
            // Visual Metallic / Gradient Card Face
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(gradientColors))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.bankName.ifBlank { "Doracash Card" },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = card.alias,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Contactless,
                                contentDescription = "Sin contacto NFC",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(22.dp)
                            )
                            Surface(
                                color = Color.White.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = card.networkEnum.badgeText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (privacyMasked) "••••  ••••  ••••  ••••" else "••••  ••••  ••••  ${card.last4}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${card.cardKindEnum.label} · ${card.currency}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (card.cardKindEnum != CardKind.DEBIT) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "Deuda actual",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = if (privacyMasked) "••••••" else FinanceUtils.formatMoney(card.debtBalance, card.currencyCode),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            if (card.creditLimit > 0.0) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Disponible",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = if (privacyMasked) "••••••" else FinanceUtils.formatMoney(card.availableCredit, card.currencyCode),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (card.creditLimit > 0.0) {
                            LinearProgressIndicator(
                                progress = { cardUtil },
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.28f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }
                    }
                }
            }

            // Card Controls & Smart Details
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (card.cardKindEnum != CardKind.DEBIT) {
                        Text(
                            text = "Corte día ${card.cutoffDay} (en ${cutoffDays}d) · Pago día ${card.dueDay} (en ${dueDays}d)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dueDays <= 3 && card.debtBalance > 0.0) DoraCollarRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (dueDays <= 3 && card.debtBalance > 0.0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (!linkedAccountName.isNullOrBlank()) {
                        Text(
                            text = linkedAccountName,
                            style = MaterialTheme.typography.labelSmall,
                            color = DoraSkyBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Toggleable status badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = card.googleWalletLinked,
                        onClick = onToggleWalletLink,
                        leadingIcon = {
                            Icon(Icons.Filled.Wallet, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(if (card.googleWalletLinked) "En Wallet" else "+ Wallet", maxLines = 1) }
                    )
                    if (card.cardKindEnum != CardKind.DEBIT) {
                        FilterChip(
                            selected = card.autoDebitEnabled,
                            onClick = onToggleAutoDebit,
                            leadingIcon = {
                                Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                val txt = if (card.autoDebitEnabled) {
                                    when (card.autoDebitModeEnum) {
                                        AutoDebitMode.FULL_DEBT -> "Auto: Total"
                                        AutoDebitMode.PERCENTAGE -> "Auto: ${card.autoDebitValue.toInt()}%"
                                        AutoDebitMode.FIXED_AMOUNT -> "Auto: Cuota"
                                        AutoDebitMode.MANUAL -> "Manual"
                                    }
                                } else "Auto: Off"
                                Text(txt, maxLines = 1)
                            }
                        )
                    }
                    FilterChip(
                        selected = card.cardFrozen,
                        onClick = onToggleFrozen,
                        leadingIcon = {
                            Icon(
                                if (card.cardFrozen) Icons.Filled.Lock else Icons.Filled.AcUnit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(if (card.cardFrozen) "Congelada" else "Activa", maxLines = 1) }
                    )
                }

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onCharge,
                        enabled = !card.cardFrozen,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cargo", maxLines = 1)
                    }

                    if (card.cardKindEnum != CardKind.DEBIT) {
                        Button(
                            onClick = onPay,
                            enabled = card.debtBalance > 0.0,
                            colors = ButtonDefaults.buttonColors(containerColor = DoraMintGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pagar", maxLines = 1)
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar tarjeta", tint = DoraSkyBlue)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar tarjeta", tint = DoraDoorPink)
                    }
                }
            }
        }
    }
}
