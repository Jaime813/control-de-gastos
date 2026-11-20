package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ExpenseDataSource
import com.example.data.local.CategoryConfigRepository
import com.example.model.CategoryConfig
import com.example.model.CategoryResolver
import com.example.model.Expense
import com.example.model.PaymentMethod
import com.example.model.UiState
import com.example.model.UserSettings
import com.example.util.DateFormatUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HistoryPeriodFilter(val label: String) {
    ALL("Todo"),
    THIS_MONTH("Este mes"),
    LAST_7_DAYS("Últimos 7 días"),
    TODAY("Hoy"),
}

data class ExpenseDashboardUiState(
    val selectedDateKey: String = DateFormatUtils.todayDateKey(),
    val selectedMonthKey: String = DateFormatUtils.currentMonthKey(),
    val selectedCategoryFilter: String? = null,
    val historyCategoryFilter: String? = null,
    val historySearchQuery: String = "",
    val historyPeriodFilter: HistoryPeriodFilter = HistoryPeriodFilter.ALL,
    val isSaving: Boolean = false,
    val snackbarMessage: String? = null,
)

class ExpenseViewModel(
    private val dataSource: ExpenseDataSource,
    private val categoryRepository: CategoryConfigRepository,
    private val currentSessionId: String,
) : ViewModel() {

    private val _controls = MutableStateFlow(ExpenseDashboardUiState())
    val controls: StateFlow<ExpenseDashboardUiState> = _controls.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { categoryRepository.ensureSeeded() }
                .onFailure { Log.w(TAG, "Error seeding categories in Room", it) }
        }
    }

    val categoriesState: StateFlow<List<CategoryConfig>> =
        categoryRepository.observeCategories()
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing Room categories", error)
                emit(CategoryResolver.defaultConfigs())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CategoryResolver.defaultConfigs()
            )

    val expensesState: StateFlow<UiState<List<Expense>>> =
        dataSource.observeExpenses()
            .map<List<Expense>, UiState<List<Expense>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing expenses for $currentSessionId", rootError)
                emit(UiState.Error(rootError.message ?: "No se pudieron cargar tus gastos."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val settingsState: StateFlow<UserSettings> =
        dataSource.observeUserSettings()
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing settings for $currentSessionId", firestoreError ?: error)
                emit(UserSettings(id = currentSessionId, userId = currentSessionId))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UserSettings(id = currentSessionId, userId = currentSessionId)
            )

    fun selectDate(dateKey: String) {
        if (DateFormatUtils.isValidDateKey(dateKey)) {
            _controls.update { it.copy(selectedDateKey = dateKey) }
        }
    }

    fun shiftDay(delta: Int) {
        _controls.update { state ->
            state.copy(selectedDateKey = DateFormatUtils.shiftDateKey(state.selectedDateKey, delta))
        }
    }

    fun selectMonth(monthKey: String) {
        _controls.update { it.copy(selectedMonthKey = monthKey) }
    }

    fun shiftMonth(delta: Int) {
        _controls.update { state ->
            state.copy(selectedMonthKey = DateFormatUtils.shiftMonthKey(state.selectedMonthKey, delta))
        }
    }

    fun selectCategoryFilter(categoryId: String?) {
        _controls.update { state ->
            val next = if (state.selectedCategoryFilter == categoryId) null else categoryId
            state.copy(selectedCategoryFilter = next)
        }
    }

    fun selectHistoryCategoryFilter(categoryId: String?) {
        _controls.update { state ->
            val next = if (categoryId != null && state.historyCategoryFilter == categoryId) null else categoryId
            state.copy(historyCategoryFilter = next)
        }
    }

    fun setHistoryCategoryDirect(categoryId: String?) {
        _controls.update { it.copy(historyCategoryFilter = categoryId) }
    }

    fun setHistorySearchQuery(query: String) {
        _controls.update { it.copy(historySearchQuery = query) }
    }

    fun setHistoryPeriodFilter(period: HistoryPeriodFilter) {
        _controls.update { it.copy(historyPeriodFilter = period) }
    }

    fun clearHistoryFilters() {
        _controls.update {
            it.copy(
                historyCategoryFilter = null,
                historySearchQuery = "",
                historyPeriodFilter = HistoryPeriodFilter.ALL
            )
        }
    }

    fun clearSnackbar() {
        _controls.update { it.copy(snackbarMessage = null) }
    }

    fun showMessage(message: String) {
        _controls.update { it.copy(snackbarMessage = message) }
    }

    fun addExpense(
        title: String,
        amount: Double,
        categoryId: String,
        categoryLabel: String,
        dateKey: String,
        note: String?,
        paymentMethod: PaymentMethod,
        cardName: String?,
        installments: Int,
        creditSettled: Boolean,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            _controls.update { it.copy(isSaving = true) }
            val result = dataSource.addExpense(
                title = title,
                amount = amount,
                categoryId = categoryId,
                categoryLabel = categoryLabel,
                dateKey = dateKey,
                note = note,
                paymentMethod = paymentMethod,
                cardName = cardName,
                installments = installments,
                creditSettled = creditSettled,
            )
            result.fold(
                onSuccess = {
                    _controls.update {
                        it.copy(
                            isSaving = false,
                            selectedDateKey = dateKey,
                            selectedMonthKey = dateKey.take(7),
                            snackbarMessage = if (paymentMethod == PaymentMethod.CREDIT_CARD) {
                                "Compra a crédito guardada"
                            } else {
                                "Gasto guardado"
                            }
                        )
                    }
                    onDone()
                },
                onFailure = { error ->
                    _controls.update {
                        it.copy(
                            isSaving = false,
                            snackbarMessage = error.localizedMessage ?: "Error al guardar el gasto"
                        )
                    }
                }
            )
        }
    }

    fun updateExpense(
        expenseId: String,
        title: String,
        amount: Double,
        categoryId: String,
        categoryLabel: String,
        dateKey: String,
        note: String?,
        paymentMethod: PaymentMethod,
        cardName: String?,
        installments: Int,
        creditSettled: Boolean,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            _controls.update { it.copy(isSaving = true) }
            val result = dataSource.updateExpense(
                expenseId = expenseId,
                title = title,
                amount = amount,
                categoryId = categoryId,
                categoryLabel = categoryLabel,
                dateKey = dateKey,
                note = note,
                paymentMethod = paymentMethod,
                cardName = cardName,
                installments = installments,
                creditSettled = creditSettled,
            )
            result.fold(
                onSuccess = {
                    _controls.update {
                        it.copy(
                            isSaving = false,
                            selectedDateKey = dateKey,
                            selectedMonthKey = dateKey.take(7),
                            snackbarMessage = "Gasto actualizado"
                        )
                    }
                    onDone()
                },
                onFailure = { error ->
                    _controls.update {
                        it.copy(
                            isSaving = false,
                            snackbarMessage = error.localizedMessage ?: "Error al actualizar el gasto"
                        )
                    }
                }
            )
        }
    }

    fun toggleCreditSettled(expense: Expense) {
        if (expense.id.isBlank()) return
        val nextSettled = !expense.creditSettled
        viewModelScope.launch {
            val result = dataSource.setCreditSettled(expense.id, nextSettled)
            result.fold(
                onSuccess = {
                    _controls.update {
                        it.copy(
                            snackbarMessage = if (nextSettled) "Marcada como pagada" else "Marcada como pendiente"
                        )
                    }
                },
                onFailure = { error ->
                    _controls.update {
                        it.copy(
                            snackbarMessage = error.localizedMessage ?: "No se pudo actualizar el estado"
                        )
                    }
                }
            )
        }
    }

    fun deleteExpense(expenseId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _controls.update { it.copy(isSaving = true) }
            val result = dataSource.deleteExpense(expenseId)
            result.fold(
                onSuccess = {
                    _controls.update {
                        it.copy(isSaving = false, snackbarMessage = "Gasto eliminado")
                    }
                    onDone()
                },
                onFailure = { error ->
                    _controls.update {
                        it.copy(
                            isSaving = false,
                            snackbarMessage = error.localizedMessage ?: "Error al eliminar el gasto"
                        )
                    }
                }
            )
        }
    }

    fun saveUserSettings(
        dailyBudget: Double,
        currencySymbol: String,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            _controls.update { it.copy(isSaving = true) }
            val result = dataSource.saveUserSettings(
                dailyBudget = dailyBudget,
                currencySymbol = currencySymbol,
            )
            result.fold(
                onSuccess = {
                    _controls.update {
                        it.copy(isSaving = false, snackbarMessage = "Configuración guardada")
                    }
                    onDone()
                },
                onFailure = { error ->
                    _controls.update {
                        it.copy(
                            isSaving = false,
                            snackbarMessage = error.localizedMessage ?: "Error al guardar la configuración"
                        )
                    }
                }
            )
        }
    }

    fun saveCategoryConfig(
        id: String?,
        label: String,
        iconKey: String,
        colorHex: Long,
        isEnabled: Boolean,
        monthlyLimit: Double,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _controls.update { it.copy(isSaving = true) }
            val result = categoryRepository.saveCategory(
                id = id,
                label = label,
                iconKey = iconKey,
                colorHex = colorHex,
                isEnabled = isEnabled,
                monthlyLimit = monthlyLimit,
            )
            result.fold(
                onSuccess = {
                    _controls.update {
                        it.copy(
                            isSaving = false,
                            snackbarMessage = if (id == null) "Categoría creada en Room" else "Categoría actualizada"
                        )
                    }
                    onDone()
                },
                onFailure = { error ->
                    _controls.update {
                        it.copy(
                            isSaving = false,
                            snackbarMessage = error.localizedMessage ?: "No se pudo guardar la categoría"
                        )
                    }
                }
            )
        }
    }

    fun toggleCategoryEnabled(categoryId: String, enabled: Boolean) {
        viewModelScope.launch {
            val result = categoryRepository.setCategoryEnabled(categoryId, enabled)
            result.onFailure { error ->
                _controls.update {
                    it.copy(snackbarMessage = error.localizedMessage ?: "No se pudo actualizar la categoría")
                }
            }
        }
    }

    fun deleteCustomCategory(categoryId: String) {
        viewModelScope.launch {
            val result = categoryRepository.deleteCustomCategory(categoryId)
            result.fold(
                onSuccess = {
                    _controls.update { state ->
                        state.copy(
                            selectedCategoryFilter = if (state.selectedCategoryFilter == categoryId) null else state.selectedCategoryFilter,
                            historyCategoryFilter = if (state.historyCategoryFilter == categoryId) null else state.historyCategoryFilter,
                            snackbarMessage = "Categoría eliminada"
                        )
                    }
                },
                onFailure = { error ->
                    _controls.update {
                        it.copy(snackbarMessage = error.localizedMessage ?: "No se pudo eliminar la categoría")
                    }
                }
            )
        }
    }

    fun resetCategoryDefaults() {
        viewModelScope.launch {
            val result = categoryRepository.resetDefaults()
            result.fold(
                onSuccess = {
                    _controls.update {
                        it.copy(
                            selectedCategoryFilter = null,
                            historyCategoryFilter = null,
                            snackbarMessage = "Categorías predeterminadas restauradas"
                        )
                    }
                },
                onFailure = { error ->
                    _controls.update {
                        it.copy(snackbarMessage = error.localizedMessage ?: "No se pudieron restaurar las categorías")
                    }
                }
            )
        }
    }

    private companion object {
        const val TAG = "ExpenseVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
