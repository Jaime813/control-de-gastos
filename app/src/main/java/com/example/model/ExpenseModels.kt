package com.example.model

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.CatBillsColor
import com.example.ui.theme.CatFoodColor
import com.example.ui.theme.CatHealthColor
import com.example.ui.theme.CatHomeColor
import com.example.ui.theme.CatLeisureColor
import com.example.ui.theme.CatOtherColor
import com.example.ui.theme.CatShoppingColor
import com.example.ui.theme.CatTransportColor
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ExpenseCategory(
    val label: String,
    val icon: ImageVector,
    val accentColor: Color,
    val defaultColorHex: Long,
    val iconOption: CategoryIconOption,
) {
    FOOD("Comida", Icons.Rounded.Restaurant, CatFoodColor, 0xFFE06D3BL, CategoryIconOption.FOOD),
    TRANSPORT("Transporte", Icons.Rounded.DirectionsCar, CatTransportColor, 0xFF2D7DD2L, CategoryIconOption.TRANSPORT),
    HOME("Vivienda", Icons.Rounded.Home, CatHomeColor, 0xFF0E7C61L, CategoryIconOption.HOME),
    SHOPPING("Compras", Icons.Rounded.ShoppingBag, CatShoppingColor, 0xFF9B5DE5L, CategoryIconOption.SHOPPING),
    HEALTH("Salud", Icons.Rounded.Favorite, CatHealthColor, 0xFFD6456BL, CategoryIconOption.HEALTH),
    LEISURE("Ocio", Icons.Rounded.TheaterComedy, CatLeisureColor, 0xFFE09F1FL, CategoryIconOption.LEISURE),
    BILLS("Servicios", Icons.AutoMirrored.Rounded.ReceiptLong, CatBillsColor, 0xFF4A6FA5L, CategoryIconOption.BILLS),
    OTHER("Otros", Icons.Rounded.Category, CatOtherColor, 0xFF64748BL, CategoryIconOption.OTHER);

    companion object {
        fun fromRaw(raw: String?): ExpenseCategory =
            entries.firstOrNull { it.name == raw } ?: OTHER
    }
}

enum class CategoryIconOption(
    val label: String,
    val icon: ImageVector,
) {
    FOOD("Comida", Icons.Rounded.Restaurant),
    TRANSPORT("Transporte", Icons.Rounded.DirectionsCar),
    HOME("Vivienda", Icons.Rounded.Home),
    SHOPPING("Compras", Icons.Rounded.ShoppingBag),
    HEALTH("Salud", Icons.Rounded.Favorite),
    LEISURE("Ocio", Icons.Rounded.TheaterComedy),
    BILLS("Servicios", Icons.AutoMirrored.Rounded.ReceiptLong),
    COFFEE("Café", Icons.Rounded.Coffee),
    SCHOOL("Educación", Icons.Rounded.School),
    PETS("Mascotas", Icons.Rounded.Pets),
    FITNESS("Deporte", Icons.Rounded.FitnessCenter),
    WORK("Trabajo", Icons.Rounded.Work),
    FLIGHT("Viajes", Icons.Rounded.Flight),
    OTHER("General", Icons.Rounded.Category);

    companion object {
        fun fromKey(key: String?): CategoryIconOption =
            entries.firstOrNull { it.name == key } ?: OTHER
    }
}

object CategoryColorPalette {
    val options: List<Long> = listOf(
        0xFFE06D3BL, // Terracotta
        0xFF2D7DD2L, // Cobalt
        0xFF0E7C61L, // Emerald
        0xFF9B5DE5L, // Violet
        0xFFD6456BL, // Rose
        0xFFE09F1FL, // Amber
        0xFF4A6FA5L, // Slate Blue
        0xFF64748BL, // Slate
        0xFF00A896L, // Teal
        0xFFC94C4CL, // Coral Red
    )
}

data class CategoryConfig(
    val id: String,
    val label: String,
    val iconKey: String,
    val colorHex: Long,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val monthlyLimit: Double = 0.0,
) {
    val icon: ImageVector
        get() = CategoryIconOption.fromKey(iconKey).icon

    val accentColor: Color
        get() = Color(colorHex)
}

