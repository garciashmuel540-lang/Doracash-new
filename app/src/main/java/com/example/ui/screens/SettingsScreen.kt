@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CurrencyCode
import com.example.data.DarkThemeMode
import com.example.data.DoracashSnapshot
import com.example.data.FinanceUtils
import com.example.data.WalletAccount
import com.example.ui.components.DoraemonMiniHeadIcon
import com.example.ui.components.iconForKey
import com.example.ui.theme.DoraCollarRed
import com.example.ui.theme.DoraDeepBlue
import com.example.ui.theme.DoraMintGreen
import com.example.ui.theme.DoraSkyBlue

@Composable
fun SettingsScreen(
    snapshot: DoracashSnapshot,
    userEmail: String?,
    doraemonEnabled: Boolean,
    currentDeviceId: String,
    currentVersionName: String,
    currentVersionCode: Int,
    onToggleDoraemon: (Boolean) -> Unit,
    onOpenDoraemonChat: () -> Unit,
    onOpenAccountModal: (WalletAccount?) -> Unit,
    onDeleteAccount: (WalletAccount) -> Unit,
    onUpdateSettings: (CurrencyCode, Double, DarkThemeMode) -> Unit,
    onPublishUpdateChannel: (Int, String, String, String, Boolean) -> Unit,
    onRemoveDevice: (String) -> Unit,
    onLoadDemoData: () -> Unit,
    onClearAllData: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentCurrency = CurrencyCode.safeValueOf(snapshot.profile.displayCurrency)
    val currentTheme = DarkThemeMode.safeValueOf(snapshot.profile.darkThemeMode)
    var rateText by remember(snapshot.profile.exchangeRate) {
        mutableStateOf(snapshot.profile.exchangeRate.toString())
    }

    val rel = snapshot.releaseInfo
    var updateVerCodeStr by remember(rel?.latestVersionCode) {
        mutableStateOf((rel?.latestVersionCode ?: currentVersionCode).toString())
    }
    var updateVerName by remember(rel?.latestVersionName) {
        mutableStateOf(rel?.latestVersionName?.ifBlank { currentVersionName } ?: currentVersionName)
    }
    var updateDownloadUrl by remember(rel?.downloadUrl) {
        mutableStateOf(rel?.downloadUrl ?: "https://github.com/garciashmuel540-lang/Doracash-new/releases/latest")
    }
    var updateNotes by remember(rel?.releaseNotes) {
        mutableStateOf(rel?.releaseNotes ?: "Mejoras en movimientos, quincena, botones y sincronización.")
    }
    var forceUpdateNotice by remember(rel?.forceNotice) {
        mutableStateOf(rel?.forceNotice ?: false)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Wallets & Accounts
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Mis Cuentas y Billeteras",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Button(
                            onClick = { onOpenAccountModal(null) },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("add_account_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cuenta", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }
                    }

                    snapshot.accounts.forEach { acc ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = iconForKey(acc.iconKey),
                                            contentDescription = acc.name,
                                            tint = DoraSkyBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = acc.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${acc.typeEnum.label} · ${acc.currency}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Text(
                                        text = FinanceUtils.formatMoney(acc.balance, acc.currencyCode),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (acc.balance < 0) DoraCollarRed else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = { onOpenAccountModal(acc) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Editar cuenta", tint = DoraSkyBlue)
                                    }
                                    if (snapshot.accounts.size > 1) {
                                        IconButton(
                                            onClick = { onDeleteAccount(acc) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar cuenta", tint = DoraCollarRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Firestore Cloud, Synced Phones & Multi-Device Update Channel
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDone,
                            contentDescription = null,
                            tint = DoraMintGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nube Firestore y Teléfonos", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Tus presupuestos, ahorros, pagos y movimientos se sincronizan por cuenta.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = DoraMintGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Versión instalada en este teléfono: v$currentVersionName ($currentVersionCode) · ${snapshot.devices.size} teléfono(s) vinculados",
                            style = MaterialTheme.typography.labelMedium,
                            color = DoraMintGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    if (snapshot.devices.isNotEmpty()) {
                        Text("Teléfonos sincronizados en esta cuenta", style = MaterialTheme.typography.labelLarge)
                        snapshot.devices.forEach { dev ->
                            val isCurrent = dev.id == currentDeviceId
                            val outdated = dev.versionCode < (rel?.latestVersionCode ?: currentVersionCode)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PhoneAndroid,
                                            contentDescription = null,
                                            tint = if (isCurrent) DoraSkyBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (isCurrent) "${dev.deviceName} (Este teléfono)" else dev.deviceName,
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "v${dev.versionName} (código ${dev.versionCode})" +
                                                    if (outdated) " · Pendiente de actualizar" else " · Al día",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (outdated) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (!isCurrent) {
                                        IconButton(
                                            onClick = { onRemoveDevice(dev.id) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "Quitar teléfono",
                                                tint = DoraCollarRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    Text("Canal de actualización para otros teléfonos", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = updateVerCodeStr,
                            onValueChange = { updateVerCodeStr = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text("Código") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = updateVerName,
                            onValueChange = { updateVerName = it.take(24) },
                            label = { Text("Versión") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = updateDownloadUrl,
                        onValueChange = { updateDownloadUrl = it.take(400) },
                        label = { Text("Enlace de descarga HTTPS (GitHub Release / Drive)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = updateNotes,
                        onValueChange = { updateNotes = it.take(240) },
                        label = { Text("Novedades para tus otros teléfonos") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Avisar inmediatamente al abrir otros teléfonos",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Switch(checked = forceUpdateNotice, onCheckedChange = { forceUpdateNotice = it })
                    }

                    Button(
                        onClick = {
                            val code = updateVerCodeStr.toIntOrNull() ?: currentVersionCode
                            onPublishUpdateChannel(
                                code,
                                updateVerName.ifBlank { currentVersionName },
                                updateDownloadUrl.trim(),
                                updateNotes.trim(),
                                forceUpdateNotice
                            )
                        },
                        enabled = (updateVerCodeStr.toIntOrNull() ?: 0) >= 1,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("publish_update_channel_button")
                    ) {
                        Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Notificar actualización a mis teléfonos", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    val validShareUrl = updateDownloadUrl.trim().startsWith("https://")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                val target = updateDownloadUrl.trim().takeIf { it.startsWith("https://") }
                                    ?: "https://github.com/garciashmuel540-lang/Doracash-new/releases/latest"
                                val shareMsg = "Descarga Doracash v$updateVerName en tu otro teléfono e inicia sesión con tu misma cuenta Google para sincronizar todo:\n$target"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Doracash v$updateVerName")
                                    putExtra(Intent.EXTRA_TEXT, shareMsg)
                                }
                                runCatching {
                                    context.startActivity(Intent.createChooser(sendIntent, "Compartir enlace de Doracash"))
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pasar enlace a otro teléfono", maxLines = 1)
                        }
                        if (validShareUrl) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateDownloadUrl.trim())))
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Abrir descarga", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Currency, Exchange Rate & Theme
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = "Moneda, Tasa y Apariencia", style = MaterialTheme.typography.titleLarge)

                    Text("Moneda principal de visualización", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CurrencyCode.entries.forEach { code ->
                            FilterChip(
                                selected = currentCurrency == code,
                                onClick = {
                                    val r = rateText.toDoubleOrNull() ?: snapshot.profile.exchangeRate
                                    onUpdateSettings(code, r, currentTheme)
                                },
                                label = { Text(code.label, maxLines = 1) }
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rateText,
                            onValueChange = { rateText = it },
                            label = { Text("Tasa (1 USD = ? C$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val parsed = rateText.toDoubleOrNull()
                                if (parsed != null && parsed > 0.0) {
                                    onUpdateSettings(currentCurrency, parsed, currentTheme)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.heightIn(min = 52.dp)
                        ) {
                            Text("Aplicar", maxLines = 1)
                        }
                    }

                    Text("Tema de la aplicación", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DarkThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = currentTheme == mode,
                                onClick = {
                                    val r = rateText.toDoubleOrNull() ?: snapshot.profile.exchangeRate
                                    onUpdateSettings(currentCurrency, r, mode)
                                },
                                label = { Text(mode.label, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        }

        // Doraemon AI Mascot Settings
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            DoraemonMiniHeadIcon(
                                active = doraemonEnabled,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Mascota Doraemon IA", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Solo aparece en pantalla cuando tiene un aviso importante",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = doraemonEnabled,
                            onCheckedChange = onToggleDoraemon,
                            modifier = Modifier.testTag("settings_doraemon_switch")
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenDoraemonChat,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("settings_open_doraemon_chat")
                    ) {
                        Text("Hablar con Doraemon IA")
                    }
                }
            }
        }

        // Data & Account Session
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Datos y Sesión Google", style = MaterialTheme.typography.titleLarge)
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = "Sincronizado como: $userEmail",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onLoadDemoData,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Cargar Demo", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onClearAllData,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DoraCollarRed),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Limpiar Todo", maxLines = 1)
                        }
                    }

                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = DoraDeepBlue),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar sesión de Google")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}
