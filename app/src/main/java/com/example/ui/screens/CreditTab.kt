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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CategoryConfig
import com.example.model.CategoryResolver
import com.example.model.Expense
import com.example.model.UserSettings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.util.DateFormatUtils

private data class CardSummary(
    val cardLabel: String,
    val pendingTotal: Double,
    val monthlyInstallmentTotal: Double,
    val pendingCount: Int,
)

@Composable
fun CreditTab(
    expenses: List<Expense>,
    categories: List<CategoryConfig>,
    settings: UserSettings,
    onEditExpense: (Expense) -> Unit,
    onToggleSettled: (Expense) -> Unit,
    modifier: Modifier = Modifier,
) {
    val symbol = settings.currencySymbol.ifBlank { "$" }
    // Filter tabs: 0 = Pendientes, 1 = Pagadas, 2 = Todas
    var statusFilter by remember { mutableIntStateOf(0) }
    var selectedCardFilter by remember { mutableStateOf<String?>(null) }

    val creditExpenses = remember(expenses) {
        expenses.filter { it.isCreditPurchase }
    }
    val pendingExpenses = remember(creditExpenses) {
        creditExpenses.filter { !it.creditSettled }
    }
    val settledExpenses = remember(creditExpenses) {
        creditExpenses.filter { it.creditSettled }
    }

    val totalPendingDebt = remember(pendingExpenses) { pendingExpenses.sumOf { it.amount } }
    val estimatedMonthlyInstallment = remember(pendingExpenses) {
        pendingExpenses.sumOf { it.installmentAmount }
    }
    val totalSettledAmount = remember(settledExpenses) { settledExpenses.sumOf { it.amount } }

    val cardSummaries = remember(pendingExpenses) {
        pendingExpenses
            .groupBy { it.cardName?.takeIf { name -> name.isNotBlank() } ?: "Sin tarjeta" }
            .map { (cardLabel, list) ->
                CardSummary(
                    cardLabel = cardLabel,
                    pendingTotal = list.sumOf { it.amount },
                    monthlyInstallmentTotal = list.sumOf { it.installmentAmount },
                    pendingCount = list.size
                )
            }
            .sortedByDescending { it.pendingTotal }
    }

    val displayedExpenses = remember(creditExpenses, statusFilter, selectedCardFilter) {
        val byStatus = when (statusFilter) {
            0 -> creditExpenses.filter { !it.creditSettled }
            1 -> creditExpenses.filter { it.creditSettled }
            else -> creditExpenses
        }
        if (selectedCardFilter == null) byStatus
        else byStatus.filter {
            (it.cardName?.takeIf { n -> n.isNotBlank() } ?: "Sin tarjeta") == selectedCardFilter
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "credit_hero") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("credit_hero_card")
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                                        MaterialTheme.colorScheme.surfaceContainer,
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CreditCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Deuda pendiente a crédito",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${pendingExpenses.size} ${
                                        if (pendingExpenses.size == 1) "compra pendiente" else "compras pendientes"
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = DateFormatUtils.formatCurrency(totalPendingDebt, symbol),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("credit_pending_total_text")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Cuota mensual aprox.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = DateFormatUtils.formatCurrencyCompact(
                                            estimatedMonthlyInstallment,
                                            symbol
                                        ),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Ya pagado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = DateFormatUtils.formatCurrencyCompact(
                                            totalSettledAmount,
                                            symbol
                                        ),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (cardSummaries.isNotEmpty()) {
                item(key = "cards_section_title") {
                    Text(
                        text = "Saldo pendiente por tarjeta",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                item(key = "cards_row") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(cardSummaries, key = { it.cardLabel }) { card ->
                            val isSelected = selectedCardFilter == card.cardLabel
                            Surface(
                                onClick = {
                                    selectedCardFilter =
                                        if (isSelected) null else card.cardLabel
                                },
                                shape = RoundedCornerShape(18.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = 1.dp,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(176.dp)
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = card.cardLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.CreditCard,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = DateFormatUtils.formatCurrencyCompact(
                                            card.pendingTotal,
                                            symbol
                                        ),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${card.pendingCount} ${
                                            if (card.pendingCount == 1) "compra" else "compras"
                                        } • ${
                                            DateFormatUtils.formatCurrencyCompact(
                                                card.monthlyInstallmentTotal,
                                                symbol
                                            )
                                        }/mes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "credit_status_filters") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val options = listOf(
                        "Pendientes (${pendingExpenses.size})",
                        "Pagadas (${settledExpenses.size})",
                        "Todas (${creditExpenses.size})"
                    )
                    options.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = statusFilter == idx,
                            onClick = { statusFilter = idx },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }
            }

            if (displayedExpenses.isEmpty()) {
                item(key = "credit_empty") {
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
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CreditCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (creditExpenses.isEmpty()) {
                                    "Sin compras a crédito"
                                } else {
                                    "No hay compras en este filtro"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (creditExpenses.isEmpty()) {
                                    "Toca \"Nuevo gasto\" y selecciona Tarjeta / Crédito para controlar cuotas y saldos."
                                } else {
                                    "Cambia el filtro arriba o quita la selección de tarjeta."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (selectedCardFilter != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(onClick = { selectedCardFilter = null }) {
                                    Text("Ver todas las tarjetas")
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = displayedExpenses,
                    key = { it.id.ifBlank { "${it.dateKey}_${it.title}_${it.amount}" } }
                ) { expense ->
                    val resolvedCategory = CategoryResolver.resolve(expense.categoryId, categories)
                    CreditPurchaseItemCard(
                        expense = expense,
                        category = resolvedCategory,
                        currencySymbol = symbol,
                        onClick = { onEditExpense(expense) },
                        onToggleSettled = { onToggleSettled(expense) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditPurchaseItemCard(
    expense: Expense,
    category: CategoryConfig,
    currencySymbol: String,
    onClick: () -> Unit,
    onToggleSettled: () -> Unit,
) {
    val cardLabel = expense.cardName?.takeIf { it.isNotBlank() } ?: "Tarjeta de crédito"
    val installmentsText = if (expense.safeInstallments > 1) {
        "${expense.safeInstallments} cuotas de ${
            DateFormatUtils.formatCurrencyCompact(expense.installmentAmount, currencySymbol)
        }"
    } else {
        "1 cuota"
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("credit_item_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (expense.creditSettled) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (expense.creditSettled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$cardLabel • $installmentsText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = DateFormatUtils.formatDateKeyFriendly(expense.dateKey),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = DateFormatUtils.formatCurrency(expense.amount, currencySymbol),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (expense.creditSettled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    onClick = onToggleSettled,
                    shape = RoundedCornerShape(10.dp),
                    color = if (expense.creditSettled) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_settled_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (expense.creditSettled) Icons.Rounded.CheckCircle
                            else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (expense.creditSettled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (expense.creditSettled) "Pagada" else "Marcar pagada",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (expense.creditSettled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}