object CategoryResolver {
    fun defaultConfigs(): List<CategoryConfig> =
        ExpenseCategory.entries.mapIndexed { index, cat ->
            CategoryConfig(
                id = cat.name,
                label = cat.label,
                iconKey = cat.iconOption.name,
                colorHex = cat.defaultColorHex,
                isEnabled = true,
                isDefault = true,
                sortOrder = index,
                monthlyLimit = 0.0,
            )
        }

    fun resolve(categoryId: String?, configs: List<CategoryConfig>): CategoryConfig {
        val cleanId = categoryId?.takeIf { it.isNotBlank() } ?: ExpenseCategory.OTHER.name
        configs.firstOrNull { it.id == cleanId }?.let { return it }
        val fallback = ExpenseCategory.fromRaw(cleanId)
        return CategoryConfig(
            id = fallback.name,
            label = fallback.label,
            iconKey = fallback.iconOption.name,
            colorHex = fallback.defaultColorHex,
            isEnabled = true,
            isDefault = true,
            sortOrder = fallback.ordinal,
        )
    }
}

enum class PaymentMethod(
    val label: String,
    val shortLabel: String,
    val icon: ImageVector,
) {
    CASH("Efectivo / Débito", "Efectivo", Icons.Rounded.AccountBalanceWallet),
    CREDIT_CARD("Tarjeta / Crédito", "Crédito", Icons.Rounded.CreditCard);

    companion object {
        fun fromRaw(raw: String?): PaymentMethod =
            entries.firstOrNull { it.name == raw } ?: CASH
    }
}

@Keep
@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = ExpenseCategory.FOOD.name,
    val dateKey: String = "",
    val monthKey: String = "",
    val note: String? = null,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val cardName: String? = null,
    val installments: Long = 1L,
    val creditSettled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val categoryId: String
        get() = category.ifBlank { ExpenseCategory.OTHER.name }

    val categoryEnum: ExpenseCategory
        get() = ExpenseCategory.fromRaw(category)

    val paymentMethodEnum: PaymentMethod
        get() = PaymentMethod.fromRaw(paymentMethod)

    val isCreditPurchase: Boolean
        get() = paymentMethodEnum == PaymentMethod.CREDIT_CARD

    val safeInstallments: Int
        get() = installments.toInt().coerceIn(1, 60)

    val installmentAmount: Double
        get() = amount / safeInstallments
}

@Keep
@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dailyBudget: Double = 0.0,
    val currencySymbol: String = "$",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class CurrencyOption(
    val country: String,
    val name: String,
    val symbol: String,
)

object CurrencyCatalog {
    val all: List<CurrencyOption> = listOf(
        CurrencyOption("Colombia", "Peso colombiano", "COP $"),
        CurrencyOption("Internacional / EE. UU.", "Dólar / Peso", "$"),
        CurrencyOption("Estados Unidos", "Dólar estadounidense", "US$"),
        CurrencyOption("Unión Europea", "Euro", "€"),
        CurrencyOption("México", "Peso mexicano", "MX$"),
        CurrencyOption("Perú", "Sol peruano", "S/"),
        CurrencyOption("Guatemala", "Quetzal", "Q"),
        CurrencyOption("Venezuela / Bolivia", "Bolívar / Boliviano", "Bs"),
        CurrencyOption("Reino Unido", "Libra esterlina", "£"),
        CurrencyOption("Chile", "Peso chileno", "CLP $"),
        CurrencyOption("Argentina", "Peso argentino", "ARS $"),
        CurrencyOption("República Dominicana", "Peso dominicano", "RD$"),
        CurrencyOption("Costa Rica", "Colón costarricense", "₡"),
        CurrencyOption("Brasil", "Real brasileño", "R$"),
        CurrencyOption("Uruguay", "Peso uruguayo", "UY$"),
        CurrencyOption("Paraguay", "Guaraní", "Gs."),
        CurrencyOption("Panamá", "Balboa", "B/."),
        CurrencyOption("Nicaragua", "Córdoba", "C$"),
        CurrencyOption("Honduras", "Lempira", "L"),
    )

    val quickSymbols: List<String> = listOf("COP $", "$", "€", "S/", "Q", "Bs", "£")

    fun findBySymbol(symbol: String): CurrencyOption? =
        all.firstOrNull { it.symbol == symbol }
}
