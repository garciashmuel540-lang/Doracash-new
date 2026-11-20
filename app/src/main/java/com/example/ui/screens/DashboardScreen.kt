@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AutoDebitMode
import com.example.data.BankCard
import com.example.data.BillOccurrence
import com.example.data.BillStatus
import com.example.data.CardKind
import com.example.data.CategoryCatalog
import com.example.data.CurrencyCode
import com.example.data.DoracashSnapshot
import com.example.data.FinanceTransaction
import com.example.data.FinanceUtils
import com.example.data.SavingsType
import com.example.data.TxType
import com.example.ui.components.CashflowBarChart
import com.example.ui.components.ExpenseDonutChart
import com.example.ui.components.MonthlyCashflowPoint
import com.example.ui.components.Pocket4DHeroCard
import com.example.ui.components.TransactionCompactRow
import com.example.ui.components.iconForKey
import com.example.ui.theme.DoraBellGold
import com.example.ui.theme.DoraCollarRed
import com.example.ui.theme.DoraDoorPink
import com.example.ui.theme.DoraMintGreen
import com.example.ui.theme.DoraSkyBlue
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardScreen(
    snapshot: DoracashSnapshot,
    onEdit: (FinanceTransaction, String, String, Double?) -> Unit,
    onEditPending: (FinanceTransaction, String, String, Double?) -> Unit,
    onDeletePending: (String) -> Unit,
    onSettle: (String) -> Unit,
    onOpenTxModal: (TxType) -> Unit,
    onNavigateTab: (String) -> Unit,
    onPayBill: (BillOccurrence) -> Unit,
    onLoadDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingSettled by remember { mutableStateOf<FinanceTransaction?>(null) }
    var editingPending by remember { mutableStateOf<FinanceTransaction?>(null) }

    editingSettled?.let { tx ->
        MovementEditDialog(
            tx = tx,
            isPending = false,
            onDismiss = { editingSettled = null },
            onSave = { note, date, amount ->
                onEdit(tx, note, date, amount)
                editingSettled = null
            }
        )
    }

    editingPending?.let { tx ->
        MovementEditDialog(
            tx = tx,
            isPending = true,
            onDismiss = { editingPending = null },
            onSave = { note, date, amount ->
                onEditPending(tx, note, date, amount)
                editingPending = null
            }
        )
    }
    val disp = CurrencyCode.safeValueOf(snapshot.profile.displayCurrency)
    val rate = snapshot.profile.exchangeRate
    val currentYm = remember { FinanceUtils.currentYearMonth() }

    val netWorth = remember(snapshot.accounts, disp, rate) {
        snapshot.accounts.sumOf {
            FinanceUtils.convertCurrency(it.balance, it.currencyCode, disp, rate)
        }
    }

    val monthTxs = remember(snapshot.transactions, currentYm) {
        snapshot.transactions.filter { it.dateIso.startsWith(currentYm) }
    }
    val monthIncome = remember(monthTxs, disp, rate) {
        monthTxs.filter { it.typeEnum == TxType.INCOME }.sumOf {
            FinanceUtils.convertCurrency(it.amount, it.currencyCode, disp, rate)
        }
    }
    val monthExpense = remember(monthTxs, disp, rate) {
        monthTxs.filter { it.typeEnum == TxType.EXPENSE }.sumOf {
            FinanceUtils.convertCurrency(it.amount, it.currencyCode, disp, rate)
        }
    }
    val savingsRate = if (monthIncome > 0.0) {
        (((monthIncome - monthExpense) / monthIncome) * 100).toInt().coerceIn(0, 100)
    } else 0

    val donutSlices = remember(monthTxs, disp, rate) {
        monthTxs.filter { it.typeEnum == TxType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (catId, list) ->
                val cat = CategoryCatalog.find(false, catId)
                val sum = list.sumOf { FinanceUtils.convertCurrency(it.amount, it.currencyCode, disp, rate) }
                Triple(cat.name, sum, cat.color)
            }
            .sortedByDescending { it.second }
    }

    val cashflowPoints = remember(snapshot.transactions, disp, rate) {
        val shortMonths = DateFormatSymbols(Locale.forLanguageTag("es-ES")).shortMonths
        val now = Calendar.getInstance()
        (5 downTo 0).map { offset ->
            val cal = (now.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
            val ym = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            val label = shortMonths[cal.get(Calendar.MONTH)].take(3)
            val txs = snapshot.transactions.filter { it.dateIso.startsWith(ym) }
            val inc = txs.filter { it.typeEnum == TxType.INCOME }
                .sumOf { FinanceUtils.convertCurrency(it.amount, it.currencyCode, disp, rate) }
            val exp = txs.filter { it.typeEnum == TxType.EXPENSE }
                .sumOf { FinanceUtils.convertCurrency(it.amount, it.currencyCode, disp, rate) }
            MonthlyCashflowPoint(label, inc, exp)
        }
    }

    val upcomingBills = remember(snapshot.scheduledBills) {
        val now = Calendar.getInstance()
        FinanceUtils.occurrencesForMonth(
            snapshot.scheduledBills,
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH)
        ).filter { it.status != BillStatus.PAID }.take(4)
    }

    val showEmptyStarterBanner = snapshot.transactions.isEmpty() && snapshot.scheduledBills.isEmpty() && snapshot.pending.isEmpty()

    val totalSavingsDisp = remember(snapshot.goals, disp, rate) {
        snapshot.goals.sumOf { FinanceUtils.convertCurrency(it.currentNio, CurrencyCode.NIO, disp, rate) }
    }
    val savingsBreakdown = remember(snapshot.goals, disp, rate) {
        listOf(SavingsType.CAJITA, SavingsType.LAFISE, SavingsType.PRESTAMO).map { st ->
            val sum = snapshot.goals.filter { it.savingsType == st }
                .sumOf { FinanceUtils.convertCurrency(it.currentNio, CurrencyCode.NIO, disp, rate) }
            st to sum
        }
    }

    val billingCards = remember(snapshot.cards) { snapshot.cards.filter { it.cardKindEnum != CardKind.DEBIT } }
    val totalCardDebtDisp = remember(snapshot.cards, disp, rate) {
        billingCards.sumOf { FinanceUtils.convertCurrency(it.debtBalance, it.currencyCode, disp, rate) }
    }
    val autoDebitCardCount = remember(snapshot.cards) {
        billingCards.count { it.autoDebitEnabled && !it.cardFrozen && it.autoDebitModeEnum != AutoDebitMode.MANUAL }
    }

    val paydayText = remember(snapshot.transactions.size) { FinanceUtils.paydayBannerText() }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payday_countdown_text")
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = DoraSkyBlue,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = paydayText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (showEmptyStarterBanner) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¡Tu bolsillo está listo!",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Registra tus movimientos o carga datos de ejemplo para explorar gráficos, arqueo y calendario.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = onLoadDemo,
                            colors = ButtonDefaults.buttonColors(containerColor = DoraBellGold, contentColor = Color(0xFF451A03)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("load_demo_button")
                        ) {
                            Text("Ver Demo", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        item {
            Pocket4DHeroCard(
                netWorth = netWorth,
                monthIncome = monthIncome,
                monthExpense = monthExpense,
                displayCurrency = disp,
                exchangeRate = rate
            )
        }

        // Quick Gadgets Bento Row
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gadgets Financieros",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Surface(
                            color = DoraMintGreen.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Ahorro $savingsRate%",
                                style = MaterialTheme.typography.labelSmall,
                                color = DoraMintGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GadgetActionTile(
                            icon = Icons.Filled.RemoveCircle,
                            label = "Gasto",
                            tint = DoraCollarRed,
                            onClick = { onOpenTxModal(TxType.EXPENSE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_expense_button")
                        )
                        GadgetActionTile(
                            icon = Icons.Filled.AddCircle,
                            label = "Ingreso",
                            tint = DoraMintGreen,
                            onClick = { onOpenTxModal(TxType.INCOME) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_income_button")
                        )
                        GadgetActionTile(
                            icon = Icons.Filled.Calculate,
                            label = "Arqueo",
                            tint = DoraSkyBlue,
                            onClick = { onNavigateTab("reconcile") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_reconcile_button")
                        )
                        GadgetActionTile(
                            icon = Icons.Filled.SwapHoriz,
                            label = "Transferir",
                            tint = DoraDoorPink,
                            onClick = { onOpenTxModal(TxType.TRANSFER) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_transfer_button")
                        )
                    }
                }
            }
        }

        // Pending Movements awaiting confirmation ("Pendiente" -> "Realizado")
        if (snapshot.pending.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Movimientos pendientes", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "Por confirmar antes de pasar a Últimos Movimientos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = DoraBellGold.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "${snapshot.pending.size} pend.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        snapshot.pending.forEach { tx ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pending_item_${tx.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TransactionCompactRow(
                                        tx = tx,
                                        accounts = snapshot.accounts,
                                        modifier = Modifier.clickable { editingPending = tx }
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = { onSettle(tx.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = DoraMintGreen,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("settle_pending_${tx.id}")
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Marcar realizado", maxLines = 1)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { editingPending = tx },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("edit_pending_${tx.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = "Editar movimiento pendiente",
                                                    tint = DoraSkyBlue
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeletePending(tx.id) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("delete_pending_${tx.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DeleteOutline,
                                                    contentDescription = "Eliminar movimiento pendiente",
                                                    tint = DoraCollarRed
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Accounts Horizontal Strip
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Mis Cuentas", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { onNavigateTab("settings") }) {
                        Text("Gestionar")
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(snapshot.accounts, key = { it.id }) { acc ->
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .width(172.dp)
                                .clickable { onNavigateTab("settings") }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = iconForKey(acc.iconKey),
                                        contentDescription = acc.name,
                                        tint = DoraSkyBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = acc.currency,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = acc.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = FinanceUtils.formatMoney(acc.balance, acc.currencyCode),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (acc.balance < 0) DoraCollarRed else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Mis Ahorros Summary Card
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTab("goals") }
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Mis Ahorros", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = FinanceUtils.formatMoney(totalSavingsDisp, disp),
                                style = MaterialTheme.typography.headlineMedium,
                                color = DoraSkyBlue,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        TextButton(onClick = { onNavigateTab("goals") }) {
                            Text("Ver ahorros")
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        savingsBreakdown.forEach { (st, amt) ->
                            Surface(
                                color = st.color.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = st.shortLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = st.color,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = FinanceUtils.formatMoney(amt, disp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tarjetas y Wallet Summary Card
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTab("cards") }
                    .testTag("dashboard_cards_summary")
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Filled.CreditCard,
                                contentDescription = null,
                                tint = DoraSkyBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Tarjetas y Wallet", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = if (snapshot.cards.isEmpty()) {
                                        "Vincula tus tarjetas, deuda y Google Wallet"
                                    } else {
                                        "${snapshot.cards.size} tarjeta(s) · $autoDebitCardCount auto-débito"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = { onNavigateTab("cards") }) {
                            Text("Abrir Wallet")
                        }
                    }

                    if (snapshot.cards.isNotEmpty()) {
                        if (billingCards.isNotEmpty()) Text(
                            text = "Deuda total: ${FinanceUtils.formatMoney(totalCardDebtDisp, disp)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (totalCardDebtDisp > 0.0) DoraCollarRed else DoraMintGreen,
                            fontWeight = FontWeight.ExtraBold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(snapshot.cards, key = { it.id }) { card ->
                                Surface(
                                    color = card.colorThemeEnum.startColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(
                                            text = "${card.alias} ••••${card.last4}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = card.colorThemeEnum.startColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (card.cardKindEnum == CardKind.DEBIT) "Débito" else FinanceUtils.formatMoney(card.debtBalance, card.currencyCode),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Donut Chart Card
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(text = "Gastos por Categoría (Mes)", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(14.dp))
                    ExpenseDonutChart(
                        slices = donutSlices,
                        totalExpense = monthExpense,
                        displayCurrency = disp
                    )
                }
            }
        }

        // Cashflow 6M Bar Chart Card
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Flujo de 6 Meses",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            LegendDot(color = DoraSkyBlue, label = "Ingresos")
                            LegendDot(color = DoraDoorPink, label = "Gastos")
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    CashflowBarChart(points = cashflowPoints)
                }
            }
        }

        // Upcoming Bills Card
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = DoraBellGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Próximos Pagos", style = MaterialTheme.typography.titleLarge)
                        }
                        TextButton(onClick = { onNavigateTab("calendar") }) {
                            Text("Ver todos")
                        }
                    }

                    if (upcomingBills.isEmpty()) {
                        Text(
                            text = "¡Todo al día! No tienes pagos pendientes este mes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        upcomingBills.forEach { occ ->
                            val statusColor = when (occ.status) {
                                BillStatus.OVERDUE -> DoraCollarRed
                                BillStatus.SOON -> Color(0xFFD97706)
                                BillStatus.PAID -> DoraMintGreen
                                BillStatus.UPCOMING -> DoraSkyBlue
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = occ.bill.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${occ.dateIso} · ${occ.status.label}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = FinanceUtils.formatMoney(occ.bill.amount, occ.bill.currencyCode),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { onPayBill(occ) },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Pagar", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Transactions (editable)
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Últimos Movimientos", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { onNavigateTab("transactions") }) {
                            Text("Ver historial")
                        }
                    }
                    if (snapshot.transactions.isEmpty()) {
                        Text(
                            text = "Aún no hay movimientos registrados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        snapshot.transactions.take(5).forEach { tx ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TransactionCompactRow(
                                    tx = tx,
                                    accounts = snapshot.accounts,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(onClickLabel = "Editar movimiento") { editingSettled = tx }
                                )
                                IconButton(
                                    onClick = { editingSettled = tx },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("edit_recent_tx_${tx.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Editar movimiento",
                                        tint = DoraSkyBlue
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

@Composable
internal fun MovementEditDialog(
    tx: FinanceTransaction,
    isPending: Boolean,
    onDismiss: () -> Unit,
    onSave: (note: String, dateIso: String, newAmount: Double?) -> Unit
) {
    var note by remember(tx.id) { mutableStateOf(tx.note) }
    var date by remember(tx.id) { mutableStateOf(tx.dateIso) }
    var amountStr by remember(tx.id) { mutableStateOf(tx.amount.toString()) }
    val canEditAmount = isPending || tx.typeEnum != TxType.TRANSFER
    val parsedAmount = amountStr.toDoubleOrNull()
    val isAmountValid = !canEditAmount || (parsedAmount != null && parsedAmount > 0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isPending) "Editar pendiente" else "Editar movimiento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(160) },
                    label = { Text("Descripción") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Monto (${tx.currency})") },
                    enabled = canEditAmount,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!canEditAmount) {
                    Text(
                        text = "Las transferencias completadas conservan el monto original para proteger ambos saldos.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Fecha (AAAA-MM-DD)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = isValidMovementDate(date) && isAmountValid,
                onClick = { onSave(note, date, if (canEditAmount) parsedAmount else null) }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun GadgetActionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = tint.copy(alpha = 0.11f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.height(78.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
