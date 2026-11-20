package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Expense
import com.example.model.ExpenseCategory
import com.example.model.UserSettings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.util.DateFormatUtils
import kotlin.math.roundToInt

private data class CategoryBreakdown(
    val category: ExpenseCategory,
    val total: Double,
    val count: Int,
    val percentage: Float,
)

@Composable
fun MonthlySummaryTab(
    expenses: List<Expense>,
    settings: UserSettings,
    selectedMonthKey: String,
    onShiftMonth: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val symbol = settings.currencySymbol.ifBlank { "$" }

    val monthExpenses = remember(expenses, selectedMonthKey) {
        expenses.filter { it.monthKey == selectedMonthKey }
    }
    val monthTotal = remember(monthExpenses) { monthExpenses.sumOf { it.amount } }
    val activeDaysCount = remember(monthExpenses) {
        monthExpenses.map { it.dateKey }.distinct().size.coerceAtLeast(1)
    }
    val dailyAverage = remember(monthTotal, monthExpenses) {
        if (monthExpenses.isEmpty()) 0.0 else monthTotal / activeDaysCount
    }

    val categoryBreakdowns = remember(monthExpenses, monthTotal) {
        if (monthTotal <= 0.0) emptyList()
        else {
            monthExpenses
                .groupBy { it.categoryEnum }
                .map { (cat, list) ->
                    val sum = list.sumOf { it.amount }
                    CategoryBreakdown(
                        category = cat,
                        total = sum,
                        count = list.size,
                        percentage = (sum / monthTotal).toFloat().coerceIn(0f, 1f)
                    )
                }
                .sortedByDescending { it.total }
        }
    }

    val cashMonthExpenses = remember(monthExpenses) { monthExpenses.filter { !it.isCreditPurchase } }
    val creditMonthExpenses = remember(monthExpenses) { monthExpenses.filter { it.isCreditPurchase } }
    val cashMonthTotal = remember(cashMonthExpenses) { cashMonthExpenses.sumOf { it.amount } }
    val creditMonthTotal = remember(creditMonthExpenses) { creditMonthExpenses.sumOf { it.amount } }

    val last7DaysData = remember(expenses) {
        val keys = DateFormatUtils.lastSevenDaysKeys()
        keys.map { key ->
            val sum = expenses.filter { it.dateKey == key }.sumOf { it.amount }
            Triple(key, DateFormatUtils.shortDayLabel(key), sum)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "month_hero") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { onShiftMonth(-1) },
                                modifier = Modifier.testTag("month_prev_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    contentDescription = stringResource(R.string.cd_previous_month)
                                )
                            }

                            Text(
                                text = DateFormatUtils.formatMonthKeyFriendly(selectedMonthKey),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = { onShiftMonth(1) },
                                modifier = Modifier.testTag("month_next_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.cd_next_month)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Total del mes",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = DateFormatUtils.formatCurrency(monthTotal, symbol),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MiniStatPill(
                                label = "Promedio / día activo",
                                value = DateFormatUtils.formatCurrencyCompact(dailyAverage, symbol),
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatPill(
                                label = "Registros",
                                value = "${monthExpenses.size}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item(key = "weekly_chart") {
                SevenDayBarChartCard(
                    days = last7DaysData,
                    dailyBudget = settings.dailyBudget,
                    currencySymbol = symbol
                )
            }

            if (monthExpenses.isNotEmpty()) {
                item(key = "payment_method_breakdown") {
                    PaymentMethodBreakdownCard(
                        cashTotal = cashMonthTotal,
                        cashCount = cashMonthExpenses.size,
                        creditTotal = creditMonthTotal,
                        creditCount = creditMonthExpenses.size,
                        currencySymbol = symbol
                    )
                }
            }

            item(key = "categories_title") {
                Text(
                    text = "Gastos por categoría",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            if (categoryBreakdowns.isEmpty()) {
                item(key = "empty_month") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Insights,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Aún no hay gastos en este mes",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cuando registres gastos en este mes aparecerá tu desglose por categoría.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(categoryBreakdowns, key = { it.category.name }) { item ->
                    CategoryBreakdownRow(item = item, currencySymbol = symbol)
                }
            }
        }
    }
}

@Composable
private fun MiniStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SevenDayBarChartCard(
    days: List<Triple<String, String, Double>>,
    dailyBudget: Double,
    currencySymbol: String,
) {
    val maxSpend = remember(days, dailyBudget) {
        val peak = days.maxOfOrNull { it.third } ?: 0.0
        maxOf(peak, dailyBudget, 10.0)
    }
    val primaryColor = MaterialTheme.colorScheme.primary
    val overColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val budgetLineColor = MaterialTheme.colorScheme.tertiary

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Últimos 7 días",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (dailyBudget > 0.0) {
                    Text(
                        text = "Línea = Meta ${DateFormatUtils.formatCurrencyCompact(dailyBudget, currencySymbol)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val count = days.size.coerceAtLeast(1)
                val slotWidth = size.width / count
                val barWidth = (slotWidth * 0.48f).coerceAtMost(36.dp.toPx())
                val chartHeight = size.height

                days.forEachIndexed { index, (_, _, amount) ->
                    val x = slotWidth * index + (slotWidth - barWidth) / 2f
                    // Subtle background track
                    drawRoundRect(
                        color = trackColor.copy(alpha = 0.45f),
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                    if (amount > 0.0) {
                        val ratio = (amount / maxSpend).toFloat().coerceIn(0.05f, 1f)
                        val barH = chartHeight * ratio
                        val topY = chartHeight - barH
                        val barColor = if (dailyBudget > 0.0 && amount > dailyBudget) overColor else primaryColor
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, topY),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }
                }

                if (dailyBudget > 0.0) {
                    val budgetRatio = (dailyBudget / maxSpend).toFloat().coerceIn(0f, 1f)
                    val lineY = chartHeight * (1f - budgetRatio)
                    drawLine(
                        color = budgetLineColor,
                        start = Offset(0f, lineY),
                        end = Offset(size.width, lineY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { (dateKey, dayChar, amount) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = dayChar,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (dateKey == DateFormatUtils.todayDateKey()) FontWeight.Bold else FontWeight.Normal,
                            color = if (dateKey == DateFormatUtils.todayDateKey()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (amount > 0.0) DateFormatUtils.formatCurrencyCompact(amount, currencySymbol) else "-",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    item: CategoryBreakdown,
    currencySymbol: String,
) {
    val animatedPct by animateFloatAsState(
        targetValue = item.percentage,
        animationSpec = tween(durationMillis = 400),
        label = "category_pct"
    )
    val pctInt = (item.percentage * 100).roundToInt()

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(item.category.accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.category.icon,
                        contentDescription = item.category.label,
                        tint = item.category.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.category.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.count} ${if (item.count == 1) "gasto" else "gastos"} • $pctInt%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = DateFormatUtils.formatCurrency(item.total, currencySymbol),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedPct },
                color = item.category.accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
            )
        }
    }
}

@Composable
private fun PaymentMethodBreakdownCard(
    cashTotal: Double,
    cashCount: Int,
    creditTotal: Double,
    creditCount: Int,
    currencySymbol: String,
) {
    val grandTotal = cashTotal + creditTotal
    val cashRatio = if (grandTotal > 0.0) (cashTotal / grandTotal).toFloat().coerceIn(0f, 1f) else 0f
    val creditRatio = if (grandTotal > 0.0) (creditTotal / grandTotal).toFloat().coerceIn(0f, 1f) else 0f

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_method_breakdown_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Forma de pago en el mes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Efectivo / Débito",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = DateFormatUtils.formatCurrencyCompact(cashTotal, currencySymbol),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$cashCount ${if (cashCount == 1) "gasto" else "gastos"} • ${(cashRatio * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tarjeta / Crédito",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = DateFormatUtils.formatCurrencyCompact(creditTotal, currencySymbol),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "$creditCount ${if (creditCount == 1) "compra" else "compras"} • ${(creditRatio * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (grandTotal > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (cashRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(cashRatio)
                                .height(8.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (creditRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(creditRatio)
                                .height(8.dp)
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                    }
                }
            }
        }
    }
}
