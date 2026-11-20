package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.ui.theme.DoraSkyBlue

@Composable
fun SettingsScreen(
    snapshot: DoracashSnapshot,
    userEmail: String?,
    doraemonEnabled: Boolean,
    onToggleDoraemon: (Boolean) -> Unit,
    onOpenDoraemonChat: () -> Unit,
    onOpenAccountModal: (WalletAccount?) -> Unit,
    onDeleteAccount: (WalletAccount) -> Unit,
    onUpdateSettings: (CurrencyCode, Double, DarkThemeMode) -> Unit,
    onLoadDemoData: () -> Unit,
    onClearAllData: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentCurrency = CurrencyCode.safeValueOf(snapshot.profile.displayCurrency)
    val currentTheme = DarkThemeMode.safeValueOf(snapshot.profile.darkThemeMode)
    var rateText by remember(snapshot.profile.exchangeRate) {
        mutableStateOf(snapshot.profile.exchangeRate.toString())
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
                        Text(text = "Mis Cuentas y Billeteras", style = MaterialTheme.typography.titleLarge)
                        Button(
                            onClick = { onOpenAccountModal(null) },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("add_account_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cuenta", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    snapshot.accounts.forEach { acc ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = iconForKey(acc.iconKey),
                                        contentDescription = acc.name,
                                        tint = DoraSkyBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = acc.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${acc.typeEnum.label} · ${acc.currency}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = FinanceUtils.formatMoney(acc.balance, acc.currencyCode),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (acc.balance < 0) DoraCollarRed else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    IconButton(onClick = { onOpenAccountModal(acc) }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Editar cuenta")
                                    }
                                    if (snapshot.accounts.size > 1) {
                                        IconButton(onClick = { onDeleteAccount(acc) }) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        CurrencyCode.entries.forEach { code ->
                            FilterChip(
                                selected = currentCurrency == code,
                                onClick = {
                                    val r = rateText.toDoubleOrNull() ?: snapshot.profile.exchangeRate
                                    onUpdateSettings(code, r, currentTheme)
                                },
                                label = { Text(code.label) },
                                modifier = Modifier.weight(1f)
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
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("Aplicar")
                        }
                    }

                    Text("Tema de la aplicación", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        DarkThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = currentTheme == mode,
                                onClick = {
                                    val r = rateText.toDoubleOrNull() ?: snapshot.profile.exchangeRate
                                    onUpdateSettings(currentCurrency, r, mode)
                                },
                                label = { Text(mode.label) },
                                modifier = Modifier.weight(1f)
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
                            .height(48.dp)
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

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onLoadDemoData,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Cargar Demo")
                        }
                        OutlinedButton(
                            onClick = onClearAllData,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DoraCollarRed),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Limpiar Todo")
                        }
                    }

                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = DoraDeepBlue),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
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
