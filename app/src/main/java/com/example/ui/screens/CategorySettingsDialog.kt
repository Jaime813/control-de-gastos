package com.example.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CategoryColorPalette
import com.example.model.CategoryConfig
import com.example.model.CategoryIconOption
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.util.DateFormatUtils

@Composable
fun CategorySettingsDialog(
    categories: List<CategoryConfig>,
    currencySymbol: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSaveCategory: (
        id: String?,
        label: String,
        iconKey: String,
        colorHex: Long,
        isEnabled: Boolean,
        monthlyLimit: Double,
        onDone: () -> Unit,
    ) -> Unit,
    onToggleEnabled: (categoryId: String, enabled: Boolean) -> Unit,
    onDeleteCustomCategory: (categoryId: String) -> Unit,
    onResetDefaults: () -> Unit,
) {
    var showEditor by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryConfig?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Column {
                Text(
                    text = "Configurar categorías",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Guardado localmente en tu dispositivo (Room)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Personaliza nombre, ícono, color o límite mensual, y activa solo las categorías que usas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        editingCategory = null
                        showEditor = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("add_category_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nueva categoría")
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 310.dp)
                ) {
                    items(categories, key = { it.id }) { item ->
                        CategoryConfigRow(
                            category = item,
                            currencySymbol = currencySymbol,
                            onToggleEnabled = { enabled -> onToggleEnabled(item.id, enabled) },
                            onEdit = {
                                editingCategory = item
                                showEditor = true
                            },
                            onDelete = if (!item.isDefault) {
                                { onDeleteCustomCategory(item.id) }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = onResetDefaults,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("reset_categories_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restaurar predeterminadas")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_categories_button")
            ) {
                Text("Listo")
            }
        }
    )

    if (showEditor) {
        CategoryEditorDialog(
            initialCategory = editingCategory,
            currencySymbol = currencySymbol,
            isSaving = isSaving,
            onDismiss = { showEditor = false },
            onSave = { label, iconKey, colorHex, monthlyLimit ->
                val current = editingCategory
                onSaveCategory(
                    current?.id,
                    label,
                    iconKey,
                    colorHex,
                    current?.isEnabled ?: true,
                    monthlyLimit
                ) {
                    showEditor = false
                }
            }
        )
    }
}

@Composable
private fun CategoryConfigRow(
    category: CategoryConfig,
    currencySymbol: String,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("category_config_row_${category.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(category.accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.label,
                    tint = category.accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (category.isEnabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = buildString {
                    append(if (category.isDefault) "Predeterminada" else "Personalizada")
                    if (category.monthlyLimit > 0.0) {
                        append(" • Límite ")
                        append(DateFormatUtils.formatCurrencyCompact(category.monthlyLimit, currencySymbol))
                    }
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Editar ${category.label}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Eliminar ${category.label}",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Switch(
                checked = category.isEnabled,
                onCheckedChange = onToggleEnabled,
                modifier = Modifier.testTag("toggle_category_${category.id}")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorDialog(
    initialCategory: CategoryConfig?,
    currencySymbol: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        label: String,
        iconKey: String,
        colorHex: Long,
        monthlyLimit: Double,
    ) -> Unit,
) {
    var labelText by remember(initialCategory) {
        mutableStateOf(initialCategory?.label ?: "")
    }
    var selectedIconKey by remember(initialCategory) {
        mutableStateOf(initialCategory?.iconKey ?: CategoryIconOption.HOME.name)
    }
    var selectedColorHex by remember(initialCategory) {
        mutableLongStateOf(initialCategory?.colorHex ?: CategoryColorPalette.options.first())
    }
    var limitText by remember(initialCategory) {
        mutableStateOf(
            initialCategory?.monthlyLimit?.takeIf { it > 0.0 }?.let { lim ->
                if (lim % 1.0 == 0.0) lim.toLong().toString() else lim.toString()
            } ?: ""
        )
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = if (initialCategory == null) "Nueva categoría" else "Editar categoría",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = labelText,
                    onValueChange = {
                        if (it.length <= 32) {
                            labelText = it
                            errorText = null
                        }
                    },
                    label = { Text("Nombre de la categoría") },
                    placeholder = { Text("Ej. Vivienda, Educación, Mascotas") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_label_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { raw ->
                        val filtered = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) {
                            limitText = filtered
                            errorText = null
                        }
                    },
                    label = { Text("Límite mensual opcional ($currencySymbol)") },
                    placeholder = { Text("0 = sin límite") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = JetBrainsMonoFamily
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_limit_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryColorPalette.options.forEach { hex ->
                        val selected = hex == selectedColorHex
                        val color = Color(hex)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (selected) {
                                        Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColorHex = hex }
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Color seleccionado",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Ícono",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val accent = Color(selectedColorHex)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIconOption.entries.forEach { option ->
                        val selected = option.name == selectedIconKey
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) accent.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                )
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedIconKey = option.name }
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
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
                    val cleanLabel = labelText.trim()
                    val parsedLimit = if (limitText.isBlank()) 0.0 else limitText.toDoubleOrNull()
                    if (cleanLabel.isEmpty()) {
                        errorText = "Ingresa un nombre para la categoría."
                    } else if (parsedLimit == null || parsedLimit < 0.0 || parsedLimit > 10_000_000.0) {
                        errorText = "Ingresa un límite válido."
                    } else {
                        onSave(cleanLabel, selectedIconKey, selectedColorHex, parsedLimit)
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.testTag("save_category_button")
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
}
