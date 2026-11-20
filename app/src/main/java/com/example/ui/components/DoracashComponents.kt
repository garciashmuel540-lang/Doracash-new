package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CategoryCatalog
import com.example.data.CurrencyCode
import com.example.data.EntryClassification
import com.example.data.FinanceTransaction
import com.example.data.FinanceUtils
import com.example.data.TxType
import com.example.data.WalletAccount
import com.example.ui.theme.DoraBellGold
import com.example.ui.theme.DoraCollarRed
import com.example.ui.theme.DoraDeepBlue
import com.example.ui.theme.DoraDoorPink
import com.example.ui.theme.DoraMintGreen
import com.example.ui.theme.DoraSkyBlue

fun iconForKey(key: String): ImageVector = when (key) {
    "cash" -> Icons.Filled.AccountBalanceWallet
    "bank", "lafise" -> Icons.Filled.AccountBalance
    "cajita" -> Icons.Filled.Inventory2
    "prestamo" -> Icons.Filled.VolunteerActivism
    "savings" -> Icons.Filled.Savings
    "credit" -> Icons.Filled.CreditCard
    "food" -> Icons.Filled.Restaurant
    "bolt" -> Icons.Filled.Bolt
    "bus" -> Icons.Filled.DirectionsBus
    "home" -> Icons.Filled.Home
    "health" -> Icons.Filled.LocalHospital
    "game" -> Icons.Filled.SportsEsports
    "school" -> Icons.Filled.School
    "card" -> Icons.Filled.CreditCard
    "calc" -> Icons.Filled.Calculate
    "work" -> Icons.Filled.Work
    "rocket" -> Icons.Filled.RocketLaunch
    "gift" -> Icons.Filled.CardGiftcard
    "sparkle" -> Icons.Filled.Star
    "shield" -> Icons.Filled.Shield
    "flight" -> Icons.Filled.Flight
    "transfer" -> Icons.Filled.SwapHoriz
    else -> Icons.Filled.Inventory2
}

