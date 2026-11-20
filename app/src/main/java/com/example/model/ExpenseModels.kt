package com.example.model

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.TheaterComedy
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
) {
    FOOD("Comida", Icons.Rounded.Restaurant, CatFoodColor),
    TRANSPORT("Transporte", Icons.Rounded.DirectionsCar, CatTransportColor),
    HOME("Hogar", Icons.Rounded.Home, CatHomeColor),
    SHOPPING("Compras", Icons.Rounded.ShoppingBag, CatShoppingColor),
    HEALTH("Salud", Icons.Rounded.Favorite, CatHealthColor),
    LEISURE("Ocio", Icons.Rounded.TheaterComedy, CatLeisureColor),
    BILLS("Servicios", Icons.AutoMirrored.Rounded.ReceiptLong, CatBillsColor),
    OTHER("Otros", Icons.Rounded.Category, CatOtherColor);

    companion object {
        fun fromRaw(raw: String?): ExpenseCategory =
            entries.firstOrNull { it.name == raw } ?: OTHER
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
