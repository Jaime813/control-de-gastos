package com.example.data

import com.example.model.Expense
import com.example.model.PaymentMethod
import com.example.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface ExpenseDataSource {
    fun observeExpenses(): Flow<List<Expense>>
    fun observeUserSettings(): Flow<UserSettings>

    suspend fun addExpense(
        title: String,
        amount: Double,
        categoryId: String,
        categoryLabel: String,
        dateKey: String,
        note: String?,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        cardName: String? = null,
        installments: Int = 1,
        creditSettled: Boolean = false,
    ): Result<Unit>

    suspend fun updateExpense(
        expenseId: String,
        title: String,
        amount: Double,
        categoryId: String,
        categoryLabel: String,
        dateKey: String,
        note: String?,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        cardName: String? = null,
        installments: Int = 1,
        creditSettled: Boolean = false,
    ): Result<Unit>

    suspend fun setCreditSettled(
        expenseId: String,
        settled: Boolean,
    ): Result<Unit>

    suspend fun deleteExpense(expenseId: String): Result<Unit>

    suspend fun saveUserSettings(
        dailyBudget: Double,
        currencySymbol: String,
    ): Result<Unit>
}

class FirestoreExpenseDataSource(
    private val repository: ExpenseRepository,
    private val userId: String,
) : ExpenseDataSource {
    override fun observeExpenses(): Flow<List<Expense>> =
        repository.observeExpenses(userId)

    override fun observeUserSettings(): Flow<UserSettings> =
        repository.observeUserSettings(userId)

    override suspend fun addExpense(
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
    ): Result<Unit> = repository.addExpense(
        userId = userId,
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

    override suspend fun updateExpense(
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
    ): Result<Unit> = repository.updateExpense(
        userId = userId,
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

    override suspend fun setCreditSettled(expenseId: String, settled: Boolean): Result<Unit> =
        repository.setCreditSettled(userId, expenseId, settled)

    override suspend fun deleteExpense(expenseId: String): Result<Unit> =
        repository.deleteExpense(userId, expenseId)

    override suspend fun saveUserSettings(
        dailyBudget: Double,
        currencySymbol: String,
    ): Result<Unit> = repository.saveUserSettings(userId, dailyBudget, currencySymbol)
}
