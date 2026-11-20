package com.example

import com.example.model.CurrencyCatalog
import com.example.model.Expense
import com.example.model.ExpenseCategory
import com.example.model.PaymentMethod
import com.example.util.DateFormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `dateKey shift and validation work correctly`() {
        assertTrue(DateFormatUtils.isValidDateKey("2026-11-19"))
        assertFalse(DateFormatUtils.isValidDateKey("2026-11"))
        assertEquals("2026-11-20", DateFormatUtils.shiftDateKey("2026-11-19", 1))
        assertEquals("2026-11-18", DateFormatUtils.shiftDateKey("2026-11-19", -1))
        assertEquals("2026-12", DateFormatUtils.shiftMonthKey("2026-11", 1))
        assertEquals("2026-10", DateFormatUtils.shiftMonthKey("2026-11", -1))
    }

    @Test
    fun `currentWeekOrderedDateKeys returns Monday through Sunday for any day in week`() {
        val expectedWeek = listOf(
            "2026-11-16", // Mon
            "2026-11-17", // Tue
            "2026-11-18", // Wed
            "2026-11-19", // Thu
            "2026-11-20", // Fri
            "2026-11-21", // Sat
            "2026-11-22", // Sun
        )
        assertEquals(expectedWeek, DateFormatUtils.currentWeekOrderedDateKeys("2026-11-16"))
        assertEquals(expectedWeek, DateFormatUtils.currentWeekOrderedDateKeys("2026-11-19"))
        assertEquals(expectedWeek, DateFormatUtils.currentWeekOrderedDateKeys("2026-11-22"))
        assertTrue(DateFormatUtils.formatWeekRangeLabel(expectedWeek).contains("16 – 22"))
    }

    @Test
    fun `currency formatting displays symbol, COP, and decimals cleanly`() {
        assertEquals("$1,250.50", DateFormatUtils.formatCurrency(1250.5, "$"))
        assertEquals("€45", DateFormatUtils.formatCurrencyCompact(45.0, "€"))
        assertEquals("€45.75", DateFormatUtils.formatCurrencyCompact(45.75, "€"))
        assertEquals("COP $50,000", DateFormatUtils.formatCurrency(50000.0, "COP $"))
        assertEquals("Bs 120", DateFormatUtils.formatCurrency(120.0, "Bs"))
    }

    @Test
    fun `currency catalog includes Colombia and obeys rule length bounds`() {
        val colombia = CurrencyCatalog.findBySymbol("COP $")
        assertNotNull(colombia)
        assertEquals("Colombia", colombia?.country)
        CurrencyCatalog.all.forEach { option ->
            assertTrue(
                "Symbol ${option.symbol} exceeds Firestore 5-char bound",
                option.symbol.length in 1..5
            )
        }
    }

    @Test
    fun `expense category deserialization falls back safely`() {
        assertEquals(ExpenseCategory.FOOD, ExpenseCategory.fromRaw("FOOD"))
        assertEquals(ExpenseCategory.OTHER, ExpenseCategory.fromRaw("UNKNOWN_VALUE"))
        assertEquals(ExpenseCategory.OTHER, ExpenseCategory.fromRaw(null))
    }

    @Test
    fun `expense credit card properties compute installments and fallback cleanly`() {
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromRaw(null))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromRaw("UNKNOWN"))
        assertEquals(PaymentMethod.CREDIT_CARD, PaymentMethod.fromRaw("CREDIT_CARD"))

        val creditExpense = Expense(
            id = "e1",
            title = "Televisor",
            amount = 1200.0,
            category = ExpenseCategory.SHOPPING.name,
            paymentMethod = PaymentMethod.CREDIT_CARD.name,
            cardName = "Visa Nu",
            installments = 12L,
            creditSettled = false
        )
        assertTrue(creditExpense.isCreditPurchase)
        assertEquals(12, creditExpense.safeInstallments)
        assertEquals(100.0, creditExpense.installmentAmount, 0.001)

        val clampedExpense = creditExpense.copy(installments = 0L)
        assertEquals(1, clampedExpense.safeInstallments)
        assertEquals(1200.0, clampedExpense.installmentAmount, 0.001)
    }
}
