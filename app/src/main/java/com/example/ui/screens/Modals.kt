@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AccountType
import com.example.data.AutoDebitMode
import com.example.data.BankCard
import com.example.data.BillFrequency
import com.example.data.CardColorTheme
import com.example.data.CardKind
import com.example.data.CardNetwork
import com.example.data.CategoryCatalog
import com.example.data.CurrencyCode
import com.example.data.EntryClassification
import com.example.data.FinanceUtils
import com.example.data.SavingsGoal
import com.example.data.SavingsType
import com.example.data.TxType
import com.example.data.WalletAccount
import com.example.ui.theme.DoraCollarRed
import com.example.ui.theme.DoraMintGreen
import com.example.ui.theme.DoraSkyBlue
import com.example.wallet.CardDetector
import com.example.wallet.DetectedCardMetadata
import kotlinx.coroutines.launch

@Composable
fun TransactionModalDialog(
    initialType: TxType,
    accounts: List<WalletAccount>,
    goals: List<SavingsGoal> = emptyList(),
    cards: List<BankCard> = emptyList(),
    exchangeRate: Double = 36.62,
    onDismiss: () -> Unit,
    onSave: (
        txType: TxType,
        amount: Double,
        account: WalletAccount,
        toAccount: WalletAccount?,
        categoryId: String,
        note: String,
        dateIso: String,
        savingsType: SavingsType?,
        targetSavingsGoal: SavingsGoal?,
        customSavingsName: String?,
        applyAutoCardDebit: Boolean,
        pending: Boolean
    ) -> Unit
) {
    if (accounts.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Sin cuentas") },
            text = { Text("Crea una cuenta primero en la pestaña Cuentas.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido") } }
        )
        return
    }

    var txType by remember { mutableStateOf(initialType) }
    var chosenPending by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Boolean?>(null) }
    val pending = chosenPending == true
    var amountStr by remember { mutableStateOf("") }
    var dateIso by remember { mutableStateOf(FinanceUtils.todayIso()) }
    var note by remember { mutableStateOf("") }
    var selectedAcc by remember { mutableStateOf(accounts.first()) }
    var selectedToAcc by remember { mutableStateOf(accounts.getOrElse(1) { accounts.first() }) }

    var classification by remember(txType) { mutableStateOf(EntryClassification.NORMAL) }
    var savingsType by remember { mutableStateOf(SavingsType.CAJITA) }
    val matchingGoals = remember(goals, savingsType) {
        goals.filter { it.savingsType == savingsType }
    }
    var selectedGoalId by remember(matchingGoals) {
        mutableStateOf(matchingGoals.firstOrNull()?.id)
    }
    var customSavingsName by remember { mutableStateOf("") }

    val normalCategories = remember(txType) {
        val raw = if (txType == TxType.INCOME) CategoryCatalog.incomeCategories else CategoryCatalog.expenseCategories
        raw.filter { CategoryCatalog.classificationOf(it.id) == EntryClassification.NORMAL }
    }
    var selectedCatId by remember(txType) { mutableStateOf(normalCategories.first().id) }
    val hasEligibleAutoCards = remember(cards) {
        cards.any {
            it.cardKindEnum != CardKind.DEBIT && it.autoDebitEnabled &&
                !it.cardFrozen &&
                it.debtBalance > 0.005 &&
                it.autoDebitModeEnum != AutoDebitMode.MANUAL
        }
    }
    var applyAutoCardDebit by remember(cards) { mutableStateOf(hasEligibleAutoCards) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Puerta Mágica", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TxType.entries.forEach { t ->
                        FilterChip(
                            selected = txType == t,
                            onClick = { txType = t },
                            label = { Text(t.label, maxLines = 1) }
                        )
                    }
                }

                MovementStatusPicker(chosenPending) { chosenPending = it }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Monto (${selectedAcc.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_amount_input")
                )

                Text(
                    text = if (txType == TxType.TRANSFER) "Cuenta origen" else "Cuenta",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts, key = { it.id }) { acc ->
                        FilterChip(
                            selected = selectedAcc.id == acc.id,
                            onClick = { selectedAcc = acc },
                            label = { Text("${acc.name} (${acc.currency})") }
                        )
                    }
                }

                if (txType == TxType.TRANSFER) {
                    Text("Cuenta destino", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(accounts, key = { it.id }) { acc ->
                            FilterChip(
                                selected = selectedToAcc.id == acc.id,
                                onClick = { selectedToAcc = acc },
                                label = { Text("${acc.name} (${acc.currency})") }
                            )
                        }
                    }
                } else {
                    Text("Clasificación", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(EntryClassification.entries) { cls ->
                            FilterChip(
                                selected = classification == cls,
                                onClick = { classification = cls },
                                label = { Text(cls.label) },
                                modifier = Modifier.testTag("tx_class_${cls.name.lowercase()}")
                            )
                        }
                    }

                    when (classification) {
                        EntryClassification.NORMAL -> {
                            Text("Categoría", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(normalCategories, key = { it.id }) { cat ->
                                    FilterChip(
                                        selected = selectedCatId == cat.id,
                                        onClick = { selectedCatId = cat.id },
                                        label = { Text(cat.name) }
                                    )
                                }
                            }
                        }

                        EntryClassification.DEBT -> {
                            Surface(
                                color = DoraCollarRed.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (txType == TxType.INCOME) {
                                        "Se registrará como deuda o préstamo recibido."
                                    } else {
                                        "Se registrará como abono o pago de deuda."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DoraCollarRed,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        EntryClassification.SAVINGS -> {
                            Text("¿Qué tipo de ahorro es?", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(SavingsType.entries) { st ->
                                    FilterChip(
                                        selected = savingsType == st,
                                        onClick = { savingsType = st },
                                        label = { Text(st.shortLabel) },
                                        modifier = Modifier.testTag("tx_savings_type_${st.key}")
                                    )
                                }
                            }
                            Text(
                                text = savingsType.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text("Sumar a tu ahorro:", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(matchingGoals, key = { it.id }) { goal ->
                                    FilterChip(
                                        selected = selectedGoalId == goal.id,
                                        onClick = { selectedGoalId = goal.id },
                                        label = { Text(goal.name) }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = selectedGoalId == null,
                                        onClick = { selectedGoalId = null },
                                        label = { Text("+ Nuevo apartado") }
                                    )
                                }
                            }

                            if (selectedGoalId == null) {
                                OutlinedTextField(
                                    value = customSavingsName,
                                    onValueChange = { customSavingsName = it },
                                    label = { Text("Nombre del apartado (ej. ${savingsType.label})") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Surface(
                                color = DoraMintGreen.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Al guardar, este monto se sumará automáticamente a Mis Ahorros (${savingsType.shortLabel}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DoraMintGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = dateIso,
                    onValueChange = { dateIso = it },
                    label = { Text("Fecha (AAAA-MM-DD)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota / Descripción") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_note_input")
                )

                if (txType == TxType.INCOME && hasEligibleAutoCards && !pending) {
                    val liveAmt = amountStr.toDoubleOrNull() ?: 0.0
                    val previewDebits = remember(liveAmt, selectedAcc, cards, exchangeRate) {
                        FinanceUtils.planAutoCardDebits(liveAmt, selectedAcc.currencyCode, cards, exchangeRate)
                    }
                    Surface(
                        color = DoraSkyBlue.copy(alpha = 0.09f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto-debitar deuda de tarjetas",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = DoraSkyBlue
                                    )
                                    Text(
                                        text = "Descuenta tu deuda configurada al instante",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = applyAutoCardDebit,
                                    onCheckedChange = { applyAutoCardDebit = it },
                                    modifier = Modifier.testTag("tx_auto_debit_switch")
                                )
                            }
                            if (applyAutoCardDebit && previewDebits.isNotEmpty()) {
                                val totalDeb = previewDebits.sumOf { it.paymentInIncomeCurrency }
                                val netLeft = (liveAmt - totalDeb).coerceAtLeast(0.0)
                                previewDebits.forEach { d ->
                                    Text(
                                        text = "• ${d.card.alias} (••••${d.card.last4}): -${FinanceUtils.formatMoney(d.paymentInIncomeCurrency, selectedAcc.currencyCode)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DoraCollarRed
                                    )
                                }
                                Text(
                                    text = "Te quedaría libre en cuenta: ${FinanceUtils.formatMoney(netLeft, selectedAcc.currencyCode)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DoraMintGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val amt = amountStr.toDoubleOrNull() ?: 0.0
            val validDate = isValidMovementDate(dateIso)
            val validTransfer = txType != TxType.TRANSFER || selectedToAcc.id != selectedAcc.id
            val selectedGoal = matchingGoals.firstOrNull { it.id == selectedGoalId }
            val resolvedCatId = when {
                txType == TxType.TRANSFER -> "transfer"
                classification == EntryClassification.DEBT -> if (txType == TxType.INCOME) "deudas_i" else "deudas"
                classification == EntryClassification.SAVINGS -> CategoryCatalog.buildSavingsCategoryId(savingsType, selectedGoal?.id)
                else -> selectedCatId
            }
            val effectiveSavingsType = if (txType != TxType.TRANSFER && classification == EntryClassification.SAVINGS) {
                savingsType
            } else null

            Button(
                onClick = {
                    onSave(
                        txType,
                        amt,
                        selectedAcc,
                        if (txType == TxType.TRANSFER) selectedToAcc else null,
                        resolvedCatId,
                        note,
                        dateIso,
                        effectiveSavingsType,
                        if (effectiveSavingsType != null) selectedGoal else null,
                        if (effectiveSavingsType != null && selectedGoal == null) {
                            customSavingsName.trim().takeIf { it.isNotBlank() }
                        } else null,
                        txType == TxType.INCOME && applyAutoCardDebit,
                        pending
                    )
                },
                enabled = chosenPending != null && amt.isFinite() && FinanceUtils.round2(amt) > 0.0 && validDate && validTransfer,
                modifier = Modifier.testTag("save_tx_button")
            ) {
                Text(if (pending) "Guardar pendiente" else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun ScheduleBillDialog(
    defaultDateIso: String,
    accounts: List<WalletAccount>,
    goals: List<SavingsGoal> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        currency: CurrencyCode,
        accountId: String,
        categoryId: String,
        frequency: BillFrequency,
        startDateIso: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(CurrencyCode.NIO) }
    var frequency by remember { mutableStateOf(BillFrequency.MONTHLY) }
    var dateIso by remember { mutableStateOf(defaultDateIso) }
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }

    var classification by remember { mutableStateOf(EntryClassification.NORMAL) }
    var normalCatId by remember { mutableStateOf("servicios") }
    var savingsType by remember { mutableStateOf(SavingsType.CAJITA) }
    val matchingGoals = remember(goals, savingsType) {
        goals.filter { it.savingsType == savingsType }
    }
    var selectedGoalId by remember(matchingGoals) {
        mutableStateOf(matchingGoals.firstOrNull()?.id)
    }

    val normalBillCats = remember {
        CategoryCatalog.expenseCategories.filter { CategoryCatalog.classificationOf(it.id) == EntryClassification.NORMAL }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Programar Pago / Compromiso", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Concepto (ej. Internet, Deuda, Ahorro LAFISE)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CurrencyCode.entries.forEach { c ->
                        FilterChip(
                            selected = currency == c,
                            onClick = { currency = c },
                            label = { Text(c.name, maxLines = 1) }
                        )
                    }
                }

                Text("Clasificación del pago", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EntryClassification.entries) { cls ->
                        val label = when (cls) {
                            EntryClassification.NORMAL -> "Gasto / Servicio"
                            EntryClassification.DEBT -> "Deuda"
                            EntryClassification.SAVINGS -> "Parte del ahorro"
                        }
                        FilterChip(
                            selected = classification == cls,
                            onClick = { classification = cls },
                            label = { Text(label) },
                            modifier = Modifier.testTag("bill_class_${cls.name.lowercase()}")
                        )
                    }
                }

                when (classification) {
                    EntryClassification.NORMAL -> {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(normalBillCats, key = { it.id }) { c ->
                                FilterChip(
                                    selected = normalCatId == c.id,
                                    onClick = { normalCatId = c.id },
                                    label = { Text(c.name) }
                                )
                            }
                        }
                    }

                    EntryClassification.DEBT -> {
                        Surface(
                            color = DoraCollarRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Clasificado como cuota o pago de deuda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DoraCollarRed,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    EntryClassification.SAVINGS -> {
                        Text("Tipo de ahorro", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SavingsType.entries) { st ->
                                FilterChip(
                                    selected = savingsType == st,
                                    onClick = { savingsType = st },
                                    label = { Text(st.shortLabel) },
                                    modifier = Modifier.testTag("bill_savings_type_${st.key}")
                                )
                            }
                        }
                        if (matchingGoals.isNotEmpty()) {
                            Text("Apartado destino (opcional)", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(matchingGoals, key = { it.id }) { goal ->
                                    FilterChip(
                                        selected = selectedGoalId == goal.id,
                                        onClick = { selectedGoalId = goal.id },
                                        label = { Text(goal.name) }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = selectedGoalId == null,
                                        onClick = { selectedGoalId = null },
                                        label = { Text("Crear nuevo") }
                                    )
                                }
                            }
                        }
                        Surface(
                            color = DoraMintGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Al marcar este pago como pagado, se sumará a Mis Ahorros (${savingsType.shortLabel}).",
                                style = MaterialTheme.typography.bodySmall,
                                color = DoraMintGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dateIso,
                    onValueChange = { dateIso = it },
                    label = { Text("Fecha (AAAA-MM-DD)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Frecuencia", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BillFrequency.entries) { f ->
                        FilterChip(
                            selected = frequency == f,
                            onClick = { frequency = f },
                            label = { Text(f.label) }
                        )
                    }
                }
                if (accounts.isNotEmpty()) {
                    Text("Cuenta de pago", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(accounts, key = { it.id }) { a ->
                            FilterChip(
                                selected = accountId == a.id,
                                onClick = { accountId = a.id },
                                label = { Text(a.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val amt = amountStr.toDoubleOrNull() ?: 0.0
            val resolvedCategoryId = when (classification) {
                EntryClassification.NORMAL -> normalCatId
                EntryClassification.DEBT -> "deudas"
                EntryClassification.SAVINGS -> CategoryCatalog.buildSavingsCategoryId(savingsType, selectedGoalId)
            }
            Button(
                onClick = { onSave(title, amt, currency, accountId, resolvedCategoryId, frequency, dateIso) },
                enabled = title.isNotBlank() && amt > 0.0 && dateIso.length == 10 && accountId.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun AccountEditDialog(
    existing: WalletAccount?,
    onDismiss: () -> Unit,
    onSave: (name: String, type: AccountType, currency: CurrencyCode, balance: Double, iconKey: String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.typeEnum ?: AccountType.CASH) }
    var currency by remember { mutableStateOf(existing?.currencyCode ?: CurrencyCode.NIO) }
    var balanceStr by remember { mutableStateOf(existing?.balance?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nueva Cuenta" else "Editar Cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (ej. Efectivo, BAC, Lafise)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AccountType.entries) { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label) }
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CurrencyCode.entries.forEach { c ->
                        FilterChip(
                            selected = currency == c,
                            onClick = { if (existing == null) currency = c },
                            enabled = existing == null,
                            label = { Text(c.label, maxLines = 1) }
                        )
                    }
                }
                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("Saldo (${currency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val bal = balanceStr.toDoubleOrNull()
            Button(
                onClick = { onSave(name, type, currency, bal ?: 0.0, type.defaultIconKey) },
                enabled = name.isNotBlank() && bal != null
            ) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun BudgetCreateDialog(
    onDismiss: () -> Unit,
    onSave: (categoryId: String, limitNio: Double) -> Unit
) {
    val budgetCats = remember {
        CategoryCatalog.expenseCategories.filter {
            CategoryCatalog.classificationOf(it.id) != EntryClassification.SAVINGS && it.id != "ajuste"
        }
    }
    var categoryId by remember { mutableStateOf(budgetCats.first().id) }
    var limitStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Límite Mensual") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(budgetCats, key = { it.id }) { c ->
                        FilterChip(
                            selected = categoryId == c.id,
                            onClick = { categoryId = c.id },
                            label = { Text(c.name) }
                        )
                    }
                }
                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("Límite mensual en C$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val limit = limitStr.toDoubleOrNull() ?: 0.0
            Button(onClick = { onSave(categoryId, limit) }, enabled = limit > 0.0) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun GoalCreateDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, targetNio: Double, iconKey: String, deadlineIso: String, initialNio: Double) -> Unit
) {
    var savingsType by remember { mutableStateOf(SavingsType.CAJITA) }
    var name by remember { mutableStateOf("") }
    var initialStr by remember { mutableStateOf("") }
    var hasGoalTarget by remember { mutableStateOf(false) }
    var targetStr by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }

    val examplePlaceholder = when (savingsType) {
        SavingsType.CAJITA -> "Ej. Cajita de Emergencia"
        SavingsType.LAFISE -> "Ej. Cuenta Ahorro LAFISE"
        SavingsType.PRESTAMO -> "Ej. Préstamo a Carlos"
        SavingsType.OTRO -> "Ej. Fondo de Viaje"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Ahorro", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Tipo de ahorro", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SavingsType.entries) { st ->
                        FilterChip(
                            selected = savingsType == st,
                            onClick = { savingsType = st },
                            label = { Text(st.shortLabel) },
                            modifier = Modifier.testTag("create_savings_type_${st.key}")
                        )
                    }
                }
                Text(
                    text = savingsType.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre ($examplePlaceholder)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = initialStr,
                    onValueChange = { initialStr = it },
                    label = {
                        Text(
                            if (savingsType == SavingsType.PRESTAMO) "Monto ya prestado en C$ (opcional)"
                            else "Saldo inicial en C$ (opcional)"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                        text = "Definir meta objetivo (opcional)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = hasGoalTarget,
                        onCheckedChange = { hasGoalTarget = it }
                    )
                }

                if (hasGoalTarget) {
                    OutlinedTextField(
                        value = targetStr,
                        onValueChange = { targetStr = it },
                        label = { Text("Meta objetivo en C$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = deadline,
                        onValueChange = { deadline = it },
                        label = { Text("Fecha límite (opcional, AAAA-MM-DD)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            val initialAmt = if (initialStr.isBlank()) 0.0 else (initialStr.toDoubleOrNull() ?: -1.0)
            val targetAmt = if (hasGoalTarget) (targetStr.toDoubleOrNull() ?: 0.0) else 1.0
            val validTarget = !hasGoalTarget || targetAmt > 1.0
            Button(
                onClick = {
                    onSave(
                        name,
                        targetAmt,
                        savingsType.key,
                        if (hasGoalTarget) deadline else "",
                        initialAmt.coerceAtLeast(0.0)
                    )
                },
                enabled = name.isNotBlank() && initialAmt >= 0.0 && validTarget
            ) {
                Text("Guardar Ahorro")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun GoalDepositDialog(
    goal: SavingsGoal,
    accounts: List<WalletAccount>,
    onDismiss: () -> Unit,
    onDeposit: (account: WalletAccount, amount: Double) -> Unit
) {
    if (accounts.isEmpty()) { onDismiss(); return }
    var selectedAcc by remember { mutableStateOf(accounts.first()) }
    var amountStr by remember { mutableStateOf("") }
    val isLoan = goal.savingsType == SavingsType.PRESTAMO

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isLoan) "Prestar más a ${goal.name}" else "Abonar a ${goal.name}"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tipo: ${goal.savingsType.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts, key = { it.id }) { acc ->
                        FilterChip(
                            selected = selectedAcc.id == acc.id,
                            onClick = { selectedAcc = acc },
                            label = { Text("${acc.name} (${acc.currency})") }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Monto (${selectedAcc.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amt = amountStr.toDoubleOrNull() ?: 0.0
            Button(onClick = { onDeposit(selectedAcc, amt) }, enabled = amt > 0.0) {
                Text(if (isLoan) "Prestar" else "Abonar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun GoalRecoverDialog(
    goal: SavingsGoal,
    accounts: List<WalletAccount>,
    onDismiss: () -> Unit,
    onRecover: (account: WalletAccount, amount: Double) -> Unit
) {
    if (accounts.isEmpty()) { onDismiss(); return }
    var selectedAcc by remember { mutableStateOf(accounts.first()) }
    var amountStr by remember { mutableStateOf("") }
    val isLoan = goal.savingsType == SavingsType.PRESTAMO

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isLoan) "Recuperar préstamo · ${goal.name}" else "Retirar de ${goal.name}"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Disponible: ${FinanceUtils.formatMoney(goal.currentNio, CurrencyCode.NIO)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = goal.savingsType.color
                )
                Text("Cuenta destino donde ingresa:", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts, key = { it.id }) { acc ->
                        FilterChip(
                            selected = selectedAcc.id == acc.id,
                            onClick = { selectedAcc = acc },
                            label = { Text("${acc.name} (${acc.currency})") }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Monto a recibir (${selectedAcc.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amt = amountStr.toDoubleOrNull() ?: 0.0
            Button(onClick = { onRecover(selectedAcc, amt) }, enabled = amt > 0.0) {
                Text(if (isLoan) "Recuperar" else "Retirar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun CardEditDialog(
    existing: BankCard?,
    accounts: List<WalletAccount>,
    prefill: DetectedCardMetadata? = null,
    onDismiss: () -> Unit,
    onSave: (
        alias: String,
        bankName: String,
        cardKind: CardKind,
        network: CardNetwork,
        last4: String,
        currency: CurrencyCode,
        debtBalance: Double,
        creditLimit: Double,
        linkedAccountId: String,
        autoDebitEnabled: Boolean,
        autoDebitMode: AutoDebitMode,
        autoDebitValue: Double,
        googleWalletLinked: Boolean,
        cardFrozen: Boolean,
        cutoffDay: Int,
        dueDay: Int,
        colorTheme: CardColorTheme
    ) -> Unit
) {
    val scope = rememberCoroutineScope()

    var alias by remember { mutableStateOf(existing?.alias ?: prefill?.suggestedAlias ?: "") }
    var bankName by remember { mutableStateOf(existing?.bankName ?: prefill?.bankName ?: "") }
    var cardKind by remember { mutableStateOf(existing?.cardKindEnum ?: prefill?.cardKind ?: CardKind.CREDIT) }
    var network by remember { mutableStateOf(existing?.networkEnum ?: prefill?.network ?: CardNetwork.VISA) }
    var last4 by remember { mutableStateOf(existing?.last4 ?: prefill?.last4 ?: "") }
    var currency by remember { mutableStateOf(existing?.currencyCode ?: prefill?.suggestedCurrency ?: CurrencyCode.NIO) }
    var debtStr by remember { mutableStateOf(if (existing != null && existing.debtBalance > 0.0) existing.debtBalance.toString() else "0") }
    var limitStr by remember { mutableStateOf(if (existing != null && existing.creditLimit > 0.0) existing.creditLimit.toString() else "") }
    var linkedAccountId by remember { mutableStateOf(existing?.linkedAccountId ?: accounts.firstOrNull()?.id.orEmpty()) }
    var autoDebitEnabled by remember { mutableStateOf(existing?.autoDebitEnabled ?: (cardKind == CardKind.CREDIT)) }
    var autoDebitMode by remember { mutableStateOf(existing?.autoDebitModeEnum ?: AutoDebitMode.FULL_DEBT) }
    var autoDebitValueStr by remember { mutableStateOf(existing?.autoDebitValue?.toString() ?: "100") }
    var googleWalletLinked by remember { mutableStateOf(existing?.googleWalletLinked ?: false) }
    var cutoffDayStr by remember { mutableStateOf((existing?.cutoffDay ?: 15).toString()) }
    var dueDayStr by remember { mutableStateOf((existing?.dueDay ?: 5).toString()) }
    var colorTheme by remember { mutableStateOf(existing?.colorThemeEnum ?: prefill?.suggestedTheme ?: CardColorTheme.DORA_BLUE) }

    var binInput by remember { mutableStateOf("") }
    var detectingBin by remember { mutableStateOf(false) }
    var detectionBanner by remember { mutableStateOf(prefill?.sourceLabel?.takeIf { it.isNotBlank() }) }

    fun applyDetectedMetadata(meta: DetectedCardMetadata) {
        meta.network?.let { network = it }
        meta.cardKind?.let {
            cardKind = it
            if (existing == null && it == CardKind.DEBIT) {
                autoDebitEnabled = false
            }
        }
        meta.bankName?.let { bankName = it }
        meta.last4?.let { last4 = it }
        meta.suggestedCurrency?.let { currency = it }
        meta.suggestedTheme?.let { colorTheme = it }
        if (alias.isBlank() && !meta.suggestedAlias.isNullOrBlank()) {
            alias = meta.suggestedAlias
        }
        detectionBanner = buildList {
            if (meta.sourceLabel.isNotBlank()) add(meta.sourceLabel)
            meta.bankName?.let { add(it) }
            meta.network?.let { add(it.label) }
            meta.cardKind?.let { add(it.label) }
        }.distinct().joinToString(" · ")
    }

    val liveTextHint = remember(alias, bankName) {
        CardDetector.detectFromText("$bankName $alias")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) "Registrar tarjeta en Doracash" else "Editar Tarjeta",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Detección de la tarjeta física
                Surface(
                    color = DoraSkyBlue.copy(alpha = 0.09f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = DoraSkyBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Detectar banco y tipo de tarjeta",
                                style = MaterialTheme.typography.labelLarge,
                                color = DoraSkyBlue
                            )
                        }
                        PhysicalCardScanButton(
                            onDetected = ::applyDetectedMetadata,
                            onUnavailable = { detectionBanner = it }
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = binInput,
                                onValueChange = { raw ->
                                    val clean = raw.filter { it.isDigit() }.take(8)
                                    binInput = clean
                                    CardDetector.detectNetworkFromDigits(clean)?.let { network = it }
                                },
                                label = { Text("Primeros 6-8 dígitos (BIN, no se guardan)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("card_bin_input")
                            )
                            Button(
                                onClick = {
                                    detectingBin = true
                                    scope.launch {
                                        val res = CardDetector.lookupBin(binInput)
                                        applyDetectedMetadata(res)
                                        detectingBin = false
                                    }
                                },
                                enabled = binInput.length >= 6 && !detectingBin,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("detect_bin_button")
                            ) {
                                Text(if (detectingBin) "..." else "Detectar")
                            }
                        }

                        if (!detectionBanner.isNullOrBlank()) {
                            Text(
                                text = detectionBanner!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = DoraMintGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Nombre o alias (ej. BAC Cashback)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_alias_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Banco / Emisor") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("card_bank_input")
                    )
                    OutlinedTextField(
                        value = last4,
                        onValueChange = { input ->
                            last4 = input.filter { it.isDigit() }.take(4)
                        },
                        label = { Text("Últimos 4") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_last4_input")
                    )
                }

                val hasUnappliedBankOrKind = (liveTextHint.bankName != null && !bankName.equals(liveTextHint.bankName, ignoreCase = true)) ||
                    (liveTextHint.cardKind != null && cardKind != liveTextHint.cardKind) ||
                    (liveTextHint.network != null && network != liveTextHint.network)
                if (hasUnappliedBankOrKind) {
                    FilterChip(
                        selected = false,
                        onClick = { applyDetectedMetadata(liveTextHint) },
                        leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = {
                            val parts = listOfNotNull(
                                liveTextHint.bankName,
                                liveTextHint.network?.label,
                                liveTextHint.cardKind?.label
                            ).joinToString(" · ")
                            Text("Aplicar detección: $parts")
                        }
                    )
                }

                Text(
                    text = "Seguridad: solo almacenamos los últimos 4 dígitos (nunca el número completo ni CVV).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Tipo de tarjeta", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CardKind.entries) { kind ->
                        FilterChip(
                            selected = cardKind == kind,
                            onClick = {
                                cardKind = kind
                                if (existing == null && kind == CardKind.DEBIT) {
                                    autoDebitEnabled = false
                                }
                            },
                            label = { Text(kind.label) }
                        )
                    }
                }

                Text("Red y Moneda", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CardNetwork.entries) { net ->
                        FilterChip(
                            selected = network == net,
                            onClick = { network = net },
                            label = { Text(net.label) }
                        )
                    }
                    items(CurrencyCode.entries) { curr ->
                        FilterChip(
                            selected = currency == curr,
                            onClick = { currency = curr },
                            label = { Text(curr.name) }
                        )
                    }
                }

                if (cardKind != CardKind.DEBIT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = debtStr,
                            onValueChange = { debtStr = it },
                            label = { Text("Deuda actual (${currency.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_debt_input")
                        )
                        OutlinedTextField(
                            value = limitStr,
                            onValueChange = { limitStr = it },
                            label = { Text("Límite (${currency.symbol}, opc.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cutoffDayStr,
                            onValueChange = { cutoffDayStr = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Día corte (1-31)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dueDayStr,
                            onValueChange = { dueDayStr = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Día pago (1-31)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (accounts.isNotEmpty()) {
                    Text("Cuenta vinculada", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(accounts, key = { it.id }) { acc ->
                            FilterChip(
                                selected = linkedAccountId == acc.id,
                                onClick = { linkedAccountId = acc.id },
                                label = { Text("${acc.name} (${acc.currency})") }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("También la tengo en Google Wallet", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Etiqueta personal de esta tarjeta",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = googleWalletLinked, onCheckedChange = { googleWalletLinked = it })
                }

                if (cardKind != CardKind.DEBIT) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-debitar de mis ingresos", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "Descuenta tu deuda cuando registres un ingreso",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoDebitEnabled,
                            onCheckedChange = { autoDebitEnabled = it },
                            modifier = Modifier.testTag("card_auto_debit_switch")
                        )
                    }

                    if (autoDebitEnabled) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(AutoDebitMode.entries.filter { it != AutoDebitMode.MANUAL }) { mode ->
                                FilterChip(
                                    selected = autoDebitMode == mode,
                                    onClick = { autoDebitMode = mode },
                                    label = { Text(mode.label) }
                                )
                            }
                        }
                        if (autoDebitMode == AutoDebitMode.PERCENTAGE || autoDebitMode == AutoDebitMode.FIXED_AMOUNT) {
                            OutlinedTextField(
                                value = autoDebitValueStr,
                                onValueChange = { autoDebitValueStr = it },
                                label = {
                                    Text(
                                        if (autoDebitMode == AutoDebitMode.PERCENTAGE) "Porcentaje del ingreso (%)"
                                        else "Cuota fija por ingreso (${currency.symbol})"
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Text("Color de tarjeta", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CardColorTheme.entries) { theme ->
                        FilterChip(
                            selected = colorTheme == theme,
                            onClick = { colorTheme = theme },
                            label = { Text(theme.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val debt = debtStr.toDoubleOrNull() ?: 0.0
            val limit = if (limitStr.isBlank()) 0.0 else (limitStr.toDoubleOrNull() ?: 0.0)
            val autoVal = autoDebitValueStr.toDoubleOrNull() ?: 0.0
            val cutoff = (cutoffDayStr.toIntOrNull() ?: 15).coerceIn(1, 31)
            val due = (dueDayStr.toIntOrNull() ?: 5).coerceIn(1, 31)

            Button(
                onClick = {
                    onSave(
                        alias,
                        bankName,
                        cardKind,
                        network,
                        last4,
                        currency,
                        debt.coerceAtLeast(0.0),
                        limit.coerceAtLeast(0.0),
                        linkedAccountId,
                        autoDebitEnabled && cardKind != CardKind.DEBIT,
                        if (autoDebitEnabled && cardKind != CardKind.DEBIT) autoDebitMode else AutoDebitMode.MANUAL,
                        autoVal.coerceAtLeast(0.0),
                        googleWalletLinked,
                        existing?.cardFrozen ?: false,
                        cutoff,
                        due,
                        colorTheme
                    )
                },
                enabled = alias.isNotBlank() && last4.length == 4,
                modifier = Modifier.testTag("save_card_button")
            ) {
                Text("Guardar Tarjeta")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun MovementStatusPicker(pending: Boolean?, onSelect: (Boolean) -> Unit) {
    Text("Estado del movimiento", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = pending == false, onClick = { onSelect(false) }, label = { Text("Realizado") })
        FilterChip(selected = pending == true, onClick = { onSelect(true) }, label = { Text("Pendiente") })
    }
    if (pending == true) Text("Se aplicará al saldo cuando lo marques realizado.", style = MaterialTheme.typography.labelSmall)
}

@Composable
fun CardChargeDialog(
    card: BankCard,
    onDismiss: () -> Unit,
    onConfirmCharge: (amount: Double, categoryId: String, note: String, dateIso: String, pending: Boolean) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dateIso by remember { mutableStateOf(FinanceUtils.todayIso()) }
    val cats = remember {
        CategoryCatalog.expenseCategories.filter { CategoryCatalog.classificationOf(it.id) == EntryClassification.NORMAL }
    }
    var selectedCatId by remember { mutableStateOf(cats.first().id) }

    var pending by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Boolean?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compra con ${card.alias}", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MovementStatusPicker(pending) { pending = it }
                Text(
                    text = if (card.cardKindEnum == CardKind.DEBIT) {
                        if (card.linkedAccountId.isBlank()) "Vinculá una cuenta para registrar compras." else "La compra se descontará de la cuenta vinculada."
                    } else {
                        "Sumará este gasto a la deuda de tu tarjeta (•••• ${card.last4})."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Monto (${card.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(cats, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCatId == cat.id,
                            onClick = { selectedCatId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Comercio o detalle (ej. Súper, Netflix)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateIso,
                    onValueChange = { dateIso = it },
                    label = { Text("Fecha (AAAA-MM-DD)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amt = amountStr.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onConfirmCharge(amt, selectedCatId, note, dateIso, pending == true) },
                enabled = pending != null && amt.isFinite() && FinanceUtils.round2(amt) > 0.0 && isValidMovementDate(dateIso)
            ) {
                Text(if (pending == true) "Guardar pendiente" else "Registrar compra")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun CardPayDebtDialog(
    card: BankCard,
    accounts: List<WalletAccount>,
    exchangeRate: Double,
    onDismiss: () -> Unit,
    onConfirmPay: (payAccount: WalletAccount, amountInAccountCurrency: Double, pending: Boolean) -> Unit
) {
    if (accounts.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Sin cuentas") },
            text = { Text("Agrega una cuenta con fondos para pagar tu tarjeta.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido") } }
        )
        return
    }
    val defaultAcc = accounts.firstOrNull { it.id == card.linkedAccountId } ?: accounts.first()
    var selectedAcc by remember { mutableStateOf(defaultAcc) }
    val fullInAcc = FinanceUtils.round2(
        FinanceUtils.convertCurrency(card.debtBalance, card.currencyCode, selectedAcc.currencyCode, exchangeRate)
    )
    var amountStr by remember(selectedAcc) { mutableStateOf(fullInAcc.toString()) }

    var pending by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Boolean?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pagar ${card.alias}", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MovementStatusPicker(pending) { pending = it }
                Text(
                    text = "Deuda pendiente: ${FinanceUtils.formatMoney(card.debtBalance, card.currencyCode)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = DoraCollarRed,
                    fontWeight = FontWeight.Bold
                )
                Text("Pagar desde cuenta:", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(accounts, key = { it.id }) { acc ->
                        FilterChip(
                            selected = selectedAcc.id == acc.id,
                            onClick = { selectedAcc = acc },
                            label = { Text("${acc.name} (${acc.currency})") }
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(0.25 to "25%", 0.50 to "50%", 1.0 to "Saldar todo").forEach { (ratio, label) ->
                        val suggested = FinanceUtils.round2(
                            FinanceUtils.convertCurrency(
                                card.debtBalance * ratio,
                                card.currencyCode,
                                selectedAcc.currencyCode,
                                exchangeRate
                            )
                        )
                        FilterChip(
                            selected = (amountStr.toDoubleOrNull() ?: -1.0) == suggested,
                            onClick = { amountStr = suggested.toString() },
                            label = { Text(label, maxLines = 1) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Monto a pagar (${selectedAcc.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amt = amountStr.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onConfirmPay(selectedAcc, amt, pending == true) },
                enabled = pending != null && amt.isFinite() && FinanceUtils.round2(amt) > 0.0 && amt <= fullInAcc
            ) {
                Text(if (pending == true) "Guardar pendiente" else "Confirmar pago")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

internal fun isValidMovementDate(value: String): Boolean {
    if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) return false
    val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).apply { isLenient = false }
    return runCatching { format.parse(value) }.getOrNull() != null
}
