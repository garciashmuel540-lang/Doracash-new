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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.CategoryCatalog
import com.example.data.CurrencyCode
import com.example.data.DoracashSnapshot
import com.example.data.EntryClassification
import com.example.data.FinanceTransaction
import com.example.data.FinanceUtils
import com.example.data.SavingsType
import com.example.data.TxType
import com.example.ui.components.TransactionCompactRow
import com.example.ui.theme.DoraCollarRed
import com.example.ui.theme.DoraMintGreen
import com.example.ui.theme.DoraSkyBlue

@Composable
fun TransactionsScreen(
    snapshot: DoracashSnapshot,
    onDeleteTx: (FinanceTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<TxType?>(null) }
    var selectedClassification by remember { mutableStateOf<EntryClassification?>(null) }
    var selectedSavingsType by remember { mutableStateOf<SavingsType?>(null) }
    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    var filterMonthOnly by remember { mutableStateOf(true) }

    val disp = CurrencyCode.safeValueOf(snapshot.profile.displayCurrency)
    val rate = snapshot.profile.exchangeRate
    val currentYm = remember { FinanceUtils.currentYearMonth() }

    val filtered = remember(
        snapshot.transactions,
        searchQuery,
        selectedType,
        selectedClassification,
        selectedSavingsType,
        selectedAccountId,
        filterMonthOnly
    ) {
        snapshot.transactions.filter { tx ->
            if (filterMonthOnly && !tx.dateIso.startsWith(currentYm)) return@filter false
            if (selectedType != null && tx.typeEnum != selectedType) return@filter false
            if (selectedClassification != null) {
                val txClass = CategoryCatalog.classificationOf(tx.categoryId)
                if (txClass != selectedClassification) return@filter false
                if (selectedClassification == EntryClassification.SAVINGS && selectedSavingsType != null) {
                    if (CategoryCatalog.savingsTypeOf(tx.categoryId) != selectedSavingsType) return@filter false
                }
            }
            if (selectedAccountId != null && tx.accountId != selectedAccountId && tx.toAccountId != selectedAccountId) {
                return@filter false
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                val catName = CategoryCatalog.find(tx.typeEnum == TxType.INCOME, tx.categoryId).name.lowercase()
                if (!tx.note.lowercase().contains(q) && !catName.contains(q)) return@filter false
            }
            true
        }
    }

    val totalInc = remember(filtered, disp, rate) {
        filtered.filter { it.typeEnum == TxType.INCOME }
            .sumOf { FinanceUtils.convertCurrency(it.amount, it.currencyCode, disp, rate) }
    }
    val totalExp = remember(filtered, disp, rate) {
        filtered.filter { it.typeEnum == TxType.EXPENSE }
            .sumOf { FinanceUtils.convertCurrency(it.amount, it.currencyCode, disp, rate) }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por nota o categoría...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tx_search_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = filterMonthOnly,
                                onClick = { filterMonthOnly = !filterMonthOnly },
                                label = { Text(if (filterMonthOnly) "Este mes" else "Todos los meses") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedType == null,
                                onClick = { selectedType = null },
                                label = { Text("Todos") }
                            )
                        }
                        items(TxType.entries) { t ->
                            FilterChip(
                                selected = selectedType == t,
                                onClick = { selectedType = if (selectedType == t) null else t },
                                label = { Text(t.label) }
                            )
                        }
                    }

                    // Classification filters: Deudas / Ahorros (Cajita, LAFISE, Prestamo)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedClassification == EntryClassification.DEBT,
                                onClick = {
                                    selectedClassification = if (selectedClassification == EntryClassification.DEBT) {
                                        null
                                    } else {
                                        selectedSavingsType = null
                                        EntryClassification.DEBT
                                    }
                                },
                                label = { Text("Deudas") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedClassification == EntryClassification.SAVINGS,
                                onClick = {
                                    if (selectedClassification == EntryClassification.SAVINGS) {
                                        selectedClassification = null
                                        selectedSavingsType = null
                                    } else {
                                        selectedClassification = EntryClassification.SAVINGS
                                    }
                                },
                                label = { Text("Ahorros") }
                            )
                        }
                        if (selectedClassification == EntryClassification.SAVINGS) {
                            items(SavingsType.entries) { st ->
                                FilterChip(
                                    selected = selectedSavingsType == st,
                                    onClick = {
                                        selectedSavingsType = if (selectedSavingsType == st) null else st
                                    },
                                    label = { Text(st.shortLabel) }
                                )
                            }
                        }
                    }

                    if (snapshot.accounts.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedAccountId == null,
                                    onClick = { selectedAccountId = null },
                                    label = { Text("Todas las cuentas") }
                                )
                            }
                            items(snapshot.accounts, key = { it.id }) { acc ->
                                FilterChip(
                                    selected = selectedAccountId == acc.id,
                                    onClick = { selectedAccountId = if (selectedAccountId == acc.id) null else acc.id },
                                    label = { Text(acc.name) }
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "+${FinanceUtils.formatMoney(totalInc, disp)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = DoraMintGreen
                        )
                        Text(
                            text = "-${FinanceUtils.formatMoney(totalExp, disp)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = DoraCollarRed
                        )
                        Text(
                            text = "${filtered.size} movs.",
                            style = MaterialTheme.typography.labelLarge,
                            color = DoraSkyBlue
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "No hay movimientos con este filtro",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Usa el botón Puerta Mágica (+) para agregar un ingreso, gasto o transferencia.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { tx ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TransactionCompactRow(
                        tx = tx,
                        accounts = snapshot.accounts,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onDeleteTx(tx) },
                        modifier = Modifier.testTag("delete_tx_${tx.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Eliminar movimiento",
                            tint = DoraCollarRed
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}
