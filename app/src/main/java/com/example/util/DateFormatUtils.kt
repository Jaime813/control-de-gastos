package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateFormatUtils {
    private val spanishLocale = Locale.forLanguageTag("es-ES")
    private val isoDateRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")

    fun todayDateKey(): String = formatCalendarAsDateKey(Calendar.getInstance())

    fun currentMonthKey(): String = todayDateKey().take(7)

    fun isValidDateKey(dateKey: String): Boolean = isoDateRegex.matches(dateKey)

    fun shiftDateKey(dateKey: String, daysDelta: Int): String {
        val cal = parseDateKeyToCalendar(dateKey)
        cal.add(Calendar.DAY_OF_MONTH, daysDelta)
        return formatCalendarAsDateKey(cal)
    }

    fun shiftMonthKey(monthKey: String, monthsDelta: Int): String {
        val cal = parseMonthKeyToCalendar(monthKey)
        cal.add(Calendar.MONTH, monthsDelta)
        return formatCalendarAsDateKey(cal).take(7)
    }

    fun formatDateKeyFriendly(dateKey: String): String {
        val today = todayDateKey()
        val yesterday = shiftDateKey(today, -1)
        return when (dateKey) {
            today -> "Hoy"
            yesterday -> "Ayer"
            else -> {
                val cal = parseDateKeyToCalendar(dateKey)
                val formatter = SimpleDateFormat("EEE, d MMM", spanishLocale)
                formatter.format(cal.time).replaceFirstChar { it.titlecase(spanishLocale) }
            }
        }
    }

    fun formatDateKeyFull(dateKey: String): String {
        val cal = parseDateKeyToCalendar(dateKey)
        val formatter = SimpleDateFormat("EEEE d 'de' MMMM", spanishLocale)
        return formatter.format(cal.time).replaceFirstChar { it.titlecase(spanishLocale) }
    }

    fun formatMonthKeyFriendly(monthKey: String): String {
        val cal = parseMonthKeyToCalendar(monthKey)
        val formatter = SimpleDateFormat("MMMM yyyy", spanishLocale)
        return formatter.format(cal.time).replaceFirstChar { it.titlecase(spanishLocale) }
    }

    fun dateKeyFromUtcMillis(utcMillis: Long): String {
        // Material3 DatePicker returns UTC midnight millis for the chosen calendar date
        val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val year = utcCal.get(Calendar.YEAR)
        val month = utcCal.get(Calendar.MONTH) + 1
        val day = utcCal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    fun utcMillisFromDateKey(dateKey: String): Long {
        val cal = parseDateKeyToCalendar(dateKey)
        val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
        }
        return utcCal.timeInMillis
    }

    fun currentWeekOrderedDateKeys(referenceDateKey: String = todayDateKey()): List<String> {
        val cal = parseDateKeyToCalendar(referenceDateKey)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysSinceMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
        return (0 until 7).map {
            val key = formatCalendarAsDateKey(cal)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            key
        }
    }

    fun currentWeekDateKeys(referenceDateKey: String = todayDateKey()): Set<String> =
        currentWeekOrderedDateKeys(referenceDateKey).toSet()

    fun formatWeekRangeLabel(weekKeys: List<String>): String {
        if (weekKeys.isEmpty()) return ""
        val firstCal = parseDateKeyToCalendar(weekKeys.first())
        val lastCal = parseDateKeyToCalendar(weekKeys.last())
        val dayFormat = SimpleDateFormat("d", spanishLocale)
        val monthFormat = SimpleDateFormat("MMM", spanishLocale)
        val firstDay = dayFormat.format(firstCal.time)
        val lastDay = dayFormat.format(lastCal.time)
        val firstMonth = monthFormat.format(firstCal.time).trimEnd('.')
        val lastMonth = monthFormat.format(lastCal.time).trimEnd('.')
        return if (firstCal.get(Calendar.MONTH) == lastCal.get(Calendar.MONTH)) {
            "$firstDay – $lastDay $lastMonth"
        } else {
            "$firstDay $firstMonth – $lastDay $lastMonth"
        }
    }

    fun lastSevenDaysKeys(referenceDateKey: String = todayDateKey()): List<String> {
        return (-6..0).map { offset -> shiftDateKey(referenceDateKey, offset) }
    }

    fun shortDayLabel(dateKey: String): String {
        val cal = parseDateKeyToCalendar(dateKey)
        val formatter = SimpleDateFormat("EEEEE", spanishLocale)
        return formatter.format(cal.time).uppercase(spanishLocale)
    }

    fun dayOfMonthNumber(dateKey: String): Int {
        return dateKey.takeLast(2).toIntOrNull() ?: 1
    }

    fun daysInMonth(monthKey: String): Int {
        val cal = parseMonthKeyToCalendar(monthKey)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun formatCurrency(amount: Double, symbol: String): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val pattern = if (amount % 1.0 == 0.0) "#,##0" else "#,##0.00"
        val formattedNumber = DecimalFormat(pattern, symbols).format(amount)
        val separator = if (symbol.lastOrNull()?.isLetter() == true) " " else ""
        return "$symbol$separator$formattedNumber"
    }

    fun formatCurrencyCompact(amount: Double, symbol: String): String =
        formatCurrency(amount, symbol)

    private fun formatCalendarAsDateKey(cal: Calendar): String {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    private fun parseDateKeyToCalendar(dateKey: String): Calendar {
        val parts = dateKey.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
        val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
        return Calendar.getInstance().apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }
    }

    private fun parseMonthKeyToCalendar(monthKey: String): Calendar {
        val parts = monthKey.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
        return Calendar.getInstance().apply {
            clear()
            set(year, month, 1, 12, 0, 0)
        }
    }
}
