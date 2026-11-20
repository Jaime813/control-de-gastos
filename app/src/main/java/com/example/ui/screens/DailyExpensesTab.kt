package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CategoryConfig
import com.example.model.CategoryResolver
import com.example.model.Expense
import com.example.model.UserSettings
import com.example.ui.components.CurrentWeekSummaryCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.util.DateFormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyExpensesTab(
    expenses: List<Expense>,
    categories: List<CategoryConfig>,
    settings: UserSettings,
    selectedDateKey: String,
    selectedCategoryFilter: String?,
    onShiftDay: (Int) -> Unit,
    onSelectDate: (String) -> Unit,
    onSelectCategoryFilter: (String?) -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onOpenBudgetSettings: () -> Unit,
    onOpenCategorySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val dayExpenses = remember(expenses, selectedDateKey) {
        expenses.filter { it.dateKey == selectedDateKey }
    }
    val filterCategories = remember(categories, dayExpenses) {
        val usedIds = dayExpenses.map { it.categoryId }.toSet()
        val known = categories.filter { it.isEnabled || it.id in usedIds }
        val knownIds = known.map { it.id }.toSet()
        known + (usedIds - knownIds).map { CategoryResolver.resolve(it, categories) }
    }
    val filteredDayExpenses = remember(dayExpenses, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) dayExpenses
        else dayExpenses.filter { it.categoryId == selectedCategoryFilter }
    }

    val dayTotal = remember(dayExpenses) { dayExpenses.sumOf { it.amount } }
    val weekKeys = remember(selectedDateKey) { DateFormatUtils.currentWeekDateKeys(selectedDateKey) }
    val weekTotal = remember(expenses, weekKeys) {
        expenses.filter { it.dateKey in weekKeys }.sumOf { it.amount }
    }
    val selectedMonthKey = remember(selectedDateKey) { selectedDateKey.take(7) }
    val monthTotal = remember(expenses, selectedMonthKey) {
        expenses.filter { it.monthKey == selectedMonthKey }.sumOf { it.amount }
    }

    val symbol = settings.currencySymbol.ifBlank { "$" }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "hero_card") {
                DailyHeroControlCard(
                    selectedDateKey = selectedDateKey,
                    dayTotal = dayTotal,
                    weekTotal = weekTotal,
                    monthTotal = monthTotal,
                    dailyBudget = settings.dailyBudget,
                    currencySymbol = symbol,
                    expenseCount = dayExpenses.size,
                    onShiftDay = onShiftDay,
                    onOpenDatePicker = { showDatePicker = true },
                    onOpenBudgetSettings = onOpenBudgetSettings,
                )
            }

            item(key = "current_week_card") {
                CurrentWeekSummaryCard(
                    expenses = expenses,
                    dailyBudget = settings.dailyBudget,
                    currencySymbol = symbol,
                    selectedDateKey = selectedDateKey,
                    onSelectDate = onSelectDate
                )
            }

            item(key = "category_filters") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item(key = "filter_all") {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { onSelectCategoryFilter(null) },
                            label = { Text("Todos (${dayExpenses.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    items(filterCategories, key = { it.id }) { category ->
                        val count = dayExpenses.count { it.categoryId == category.id }
                        val isSelected = selectedCategoryFilter == category.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectCategoryFilter(category.id) },
                            leadingIcon = {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) category.accentColor
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(if (count > 0) "${category.label} ($count)" else category.label)
                            }
                        )
                    }
                    item(key = "filter_manage_categories") {
                        FilterChip(
                            selected = false,
                            onClick = onOpenCategorySettings,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Categorías") }
                        )
                    }
                }
            }

            item(key = "list_header") {
                val activeCatLabel = selectedCategoryFilter?.let {
                    CategoryResolver.resolve(it, categories).label
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, top = 4.dp)
                ) {
                    Text(
                        text = if (activeCatLabel == null) {
                            "Movimientos del día"
                        } else {
                            "Movimientos • $activeCatLabel"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (selectedCategoryFilter != null && filteredDayExpenses.isNotEmpty()) {
                        Text(
                            text = DateFormatUtils.formatCurrency(
                                filteredDayExpenses.sumOf { it.amount },
                                symbol
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = JetBrainsMonoFamily,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (filteredDayExpenses.isEmpty()) {
                item(key = "empty_day") {
                    EmptyDayCard(
                        hasFilter = selectedCategoryFilter != null,
                        onClearFilter = { onSelectCategoryFilter(null) }
                    )
                }
            } else {
                items(
                    items = filteredDayExpenses,
                    key = { it.id.ifBlank { "${it.dateKey}_${it.title}_${it.amount}" } }
                ) { expense ->
                    val resolvedCategory = CategoryResolver.resolve(expense.categoryId, categories)
                    ExpenseItemCard(
                        expense = expense,
                        category = resolvedCategory,
                        currencySymbol = symbol,
                        onClick = { onEditExpense(expense) },
                        onDeleteClick = { onDeleteExpense(expense) }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateFormatUtils.utcMillisFromDateKey(selectedDateKey)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onSelectDate(DateFormatUtils.dateKeyFromUtcMillis(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Ir al día")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DailyHeroControlCard(
    selectedDateKey: String,
    dayTotal: Double,
    weekTotal: Double,
    monthTotal: Double,
    dailyBudget: Double,
    currencySymbol: String,
    expenseCount: Int,
    onShiftDay: (Int) -> Unit,
    onOpenDatePicker: () -> Unit,
    onOpenBudgetSettings: () -> Unit,
) {
    val isOverBudget = dailyBudget > 0.0 && dayTotal > dailyBudget
    val rawProgress = if (dailyBudget > 0.0) (dayTotal / dailyBudget).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 350),
        label = "budget_progress"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
                            MaterialTheme.colorScheme.surfaceContainer,
                        )
                    )
                )
                .padding(18.dp)
        ) {
            // Day Selector Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { onShiftDay(-1) },
                    modifier = Modifier.testTag("day_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.cd_previous_day)
                    )
                }

                Surface(
                    onClick = onOpenDatePicker,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("day_picker_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = stringResource(R.string.cd_pick_date),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = DateFormatUtils.formatDateKeyFriendly(selectedDateKey),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(
                    onClick = { onShiftDay(1) },
                    modifier = Modifier.testTag("day_next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.cd_next_day)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Day Total
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "${stringResource(R.string.label_spent_day)} • $expenseCount ${
                            if (expenseCount == 1) "gasto" else "gastos"
                        }",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DateFormatUtils.formatCurrency(dayTotal, currencySymbol),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isOverBudget) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("day_total_text")
                    )
                }

                Surface(
                    onClick = onOpenBudgetSettings,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("budget_settings_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = stringResource(R.string.label_set_budget),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (dailyBudget > 0.0) {
                                "Meta ${DateFormatUtils.formatCurrencyCompact(dailyBudget, currencySymbol)}"
                            } else {
                                "Fijar meta"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            AnimatedVisibility(visible = dailyBudget > 0.0) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        color = if (isOverBudget) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val diff = kotlin.math.abs(dailyBudget - dayTotal)
                    val statusText = if (isOverBudget) {
                        "Te pasaste por ${DateFormatUtils.formatCurrency(diff, currencySymbol)}"
                    } else {
                        "Te quedan ${DateFormatUtils.formatCurrency(diff, currencySymbol)} disponibles hoy"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isOverBudget) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(12.dp))

            // Week & Month Totals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.label_this_week),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = DateFormatUtils.formatCurrency(weekTotal, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.label_this_month),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = DateFormatUtils.formatCurrency(monthTotal, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseItemCard(
    expense: Expense,
    category: CategoryConfig,
    currencySymbol: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("expense_item_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(category.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.label,
                    tint = category.accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title.ifBlank { category.label },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = if (!expense.note.isNullOrBlank()) {
                    "${category.label} • ${expense.note}"
                } else {
                    category.label
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (expense.isCreditPurchase) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val cardLabel = expense.cardName?.takeIf { it.isNotBlank() } ?: "Crédito"
                    val cuotasText = if (expense.safeInstallments > 1) " • ${expense.safeInstallments} cuotas" else ""
                    val statusLabel = if (expense.creditSettled) "Pagada" else "Pendiente"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (expense.creditSettled) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CreditCard,
                                contentDescription = null,
                                tint = if (expense.creditSettled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$cardLabel$cuotasText • $statusLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (expense.creditSettled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "-${DateFormatUtils.formatCurrency(expense.amount, currencySymbol)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.secondary
            )

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_expense),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyDayCard(
    hasFilter: Boolean,
    onClearFilter: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (hasFilter) "Sin gastos en esta categoría" else stringResource(R.string.empty_day_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasFilter) {
                    "Prueba viendo todas las categorías o agrega un nuevo gasto."
                } else {
                    stringResource(R.string.empty_day_subtitle)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (hasFilter) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onClearFilter) {
                    Text("Ver todos los gastos")
                }
            }
        }
    }
}
