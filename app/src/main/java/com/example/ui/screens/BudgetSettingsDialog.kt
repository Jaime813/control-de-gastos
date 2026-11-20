package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.CurrencyCatalog
import com.example.model.UserSettings
import com.example.ui.theme.JetBrainsMonoFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetSettingsDialog(
    currentSettings: UserSettings,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (dailyBudget: Double, currencySymbol: String) -> Unit,
) {
    var budgetText by remember(currentSettings) {
        mutableStateOf(
            if (currentSettings.dailyBudget > 0.0) {
                if (currentSettings.dailyBudget % 1.0 == 0.0) {
                    currentSettings.dailyBudget.toLong().toString()
                } else {
                    currentSettings.dailyBudget.toString()
                }
            } else ""
        )
    }
    var selectedSymbol by remember(currentSettings) {
        mutableStateOf(currentSettings.currencySymbol.ifBlank { "$" })
    }
    var showAllCurrencies by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val selectedOption = remember(selectedSymbol) {
        CurrencyCatalog.findBySymbol(selectedSymbol)
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = "Control y moneda",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Define tu límite de gasto diario (déjalo en 0 si no quieres límite) y elige la moneda de tu país.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { raw ->
                        val filtered = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) {
                            budgetText = filtered
                            errorText = null
                        }
                    },
                    label = { Text("Meta diaria ($selectedSymbol)") },
                    placeholder = {
                        Text(if (selectedSymbol == "COP $") "Ej. 50000" else "Ej. 300")
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFamily
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_budget_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Moneda",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (selectedOption != null) {
                        "${selectedOption.country} • ${selectedOption.name} (${selectedOption.symbol})"
                    } else {
                        "Símbolo personalizado ($selectedSymbol)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CurrencyCatalog.quickSymbols.forEach { symbol ->
                        FilterChip(
                            selected = selectedSymbol == symbol,
                            onClick = {
                                selectedSymbol = symbol
                                errorText = null
                            },
                            label = {
                                Text(
                                    text = if (symbol == "COP $") "COP $ (Col)" else symbol,
                                    fontFamily = JetBrainsMonoFamily
                                )
                            },
                            modifier = Modifier.testTag("currency_chip_$symbol")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showAllCurrencies = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("more_currencies_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Todos los países y símbolo propio")
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = if (budgetText.isBlank()) 0.0 else budgetText.toDoubleOrNull()
                    val cleanSymbol = selectedSymbol.trim()
                    if (parsed == null || parsed < 0.0 || parsed > 10_000_000.0) {
                        errorText = "Ingresa una meta válida entre 0 y 10,000,000."
                    } else if (cleanSymbol.isEmpty() || cleanSymbol.length > 5) {
                        errorText = "El símbolo debe tener entre 1 y 5 caracteres."
                    } else {
                        onSave(parsed, cleanSymbol)
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.testTag("save_budget_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancelar")
            }
        }
    )

    if (showAllCurrencies) {
        CurrencyPickerDialog(
            currentSymbol = selectedSymbol,
            onDismiss = { showAllCurrencies = false },
            onSelectSymbol = { symbol ->
                selectedSymbol = symbol
                errorText = null
                showAllCurrencies = false
            }
        )
    }
}

@Composable
private fun CurrencyPickerDialog(
    currentSymbol: String,
    onDismiss: () -> Unit,
    onSelectSymbol: (String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var customSymbol by remember(currentSymbol) {
        mutableStateOf(if (CurrencyCatalog.findBySymbol(currentSymbol) == null) currentSymbol else "")
    }

    val filteredOptions = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) CurrencyCatalog.all
        else CurrencyCatalog.all.filter {
            it.country.lowercase().contains(q) ||
                it.name.lowercase().contains(q) ||
                it.symbol.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Elegir moneda",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar país o moneda...") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                ) {
                    items(filteredOptions, key = { "${it.country}_${it.symbol}" }) { option ->
                        val isSelected = option.symbol == currentSymbol
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectSymbol(option.symbol) }
                                .minimumInteractiveComponentSize()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Text(
                                    text = option.symbol,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.country,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = option.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Seleccionada",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customSymbol,
                        onValueChange = { if (it.length <= 5) customSymbol = it },
                        label = { Text("Otro símbolo (máx. 5)") },
                        placeholder = { Text("Ej. UY$") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val clean = customSymbol.trim()
                            if (clean.isNotEmpty()) {
                                onSelectSymbol(clean)
                            }
                        },
                        enabled = customSymbol.trim().isNotEmpty(),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("Usar")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
