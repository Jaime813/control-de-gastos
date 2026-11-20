package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CategoryConfigRepository
import com.example.data.local.GuestExpenseDatabase
import com.example.data.local.LocalGuestRepository
import com.example.model.CategoryIconOption
import com.example.model.ExpenseCategory
import com.example.model.PaymentMethod
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GastoDiario", appName)
    }

    @Test
    fun `guest room repository supports cash and credit expenses, settling, and settings`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GuestExpenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = LocalGuestRepository(db.guestExpenseDao())

            val addResult = repo.addExpense(
                title = "Café",
                amount = 45.0,
                categoryId = ExpenseCategory.FOOD.name,
                categoryLabel = ExpenseCategory.FOOD.label,
                dateKey = "2026-11-19",
                note = "Mañana",
                paymentMethod = PaymentMethod.CASH,
                cardName = null,
                installments = 1,
                creditSettled = false
            )
            assertTrue(addResult.isSuccess)

            val savedList = repo.observeExpenses().first()
            assertEquals(1, savedList.size)
            assertEquals("Café", savedList.first().title)
            assertEquals(45.0, savedList.first().amount, 0.001)
            assertFalse(savedList.first().isCreditPurchase)

            val expenseId = savedList.first().id
            val updateResult = repo.updateExpense(
                expenseId = expenseId,
                title = "Café y pan",
                amount = 65.0,
                categoryId = ExpenseCategory.FOOD.name,
                categoryLabel = ExpenseCategory.FOOD.label,
                dateKey = "2026-11-19",
                note = null,
                paymentMethod = PaymentMethod.CASH,
                cardName = null,
                installments = 1,
                creditSettled = false
            )
            assertTrue(updateResult.isSuccess)
            assertEquals("Café y pan", repo.observeExpenses().first().first().title)

            val creditAddResult = repo.addExpense(
                title = "Audífonos",
                amount = 300.0,
                categoryId = ExpenseCategory.SHOPPING.name,
                categoryLabel = ExpenseCategory.SHOPPING.label,
                dateKey = "2026-11-19",
                note = "Compra en línea",
                paymentMethod = PaymentMethod.CREDIT_CARD,
                cardName = "Visa Nu",
                installments = 6,
                creditSettled = false
            )
            assertTrue(creditAddResult.isSuccess)

            val creditExpense = repo.observeExpenses().first().first { it.isCreditPurchase }
            assertEquals("Visa Nu", creditExpense.cardName)
            assertEquals(6, creditExpense.safeInstallments)
            assertEquals(50.0, creditExpense.installmentAmount, 0.001)
            assertFalse(creditExpense.creditSettled)

            val settleResult = repo.setCreditSettled(creditExpense.id, true)
            assertTrue(settleResult.isSuccess)
            val settledExpense = repo.observeExpenses().first().first { it.id == creditExpense.id }
            assertTrue(settledExpense.creditSettled)

            val settingsResult = repo.saveUserSettings(dailyBudget = 300.0, currencySymbol = "€")
            assertTrue(settingsResult.isSuccess)
            val settings = repo.observeUserSettings().first()
            assertEquals(300.0, settings.dailyBudget, 0.001)
            assertEquals("€", settings.currencySymbol)

            val deleteResult = repo.deleteExpense(expenseId)
            assertTrue(deleteResult.isSuccess)
            assertEquals(1, repo.observeExpenses().first().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `room category config repository seeds defaults, saves custom categories, toggles, and resets`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GuestExpenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val categoryRepo = CategoryConfigRepository(db.categoryConfigDao())
            categoryRepo.ensureSeeded()

            val initial = categoryRepo.observeCategories().first()
            assertEquals(8, initial.size)
            assertTrue(initial.any { it.id == "HOME" && it.label == "Vivienda" })

            // Create custom category in Room
            val saveResult = categoryRepo.saveCategory(
                id = null,
                label = "Mascotas",
                iconKey = CategoryIconOption.PETS.name,
                colorHex = 0xFF00A896L,
                isEnabled = true,
                monthlyLimit = 150000.0
            )
            assertTrue(saveResult.isSuccess)
            val customId = saveResult.getOrThrow()

            val afterAdd = categoryRepo.observeCategories().first()
            assertEquals(9, afterAdd.size)
            val customCat = afterAdd.first { it.id == customId }
            assertEquals("Mascotas", customCat.label)
            assertFalse(customCat.isDefault)
            assertEquals(150000.0, customCat.monthlyLimit, 0.001)

            // Disable a category
            assertTrue(categoryRepo.setCategoryEnabled("LEISURE", false).isSuccess)
            assertFalse(categoryRepo.observeCategories().first().first { it.id == "LEISURE" }.isEnabled)

            // Default category cannot be deleted, custom can be deleted
            assertTrue(categoryRepo.deleteCustomCategory("FOOD").isFailure)
            assertTrue(categoryRepo.deleteCustomCategory(customId).isSuccess)
            assertEquals(8, categoryRepo.observeCategories().first().size)

            // Reset restores enabled flags and labels
            assertTrue(categoryRepo.resetDefaults().isSuccess)
            assertTrue(categoryRepo.observeCategories().first().all { it.isEnabled })
        } finally {
            db.close()
        }
    }
}
