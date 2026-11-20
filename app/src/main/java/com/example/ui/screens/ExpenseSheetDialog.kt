package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.Expense
import com.example.model.ExpenseCategory
import com.example.model.PaymentMethod
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.util.DateFormatUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseSheetDialog(
    initialExpense: Expense?,
    defaultDateKey: String,
    defaultPaymentMethod: PaymentMethod,
    recentCardNames: List<String>,
    currencySymbol: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        category: ExpenseCategory,
        dateKey: String,
        note: String?,
        paymentMethod: PaymentMethod,
        cardName: String?,
        installments: Int,
        creditSettled: Boolean,
    ) -> Unit,
    onDelete: ((expenseId: String) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditing = initialExpense != null

    var amountText by remember(initialExpense) {
        mutableStateOf(
            initialExpense?.amount?.let { amt ->
                if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
            } ?: ""
        )
    }
    var titleText by remember(initialExpense) {
        mutableStateOf(initialExpense?.title ?: "")
    }
    var noteText by remember(initialExpense) {
        mutableStateOf(initialExpense?.note ?: "")
    }
    var selectedCategory by remember(initialExpense) {
        mutableStateOf(initialExpense?.categoryEnum ?: ExpenseCategory.FOOD)
    }
    var selectedDateKey by remember(initialExpense, defaultDateKey) {
        mutableStateOf(initialExpense?.dateKey?.takeIf { it.isNotBlank() } ?: defaultDateKey)
    }
    var selectedPaymentMethod by remember(initialExpense, defaultPaymentMethod) {
        mutableStateOf(initialExpense?.paymentMethodEnum ?: defaultPaymentMethod)
    }
    var cardNameText by remember(initialExpense) {
        mutableStateOf(initialExpense?.cardName ?: "")
    }
    var selectedInstallments by remember(initialExpense) {
        mutableIntStateOf(initialExpense?.safeInstallments ?: 1)
    }
    var creditSettled by remember(initialExpense) {
        mutableStateOf(initialExpense?.creditSettled ?: false)
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amountFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (!isEditing) {
            amountFocusRequester.requestFocus()
        }
    }

    val quickSuggestions = remember(selectedCategory) {
        when (selectedCategory) {
            ExpenseCategory.FOOD -> listOf("Café", "Desayuno", "Almuerzo", "Cena", "Súper")
            ExpenseCategory.TRANSPORT -> listOf("Transporte", "Gasolina", "Taxi / App", "Estacionamiento")
            ExpenseCategory.HOME -> listOf("Limpieza", "Reparación", "Hogar", "Mantenimiento")
            ExpenseCategory.SHOPPING -> listOf("Ropa", "Regalo", "Tienda", "Personal")
            ExpenseCategory.HEALTH -> listOf("Farmacia", "Consulta", "Bienestar")
            ExpenseCategory.LEISURE -> listOf("Cine", "Salida", "Suscripción", "Café con amigos")
            ExpenseCategory.BILLS -> listOf("Luz", "Agua", "Internet", "Teléfono")
            ExpenseCategory.OTHER -> listOf("Varios", "Encargo", "Imprevisto")
        }
    }

    val cardSuggestions = remember(recentCardNames) {
        (recentCardNames + listOf("Visa", "Mastercard", "Nu", "Bancolombia", "Amex"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(6)
    }

    val installmentPresets = listOf(1, 2, 3, 6, 12, 24)

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isEditing) "Editar gasto" else "Nuevo gasto",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (initialExpense != null && onDelete != null) {
                    IconButton(
                        onClick = { onDelete(initialExpense.id) },
                        enabled = !isSaving,
                        modifier = Modifier.testTag("delete_expense_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Eliminar gasto",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { raw ->
                    val filtered = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        val decimalPart = filtered.substringAfter('.', "")
                        if (decimalPart.length <= 2) {
                            amountText = filtered
                            validationError = null
                        }
                    }
                },
                label = { Text("Monto total") },
                placeholder = { Text("0.00") },
                prefix = {
                    val prefixStyle = if (currencySymbol.length > 2) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.headlineSmall
                    }
                    Text(
                        text = "$currencySymbol ",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        style = prefixStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocusRequester)
                    .testTag("amount_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Method Selector (Cash vs Credit Card)
            Text(
                text = "Forma de pago",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PaymentMethod.entries.forEach { method ->
                    val isSelected = selectedPaymentMethod == method
                    val containerColor = if (isSelected) {
                        if (method == PaymentMethod.CREDIT_CARD) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                    val accent = if (isSelected) {
                        if (method == PaymentMethod.CREDIT_CARD) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val borderCol = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(containerColor)
                            .border(1.5.dp, borderCol, RoundedCornerShape(14.dp))
                            .clickable { selectedPaymentMethod = method }
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                            .testTag("payment_method_${method.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = method.icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = method.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }

            // Credit Card & Installments Details
            AnimatedVisibility(visible = selectedPaymentMethod == PaymentMethod.CREDIT_CARD) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Detalles de tarjeta o crédito",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = cardNameText,
                            onValueChange = { if (it.length <= 40) cardNameText = it },
                            label = { Text("Tarjeta o crédito (opcional)") },
                            placeholder = { Text("Ej. Visa, Nu, Bancolombia...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_name_input")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            cardSuggestions.forEach { card ->
                                SuggestionChip(
                                    onClick = { cardNameText = card },
                                    label = { Text(card, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Número de cuotas",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            installmentPresets.forEach { count ->
                                FilterChip(
                                    selected = selectedInstallments == count,
                                    onClick = { selectedInstallments = count },
                                    label = {
                                        Text(
                                            text = if (count == 1) "1 cuota" else "$count cuotas",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    modifier = Modifier.testTag("installments_chip_$count")
                                )
                            }
                        }

                        val parsedPreviewAmount = amountText.toDoubleOrNull() ?: 0.0
                        if (selectedInstallments > 1 && parsedPreviewAmount > 0.0) {
                            val cuota = parsedPreviewAmount / selectedInstallments
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Cuota mensual aprox.: ${
                                    DateFormatUtils.formatCurrency(cuota, currencySymbol)
                                } ($selectedInstallments cuotas)",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        if (isEditing) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Compra ya pagada / liquidada",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Switch(
                                    checked = creditSettled,
                                    onCheckedChange = { creditSettled = it },
                                    modifier = Modifier.testTag("credit_settled_switch")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category grid
            Text(
                text = "Categoría",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ExpenseCategory.entries.forEach { cat ->
                    val selected = cat == selectedCategory
                    val bgColor = if (selected) cat.accentColor.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceContainerLow
                    val borderColor = if (selected) cat.accentColor
                    else MaterialTheme.colorScheme.outlineVariant

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(bgColor)
                            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable { selectedCategory = cat }
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("category_chip_${cat.name.lowercase()}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(cat.accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = null,
                                tint = cat.accentColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) cat.accentColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title / Description
            OutlinedTextField(
                value = titleText,
                onValueChange = {
                    if (it.length <= 80) titleText = it
                },
                label = { Text("¿En qué gastaste? (opcional)") },
                placeholder = { Text("Ej. ${quickSuggestions.first()}") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                quickSuggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { titleText = suggestion },
                        label = { Text(suggestion, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("expense_date_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Fecha del gasto",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${DateFormatUtils.formatDateKeyFriendly(selectedDateKey)} • ${
                                DateFormatUtils.formatDateKeyFull(selectedDateKey)
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { if (it.length <= 160) noteText = it },
                label = { Text("Nota adicional (opcional)") },
                maxLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input")
            )

            if (validationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = validationError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull()
                    if (parsedAmount == null || parsedAmount <= 0.0) {
                        validationError = "Ingresa un monto mayor a 0."
                    } else if (parsedAmount > 10_000_000.0) {
                        validationError = "El monto es demasiado alto."
                    } else {
                        onSave(
                            titleText.trim().ifBlank { selectedCategory.label },
                            parsedAmount,
                            selectedCategory,
                            selectedDateKey,
                            noteText.trim().takeIf { it.isNotEmpty() },
                            selectedPaymentMethod,
                            cardNameText.trim().takeIf { it.isNotEmpty() },
                            selectedInstallments,
                            creditSettled,
                        )
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_expense_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = if (isEditing) "Actualizar gasto" else "Guardar gasto",
                        style = MaterialTheme.typography.titleMedium
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
                            selectedDateKey = DateFormatUtils.dateKeyFromUtcMillis(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar")
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
