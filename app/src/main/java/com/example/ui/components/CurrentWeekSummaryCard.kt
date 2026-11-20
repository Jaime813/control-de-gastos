package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Expense
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.util.DateFormatUtils

@Composable
fun CurrentWeekSummaryCard(
    expenses: List<Expense>,
    dailyBudget: Double,
    currencySymbol: String,
    selectedDateKey: String,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val todayKey = remember { DateFormatUtils.todayDateKey() }
    val weekKeys = remember(todayKey) { DateFormatUtils.currentWeekOrderedDateKeys(todayKey) }
    val weekRangeLabel = remember(weekKeys) { DateFormatUtils.formatWeekRangeLabel(weekKeys) }

    val weekExpenses = remember(expenses, weekKeys) {
        val keySet = weekKeys.toSet()
        expenses.filter { it.dateKey in keySet }
    }
    val weekTotal = remember(weekExpenses) { weekExpenses.sumOf { it.amount } }
    val dailyTotals = remember(weekExpenses, weekKeys) {
        val grouped = weekExpenses.groupBy { it.dateKey }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        weekKeys.map { key -> key to (grouped[key] ?: 0.0) }
    }

    val weeklyBudget = if (dailyBudget > 0.0) dailyBudget * 7.0 else 0.0
    val isOverWeeklyBudget = weeklyBudget > 0.0 && weekTotal > weeklyBudget
    val rawWeeklyProgress = if (weeklyBudget > 0.0) {
        (weekTotal / weeklyBudget).toFloat().coerceIn(0f, 1f)
    } else 0f
    val animatedWeeklyProgress by animateFloatAsState(
        targetValue = rawWeeklyProgress,
        animationSpec = tween(durationMillis = 380),
        label = "weekly_budget_progress"
    )

    val activeDaysThisWeek = remember(dailyTotals) {
        dailyTotals.count { it.second > 0.0 }.coerceAtLeast(1)
    }
    val dailyAverage = if (weekExpenses.isEmpty()) 0.0 else weekTotal / activeDaysThisWeek
    val maxDayAmount = remember(dailyTotals, dailyBudget) {
        val peak = dailyTotals.maxOfOrNull { it.second } ?: 0.0
        maxOf(peak, dailyBudget, 10.0)
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("current_week_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Semana actual",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$weekRangeLabel • ${weekExpenses.size} ${
                                if (weekExpenses.size == 1) "gasto" else "gastos"
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = DateFormatUtils.formatCurrency(weekTotal, currencySymbol),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isOverWeeklyBudget) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("current_week_total_text")
                    )
                    if (weekExpenses.isNotEmpty()) {
                        Text(
                            text = "Prom. ${DateFormatUtils.formatCurrencyCompact(dailyAverage, currencySymbol)}/día",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(visible = weeklyBudget > 0.0) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    LinearProgressIndicator(
                        progress = { animatedWeeklyProgress },
                        color = if (isOverWeeklyBudget) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val remaining = kotlin.math.abs(weeklyBudget - weekTotal)
                    Text(
                        text = if (isOverWeeklyBudget) {
                            "Superaste la meta semanal (${DateFormatUtils.formatCurrencyCompact(weeklyBudget, currencySymbol)}) por ${
                                DateFormatUtils.formatCurrencyCompact(remaining, currencySymbol)
                            }"
                        } else {
                            "Disponible esta semana: ${DateFormatUtils.formatCurrencyCompact(remaining, currencySymbol)} de ${
                                DateFormatUtils.formatCurrencyCompact(weeklyBudget, currencySymbol)
                            }"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverWeeklyBudget) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary
            val inactiveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                val count = dailyTotals.size.coerceAtLeast(1)
                val slotWidth = size.width / count
                val barWidth = (slotWidth * 0.42f).coerceAtMost(24.dp.toPx())
                val chartHeight = size.height

                dailyTotals.forEachIndexed { index, (dateKey, amount) ->
                    val x = slotWidth * index + (slotWidth - barWidth) / 2f
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    if (amount > 0.0) {
                        val ratio = (amount / maxDayAmount).toFloat().coerceIn(0.12f, 1f)
                        val barH = chartHeight * ratio
                        val topY = chartHeight - barH
                        val barColor = when {
                            dailyBudget > 0.0 && amount > dailyBudget -> secondaryColor
                            dateKey == todayKey || dateKey == selectedDateKey -> primaryColor
                            else -> inactiveColor
                        }
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, topY),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dailyTotals.forEach { (dateKey, amount) ->
                    val isToday = dateKey == todayKey
                    val isSelected = dateKey == selectedDateKey
                    val dayLetter = DateFormatUtils.shortDayLabel(dateKey)
                    val dayNum = DateFormatUtils.dayOfMonthNumber(dateKey)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                else androidx.compose.ui.graphics.Color.Transparent
                            )
                            .clickable { onSelectDate(dateKey) }
                            .minimumInteractiveComponentSize()
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Text(
                            text = "$dayLetter $dayNum",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isToday -> MaterialTheme.colorScheme.primary
                                isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1
                        )
                        Text(
                            text = if (amount > 0.0) {
                                DateFormatUtils.formatCurrencyCompact(amount, currencySymbol)
                            } else {
                                "—"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            color = if (amount > 0.0) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
