package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.BillOccurrence
import com.example.data.BillStatus
import com.example.data.DoracashSnapshot
import com.example.data.FinanceUtils
import com.example.ui.theme.DoraBellGold
import com.example.ui.theme.DoraCollarRed
import com.example.ui.theme.DoraMintGreen
import com.example.ui.theme.DoraSkyBlue
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
    snapshot: DoracashSnapshot,
    onPayBill: (BillOccurrence) -> Unit,
    onDeleteBill: (String) -> Unit,
    onOpenNewBillModal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = remember { Calendar.getInstance() }
    var month by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }
    var year by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var selectedDateIso by remember { mutableStateOf(FinanceUtils.todayIso()) }

    val monthName = remember(month) {
        val esLocale = Locale.forLanguageTag("es-ES")
        DateFormatSymbols(esLocale).months[month]
            .replaceFirstChar { it.titlecase(esLocale) }
    }

    val occurrences = remember(snapshot.scheduledBills, year, month) {
        FinanceUtils.occurrencesForMonth(snapshot.scheduledBills, year, month)
    }

    val firstCal = remember(year, month) {
        Calendar.getInstance().apply { set(year, month, 1, 0, 0, 0) }
    }
    val daysInMonth = firstCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startOffset = remember(year, month) {
        val dow = firstCal.get(Calendar.DAY_OF_WEEK) // Sun=1, Mon=2..Sat=7
        if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
    }
    val ym = remember(year, month) { String.format(Locale.US, "%04d-%02d", year, month + 1) }
    val todayIso = remember { FinanceUtils.todayIso() }

    val selectedOccs = remember(occurrences, selectedDateIso) {
        occurrences.filter { it.dateIso == selectedDateIso }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Month Calendar Grid Card
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (month == 0) { month = 11; year-- } else month--
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior")
                            }
                            Text(
                                text = "$monthName $year",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (month == 11) { month = 0; year++ } else month++
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente")
                            }
                        }

                        Button(
                            onClick = { onOpenNewBillModal(selectedDateIso) },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("add_bill_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Programar", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("L", "M", "X", "J", "V", "S", "D").forEach { d ->
                            Text(
                                text = d,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val totalCells = startOffset + daysInMonth
                    val rows = (totalCells + 6) / 7
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (r in 0 until rows) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                for (c in 0 until 7) {
                                    val cellIdx = r * 7 + c
                                    val dayNum = cellIdx - startOffset + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val iso = String.format(Locale.US, "%s-%02d", ym, dayNum)
                                        val dayOccs = occurrences.filter { it.dateIso == iso }
                                        val isSelected = iso == selectedDateIso
                                        val isToday = iso == todayIso

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                                )
                                                .then(
                                                    if (isSelected) Modifier.border(1.5.dp, DoraSkyBlue, RoundedCornerShape(14.dp))
                                                    else Modifier
                                                )
                                                .clickable { selectedDateIso = iso }
                                                .padding(4.dp)
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isToday) DoraSkyBlue else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                modifier = Modifier.align(Alignment.TopCenter)
                                            )
                                            if (dayOccs.isNotEmpty()) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    modifier = Modifier.align(Alignment.BottomCenter)
                                                ) {
                                                    dayOccs.take(3).forEach { o ->
                                                        val dotClr = when (o.status) {
                                                            BillStatus.PAID -> DoraMintGreen
                                                            BillStatus.OVERDUE -> DoraCollarRed
                                                            BillStatus.SOON -> DoraBellGold
                                                            BillStatus.UPCOMING -> DoraSkyBlue
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .clip(CircleShape)
                                                                .background(dotClr)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatusLegend(DoraMintGreen, "Pagado")
                        StatusLegend(DoraBellGold, "Pronto")
                        StatusLegend(DoraCollarRed, "Vencido")
                        StatusLegend(DoraSkyBlue, "Programado")
                    }
                }
            }
        }

        // Selected Date Payments
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Pagos del $selectedDateIso", style = MaterialTheme.typography.titleLarge)
                    if (selectedOccs.isEmpty()) {
                        Text(
                            text = "No hay pagos programados para este día.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        selectedOccs.forEach { occ ->
                            BillOccurrenceCard(occ = occ, snapshot = snapshot, onPayBill = onPayBill, onDeleteBill = onDeleteBill)
                        }
                    }
                }
            }
        }

        // All Bills in Month
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Agenda de $monthName (${occurrences.size})", style = MaterialTheme.typography.titleLarge)
                    if (occurrences.isEmpty()) {
                        Text(
                            text = "Sin fechas de pago este mes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        occurrences.forEach { occ ->
                            BillOccurrenceCard(occ = occ, snapshot = snapshot, onPayBill = onPayBill, onDeleteBill = onDeleteBill)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun BillOccurrenceCard(
    occ: BillOccurrence,
    snapshot: DoracashSnapshot,
    onPayBill: (BillOccurrence) -> Unit,
    onDeleteBill: (String) -> Unit
) {
    val accName = snapshot.accounts.firstOrNull { it.id == occ.bill.accountId }?.name ?: "Cuenta"
    val classification = com.example.data.CategoryCatalog.classificationOf(occ.bill.categoryId)
    val savingsType = com.example.data.CategoryCatalog.savingsTypeOf(occ.bill.categoryId)
    val statusColor = when (occ.status) {
        BillStatus.PAID -> DoraMintGreen
        BillStatus.OVERDUE -> DoraCollarRed
        BillStatus.SOON -> Color(0xFFD97706)
        BillStatus.UPCOMING -> DoraSkyBlue
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(text = occ.bill.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${occ.dateIso} · $accName · ${occ.bill.frequencyEnum.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (classification != com.example.data.EntryClassification.NORMAL) {
                        val badgeLabel = when (classification) {
                            com.example.data.EntryClassification.DEBT -> "Deuda"
                            com.example.data.EntryClassification.SAVINGS -> "Ahorro · ${savingsType?.shortLabel ?: "General"}"
                            com.example.data.EntryClassification.NORMAL -> ""
                        }
                        val badgeColor = if (classification == com.example.data.EntryClassification.DEBT) {
                            DoraCollarRed
                        } else {
                            savingsType?.color ?: DoraMintGreen
                        }
                        Surface(
                            color = badgeColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = occ.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = FinanceUtils.formatMoney(occ.bill.amount, occ.bill.currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (occ.status != BillStatus.PAID) {
                        Button(
                            onClick = { onPayBill(occ) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("Pagar ahora", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    IconButton(onClick = { onDeleteBill(occ.bill.id) }) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Eliminar pago programado",
                            tint = DoraCollarRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
