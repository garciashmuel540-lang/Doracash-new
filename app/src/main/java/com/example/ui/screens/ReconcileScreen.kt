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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.DenominationCatalog
import com.example.data.DoracashSnapshot
import com.example.data.FinanceUtils
import com.example.data.WalletAccount
import com.example.ui.theme.DoraCollarRed
import com.example.ui.theme.DoraDeepBlue
import com.example.ui.theme.DoraMintGreen
import com.example.ui.theme.DoraSkyBlue
import kotlin.math.abs

@Composable
fun ReconcileScreen(
    snapshot: DoracashSnapshot,
    onSaveReconciliation: (account: WalletAccount, counted: Double, adjust: Boolean, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (snapshot.accounts.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Primero crea una cuenta en la pestaña Cuentas.", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    var selectedAccountId by remember(snapshot.accounts) {
        mutableStateOf(snapshot.accounts.first().id)
    }
    val account = snapshot.accounts.firstOrNull { it.id == selectedAccountId } ?: snapshot.accounts.first()

    var countByBills by remember { mutableStateOf(true) }
    val counts = remember(account.id) { mutableStateMapOf<Double, Int>() }
    var directInput by remember(account.id) { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    val denominations = remember(account.currency) {
        DenominationCatalog.forCurrency(account.currencyCode)
    }

    val countedTotal = if (countByBills) {
        denominations.sumOf { d -> d * (counts[d] ?: 0) }
    } else {
        directInput.toDoubleOrNull() ?: 0.0
    }
    val diff = FinanceUtils.round2(countedTotal - account.balance)
    val isExact = abs(diff) < 0.01

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Account Selector + Mode Switch
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Conciliación de Efectivo y Cuentas", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Compara tu dinero físico real o saldo del banco contra lo registrado en Doracash.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(snapshot.accounts, key = { it.id }) { acc ->
                            FilterChip(
                                selected = acc.id == account.id,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text("${acc.name} (${acc.currency})") }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = countByBills,
                            onClick = { countByBills = true },
                            label = { Text("Contar Billetes") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !countByBills,
                            onClick = { countByBills = false },
                            label = { Text("Saldo Directo") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Result Banner Card
        item {
            val statusColor = when {
                isExact -> DoraMintGreen
                diff > 0 -> DoraSkyBlue
                else -> DoraCollarRed
            }
            val statusText = when {
                isExact -> "Cuadre Exacto"
                diff > 0 -> "Sobrante +${FinanceUtils.formatMoney(diff, account.currencyCode)}"
                else -> "Faltante ${FinanceUtils.formatMoney(diff, account.currencyCode)}"
            }

            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Saldo en sistema", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FinanceUtils.formatMoney(account.balance, account.currencyCode), style = MaterialTheme.typography.titleMedium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Conteo real", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = FinanceUtils.formatMoney(countedTotal, account.currencyCode),
                                style = MaterialTheme.typography.titleLarge,
                                color = DoraDeepBlue
                            )
                        }
                    }

                    Surface(
                        color = statusColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = statusText, style = MaterialTheme.typography.titleMedium, color = statusColor, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Nota de arqueo (opcional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                onSaveReconciliation(account, countedTotal, false, noteInput)
                                noteInput = ""
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("reconcile_audit_only_button")
                        ) {
                            Text("Solo Auditoría")
                        }
                        Button(
                            onClick = {
                                onSaveReconciliation(account, countedTotal, true, noteInput)
                                noteInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DoraDeepBlue),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("reconcile_adjust_button")
                        ) {
                            Text("Conciliar y Ajustar")
                        }
                    }
                }
            }
        }

        // Denomination Counter OR Direct Input
        if (countByBills) {
            items(denominations) { denom ->
                val qty = counts[denom] ?: 0
                val sub = denom * qty
                val denomLabel = if (denom >= 1.0) denom.toInt().toString() else "%.2f".format(denom)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Text(
                                text = "${account.currencyCode.symbol} $denomLabel",
                                style = MaterialTheme.typography.titleMedium,
                                color = DoraDeepBlue,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Subtotal: ${FinanceUtils.formatMoney(sub, account.currencyCode)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = { counts[denom] = (qty - 1).coerceAtLeast(0) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Restar billete", modifier = Modifier.padding(10.dp))
                            }
                            Text(
                                text = qty.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(32.dp)
                            )
                            Surface(
                                onClick = { counts[denom] = qty + 1 },
                                shape = RoundedCornerShape(12.dp),
                                color = DoraSkyBlue,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "Sumar billete",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = directInput,
                            onValueChange = { directInput = it },
                            label = { Text("Saldo real en ${account.name} (${account.currency})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reconcile_direct_input")
                        )
                    }
                }
            }
        }

        // Reconciliation History
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Historial de Conciliaciones", style = MaterialTheme.typography.titleLarge)
                    if (snapshot.reconciliations.isEmpty()) {
                        Text(
                            text = "Aún no has registrado arqueos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        snapshot.reconciliations.take(10).forEach { rec ->
                            val accName = snapshot.accounts.firstOrNull { it.id == rec.accountId }?.name ?: "Cuenta"
                            val diffClr = when {
                                abs(rec.difference) < 0.01 -> DoraMintGreen
                                rec.difference > 0 -> DoraSkyBlue
                                else -> DoraCollarRed
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "$accName · ${rec.dateIso}", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = rec.note.ifBlank { if (rec.balanceAdjusted) "Saldo ajustado" else "Solo auditoría" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = if (abs(rec.difference) < 0.01) "Exacto"
                                        else (if (rec.difference > 0) "+" else "") + FinanceUtils.formatMoney(rec.difference, rec.currencyCode),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = diffClr,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}
