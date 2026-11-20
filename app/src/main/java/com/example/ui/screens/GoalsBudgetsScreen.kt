package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CategoryBudget
import com.example.data.CategoryCatalog
import com.example.data.CurrencyCode
import com.example.data.DoracashSnapshot
import com.example.data.FinanceUtils
import com.example.data.SavingsGoal
import com.example.data.SavingsType
import com.example.data.TxType
import com.example.ui.components.iconForKey
import com.example.ui.theme.DoraBellGold
import com.example.ui.theme.DoraCollarRed
import com.example.ui.theme.DoraDorayakiAmber
import com.example.ui.theme.DoraMintGreen
import com.example.ui.theme.DoraSkyBlue

@Composable
fun GoalsBudgetsScreen(
    snapshot: DoracashSnapshot,
    onOpenNewBudget: () -> Unit,
    onDeleteBudget: (CategoryBudget) -> Unit,
    onOpenNewGoal: () -> Unit,
    onOpenDepositGoal: (SavingsGoal) -> Unit,
    onOpenRecoverGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit,
    modifier: Modifier = Modifier
) {
    val disp = CurrencyCode.safeValueOf(snapshot.profile.displayCurrency)
    val rate = snapshot.profile.exchangeRate
    val ym = remember { FinanceUtils.currentYearMonth() }
    var selectedSavingsFilter by remember { mutableStateOf<SavingsType?>(null) }

    val monthExpenses = remember(snapshot.transactions, ym) {
        snapshot.transactions.filter { it.typeEnum == TxType.EXPENSE && it.dateIso.startsWith(ym) }
    }

    val totalSavedDisp = remember(snapshot.goals, disp, rate) {
        snapshot.goals.sumOf { FinanceUtils.convertCurrency(it.currentNio, CurrencyCode.NIO, disp, rate) }
    }
    val totalsByType = remember(snapshot.goals, disp, rate) {
        SavingsType.entries.associateWith { t ->
            snapshot.goals.filter { it.savingsType == t }
                .sumOf { FinanceUtils.convertCurrency(it.currentNio, CurrencyCode.NIO, disp, rate) }
        }
    }
    val filteredGoals = remember(snapshot.goals, selectedSavingsFilter) {
        if (selectedSavingsFilter == null) snapshot.goals
        else snapshot.goals.filter { it.savingsType == selectedSavingsFilter }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Mis Ahorros (Cajita, LAFISE, Prestado y Reservas)
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Mis ahorros", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Cajita, LAFISE, préstamos otorgados y reservas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onOpenNewGoal,
                            colors = ButtonDefaults.buttonColors(containerColor = DoraDorayakiAmber, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("add_goal_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ahorro", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }
                    }

                    // Total Summary Box
                    Surface(
                        color = DoraSkyBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Total en Ahorros",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FinanceUtils.formatMoney(totalSavedDisp, disp),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = DoraSkyBlue,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(SavingsType.entries) { st ->
                                    val amt = totalsByType[st] ?: 0.0
                                    Surface(
                                        color = st.color.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = iconForKey(st.key),
                                                contentDescription = null,
                                                tint = st.color,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${st.shortLabel}: ${FinanceUtils.formatMoney(amt, disp)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = st.color,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Filter Chips by SavingsType
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedSavingsFilter == null,
                                onClick = { selectedSavingsFilter = null },
                                label = { Text("Todos (${snapshot.goals.size})") },
                                modifier = Modifier.testTag("savings_filter_all")
                            )
                        }
                        items(SavingsType.entries) { st ->
                            FilterChip(
                                selected = selectedSavingsFilter == st,
                                onClick = {
                                    selectedSavingsFilter = if (selectedSavingsFilter == st) null else st
                                },
                                label = { Text(st.shortLabel) },
                                modifier = Modifier.testTag("savings_filter_${st.key}")
                            )
                        }
                    }

                    if (filteredGoals.isEmpty()) {
                        Text(
                            text = if (snapshot.goals.isEmpty()) {
                                "Crea tu primer ahorro (Cajita, LAFISE o por Préstamo) o clasifica un ingreso, gasto o pago como Parte del ahorro."
                            } else {
                                "No hay ahorros en la categoría ${selectedSavingsFilter?.shortLabel ?: ""}."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        filteredGoals.forEach { goal ->
                            val st = goal.savingsType
                            val curDisp = FinanceUtils.convertCurrency(goal.currentNio, CurrencyCode.NIO, disp, rate)
                            val targetDisp = FinanceUtils.convertCurrency(goal.targetNio, CurrencyCode.NIO, disp, rate)
                            val ratio = if (goal.hasExplicitTarget && goal.targetNio > 0.0) {
                                (goal.currentNio / goal.targetNio).toFloat().coerceIn(0f, 1f)
                            } else 0f
                            val pct = (ratio * 100).toInt()

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = iconForKey(goal.iconKey),
                                                contentDescription = goal.name,
                                                tint = st.color,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Text(
                                                    text = goal.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Surface(
                                                        color = st.color.copy(alpha = 0.16f),
                                                        shape = RoundedCornerShape(50)
                                                    ) {
                                                        Text(
                                                            text = st.label,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = st.color,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    if (goal.deadlineIso.isNotBlank()) {
                                                        Text(
                                                            text = goal.deadlineIso,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        IconButton(onClick = { onDeleteGoal(goal) }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar ahorro", tint = DoraCollarRed)
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val amountSuffix = if (st == SavingsType.PRESTAMO) "prestados" else "ahorrados"
                                        Text(
                                            text = "${FinanceUtils.formatMoney(curDisp, disp)} $amountSuffix",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = st.color,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (goal.hasExplicitTarget) {
                                            Text(
                                                text = "$pct% de ${FinanceUtils.formatMoney(targetDisp, disp)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (goal.hasExplicitTarget) {
                                        LinearProgressIndicator(
                                            progress = { ratio },
                                            color = st.color,
                                            trackColor = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(50))
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = { onOpenDepositGoal(goal) },
                                            colors = ButtonDefaults.buttonColors(containerColor = st.color),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("deposit_goal_${goal.id}")
                                        ) {
                                            Text(
                                                text = if (st == SavingsType.PRESTAMO) "+ Prestar" else "+ Abonar",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (goal.currentNio > 0.0) {
                                            OutlinedButton(
                                                onClick = { onOpenRecoverGoal(goal) },
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 48.dp)
                                                    .testTag("recover_goal_${goal.id}")
                                            ) {
                                                Text(
                                                    text = if (st == SavingsType.PRESTAMO) "Recuperar" else "Retirar",
                                                    style = MaterialTheme.typography.labelLarge,
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
                }
            }
        }

        // Monthly Budgets
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Presupuestos del Mes", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Controla tus límites por categoría",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onOpenNewBudget,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("add_budget_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Límite", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    if (snapshot.budgets.isEmpty()) {
                        Text(
                            text = "No tienes límites configurados este mes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        snapshot.budgets.forEach { budget ->
                            val cat = CategoryCatalog.find(false, budget.categoryId)
                            val spent = monthExpenses.filter { CategoryCatalog.baseCategoryId(it.categoryId) == budget.categoryId }
                                .sumOf { FinanceUtils.convertCurrency(it.amount, it.currencyCode, disp, rate) }
                            val limit = FinanceUtils.convertCurrency(budget.limitNio, CurrencyCode.NIO, disp, rate)
                            val ratio = if (limit > 0.0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
                            val barColor = when {
                                spent > limit -> DoraCollarRed
                                ratio >= 0.8f -> DoraBellGold
                                else -> DoraMintGreen
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = iconForKey(cat.iconKey),
                                                contentDescription = cat.name,
                                                tint = cat.color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = cat.name, style = MaterialTheme.typography.titleMedium)
                                        }
                                        IconButton(onClick = { onDeleteBudget(budget) }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar presupuesto", tint = DoraCollarRed)
                                        }
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Gastado: ${FinanceUtils.formatMoney(spent, disp)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (spent > limit) DoraCollarRed else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Límite: ${FinanceUtils.formatMoney(limit, disp)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        color = barColor,
                                        trackColor = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(50))
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