@Composable
fun Pocket4DHeroCard(
    netWorth: Double,
    monthIncome: Double,
    monthExpense: Double,
    displayCurrency: CurrencyCode,
    exchangeRate: Double,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(DoraSkyBlue, DoraDeepBlue)
                    )
                )
                .padding(20.dp)
        ) {
            // Subtle 4D Pocket background ring
            Canvas(
                modifier = Modifier
                    .matchParentSize()
            ) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.92f, size.height * 0.15f),
                    style = Stroke(width = 18.dp.toPx())
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PATRIMONIO · TU BOLSILLO AL INSTANTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.88f),
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Surface(
                        color = Color.White.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "1 USD = C$ ${"%.2f".format(exchangeRate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = FinanceUtils.formatMoney(netWorth, displayCurrency),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Doraemon Red Collar accent divider with Golden Bell
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(DoraCollarRed)
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(DoraBellGold)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF78350F))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4D White Inner Pocket Card
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 32.dp,
                        bottomEnd = 32.dp
                    ),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ingresos del mes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "+${FinanceUtils.formatMoney(monthIncome, displayCurrency)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = DoraMintGreen,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Gastos del mes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "-${FinanceUtils.formatMoney(monthExpense, displayCurrency)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = DoraCollarRed,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseDonutChart(
    slices: List<Triple<String, Double, Color>>,
    totalExpense: Double,
    displayCurrency: CurrencyCode,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(126.dp)) {
            Canvas(modifier = Modifier.size(116.dp)) {
                val strokeWidth = 18.dp.toPx()
                if (totalExpense <= 0.0 || slices.isEmpty()) {
                    drawArc(
                        color = Color(0xFFCBD5E1).copy(alpha = 0.4f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    var startAngle = -90f
                    slices.forEach { (_, value, color) ->
                        val sweep = ((value / totalExpense) * 360.0).toFloat()
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FinanceUtils.formatMoney(totalExpense, displayCurrency),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (slices.isEmpty() || totalExpense <= 0.0) {
                Text(
                    text = "Aún no registras gastos este mes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                slices.take(4).forEach { (name, value, color) ->
                    val pct = ((value / totalExpense) * 100).toInt()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

data class MonthlyCashflowPoint(val label: String, val income: Double, val expense: Double)

@Composable
fun CashflowBarChart(
    points: List<MonthlyCashflowPoint>,
    modifier: Modifier = Modifier
) {
    val maxVal = (points.flatMap { listOf(it.income, it.expense) }.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            if (points.isEmpty()) return@Canvas
            val groupWidth = size.width / points.size
            val barWidth = (groupWidth * 0.28f).coerceAtMost(16.dp.toPx())
            val gap = 4.dp.toPx()
            points.forEachIndexed { idx, p ->
                val centerX = groupWidth * idx + groupWidth / 2f
                val incHeight = ((p.income / maxVal) * size.height).toFloat().coerceAtLeast(4.dp.toPx())
                val expHeight = ((p.expense / maxVal) * size.height).toFloat().coerceAtLeast(4.dp.toPx())

                drawRoundRect(
                    color = DoraSkyBlue,
                    topLeft = Offset(centerX - barWidth - gap / 2f, size.height - incHeight),
                    size = Size(barWidth, incHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                drawRoundRect(
                    color = DoraDoorPink,
                    topLeft = Offset(centerX + gap / 2f, size.height - expHeight),
                    size = Size(barWidth, expHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ) {
            points.forEach { p ->
                Text(
                    text = p.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TransactionCompactRow(
    tx: FinanceTransaction,
    accounts: List<WalletAccount>,
    modifier: Modifier = Modifier
) {
    val isTransfer = tx.typeEnum == TxType.TRANSFER
    val isIncome = tx.typeEnum == TxType.INCOME
    val cat = if (isTransfer) null else CategoryCatalog.find(isIncome, tx.categoryId)
    val srcAcc = accounts.firstOrNull { it.id == tx.accountId }
    val dstAcc = if (isTransfer) accounts.firstOrNull { it.id == tx.toAccountId } else null

    val icon = when {
        isTransfer -> Icons.Filled.SwapHoriz
        else -> iconForKey(cat?.iconKey ?: "box")
    }
    val tint = when {
        isTransfer -> DoraSkyBlue
        isIncome -> DoraMintGreen
        else -> cat?.color ?: DoraCollarRed
    }
    val amountPrefix = when {
        isTransfer -> ""
        isIncome -> "+"
        else -> "-"
    }
    val amountColor = when {
        isTransfer -> DoraSkyBlue
        isIncome -> DoraMintGreen
        else -> DoraCollarRed
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.16f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = cat?.name ?: "Transferencia",
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.note.ifBlank { cat?.name ?: "Transferencia" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = buildString {
                    append(tx.dateIso)
                    append(" · ")
                    append(srcAcc?.name ?: "Cuenta")
                    if (dstAcc != null) {
                        append(" → ")
                        append(dstAcc.name)
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val classification = CategoryCatalog.classificationOf(tx.categoryId)
                if (!isTransfer && classification != EntryClassification.NORMAL) {
                    val sType = CategoryCatalog.savingsTypeOf(tx.categoryId)
                    val badgeText = when (classification) {
                        EntryClassification.DEBT -> "Deuda"
                        EntryClassification.SAVINGS -> "Ahorro · ${sType?.shortLabel ?: "General"}"
                        EntryClassification.NORMAL -> ""
                    }
                    val badgeColor = if (classification == EntryClassification.DEBT) {
                        DoraCollarRed
                    } else {
                        sType?.color ?: DoraMintGreen
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        color = badgeColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$amountPrefix${FinanceUtils.formatMoney(tx.amount, tx.currencyCode)}",
                style = MaterialTheme.typography.titleMedium,
                color = amountColor,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun BellAlertButton(
    alertCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = DoraBellGold.copy(alpha = 0.22f),
        modifier = modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Alertas de pagos",
                tint = Color(0xFFB45309),
                modifier = Modifier.size(22.dp)
            )
            if (alertCount > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(DoraCollarRed)
                ) {
                    Text(
                        text = alertCount.coerceAtMost(9).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
