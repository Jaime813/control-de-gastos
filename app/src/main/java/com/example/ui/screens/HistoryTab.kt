package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CategoryConfig
import com.example.model.CategoryResolver
import com.example.model.Expense
import com.example.model.UserSettings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.HistoryPeriodFilter
import com.example.util.DateFormatUtils

@Composable
fun HistoryTab(
    expenses: List<Expense>,
    categories: List<CategoryConfig>,
    settings: UserSettings,
    selectedCategoryId: String?,
    searchQuery: String,
    selectedPeriod: HistoryPeriodFilter,
    onSelectCategoryFilter: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectPeriodFilter: (HistoryPeriodFilter) -> Unit,
    onClearFilters: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onOpenCategorySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val symbol = settings.currencySymbol.ifBlank { "$" }
    val todayKey = remember { DateFormatUtils.todayDateKey() }
    val currentMonthKey = remember { DateFormatUtils.currentMonthKey() }
    val last7DaysSet = remember { DateFormatUtils.lastSevenDaysKeys().toSet() }

    val periodFilteredExpenses = remember(expenses, selectedPeriod, todayKey, currentMonthKey, last7DaysSet) {
        when (selectedPeriod) {
            HistoryPeriodFilter.ALL -> expenses
            HistoryPeriodFilter.THIS_MONTH -> expenses.filter { it.monthKey == currentMonthKey }
            HistoryPeriodFilter.LAST_7_DAYS -> expenses.filter { it.dateKey in last7DaysSet }
            HistoryPeriodFilter.TODAY -> expenses.filter { it.dateKey == todayKey }
        }
    }

    val searchFilteredExpenses = remember(periodFilteredExpenses, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) periodFilteredExpenses
        else periodFilteredExpenses.filter { exp ->
            exp.title.lowercase().contains(q) ||
                (exp.note?.lowercase()?.contains(q) == true) ||
                (exp.cardName?.lowercase()?.contains(q) == true)
        }
    }

    // Categories shown in filter bar: all enabled categories plus any category present in expenses
    val filterableCategories = remember(categories, searchFilteredExpenses) {
        val usedIds = searchFilteredExpenses.map { it.categoryId }.toSet()
        val known = categories.filter { it.isEnabled || it.id in usedIds }
        val knownIds = known.map { it.id }.toSet()
        val extraUnknown = (usedIds - knownIds).map { id -> CategoryResolver.resolve(id, categories) }
        known + extraUnknown
    }

    val finalFilteredExpenses = remember(searchFilteredExpenses, selectedCategoryId) {
        if (selectedCategoryId == null) searchFilteredExpenses
        else searchFilteredExpenses.filter { it.categoryId == selectedCategoryId }
    }

    val filteredTotal = remember(finalFilteredExpenses) {
        finalFilteredExpenses.sumOf { it.amount }
    }

    val selectedCategoryObj = remember(selectedCategoryId, categories) {
        selectedCategoryId?.let { CategoryResolver.resolve(it, categories) }
    }

    val currentMonthCategorySpend = remember(expenses, selectedCategoryId, currentMonthKey) {
        if (selectedCategoryId == null) 0.0
        else expenses.filter { it.monthKey == currentMonthKey && it.categoryId == selectedCategoryId }
            .sumOf { it.amount }
    }

    val groupedByDate = remember(finalFilteredExpenses) {
        finalFilteredExpenses.groupBy { it.dateKey }.entries.toList()
    }

    val hasActiveFilter = selectedCategoryId != null ||
        searchQuery.isNotBlank() ||
        selectedPeriod != HistoryPeriodFilter.ALL

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "history_search_and_period") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Buscar por descripción, nota o tarjeta...") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Limpiar búsqueda"
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("history_search_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(HistoryPeriodFilter.entries, key = { it.name }) { period ->
                                val isSelected = selectedPeriod == period
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectPeriodFilter(period) },
                                    label = { Text(period.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("history_period_${period.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            item(key = "history_category_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Filtrar por categoría",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    TextButton(
                        onClick = onOpenCategorySettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("configure_categories_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Configurar")
                    }
                }
            }

            item(key = "history_category_chips") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item(key = "history_cat_all") {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { onSelectCategoryFilter(null) },
                            label = { Text("Todas (${searchFilteredExpenses.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("history_cat_chip_all")
                        )
                    }
                    items(filterableCategories, key = { it.id }) { cat ->
                        val count = searchFilteredExpenses.count { it.categoryId == cat.id }
                        val isSelected = selectedCategoryId == cat.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectCategoryFilter(cat.id) },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) cat.accentColor
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(if (count > 0) "${cat.label} ($count)" else cat.label)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.accentColor.copy(alpha = 0.18f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("history_cat_chip_${cat.id}")
                        )
                    }
                }
            }

            item(key = "history_summary_banner") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_summary_banner")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedCategoryObj?.let { "Historial • ${it.label}" }
                                        ?: "Historial completo",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${finalFilteredExpenses.size} ${
                                        if (finalFilteredExpenses.size == 1) "movimiento" else "movimientos"
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = DateFormatUtils.formatCurrency(filteredTotal, symbol),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = selectedCategoryObj?.accentColor ?: MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("history_filtered_total_text")
                            )
                        }

                        if (selectedCategoryObj != null && selectedCategoryObj.monthlyLimit > 0.0) {
                            val limit = selectedCategoryObj.monthlyLimit
                            val ratio = (currentMonthCategorySpend / limit).toFloat().coerceIn(0f, 1f)
                            val exceeded = currentMonthCategorySpend > limit
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                color = if (exceeded) MaterialTheme.colorScheme.secondary
                                else selectedCategoryObj.accentColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Este mes en ${selectedCategoryObj.label}: ${
                                    DateFormatUtils.formatCurrencyCompact(currentMonthCategorySpend, symbol)
                                } de ${DateFormatUtils.formatCurrencyCompact(limit, symbol)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (exceeded) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (finalFilteredExpenses.isEmpty()) {
                item(key = "history_empty") {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (hasActiveFilter) {
                                    "Sin gastos con estos filtros"
                                } else {
                                    "Aún no tienes historial de gastos"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (hasActiveFilter) {
                                    "Prueba seleccionando otra categoría o limpiando los filtros activos."
                                } else {
                                    "Tus gastos diarios y compras a crédito aparecerán aquí ordenados por fecha."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (hasActiveFilter) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(onClick = onClearFilters) {
                                    Text("Limpiar filtros")
                                }
                            }
                        }
                    }
                }
            } else {
                groupedByDate.forEach { (dateKey, dayList) ->
                    item(key = "history_date_$dateKey") {
                        val daySum = dayList.sumOf { it.amount }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, end = 4.dp, top = 6.dp)
                        ) {
                            Text(
                                text = DateFormatUtils.formatDateKeyFriendly(dateKey),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DateFormatUtils.formatCurrencyCompact(daySum, symbol),
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(
                        items = dayList,
                        key = { "hist_${it.id.ifBlank { "${it.dateKey}_${it.title}_${it.amount}" }}" }
                    ) { expense ->
                        val cat = CategoryResolver.resolve(expense.categoryId, categories)
                        HistoryExpenseCard(
                            expense = expense,
                            category = cat,
                            currencySymbol = symbol,
                            onClick = { onEditExpense(expense) },
                            onDeleteClick = { onDeleteExpense(expense) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryExpenseCard(
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
            .testTag("history_expense_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(category.accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.label,
                    tint = category.accentColor,
                    modifier = Modifier.size(20.dp)
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
                    val cuotas = if (expense.safeInstallments > 1) " • ${expense.safeInstallments} cuotas" else ""
                    val status = if (expense.creditSettled) "Pagada" else "Pendiente"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CreditCard,
                            contentDescription = null,
                            tint = if (expense.creditSettled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$cardLabel$cuotas • $status",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (expense.creditSettled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
