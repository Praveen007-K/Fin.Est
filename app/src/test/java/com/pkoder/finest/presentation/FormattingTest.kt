package com.pkoder.finest.presentation

import com.pkoder.finest.presentation.components.sanitizeAmountInput
import com.pkoder.finest.presentation.util.Period
import com.pkoder.finest.presentation.util.asMoney
import com.pkoder.finest.presentation.util.asSignedMoney
import com.pkoder.finest.presentation.util.periodStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AmountInputTest {

    @Test
    fun `keeps decimals - the old isDigit filter dropped them`() {
        assertEquals("123.50", sanitizeAmountInput("123.50"))
    }

    @Test
    fun `strips letters and symbols`() {
        assertEquals("250", sanitizeAmountInput("2a5b0!"))
    }

    @Test
    fun `allows only one separator`() {
        assertEquals("12.34", sanitizeAmountInput("12.3.4"))
    }

    @Test
    fun `caps the fraction at two digits`() {
        assertEquals("9.99", sanitizeAmountInput("9.9999"))
    }

    @Test
    fun `treats a comma as a decimal separator`() {
        assertEquals("5.75", sanitizeAmountInput("5,75"))
    }

    @Test
    fun `handles a leading separator`() {
        assertEquals(".5", sanitizeAmountInput(".5"))
    }
}

class MoneyFormattingTest {

    @Test
    fun `formats with indian grouping and two decimals`() {
        assertEquals("₹1,23,456.78", 123456.78.asMoney())
    }

    @Test
    fun `groups crores in pairs after the first three digits`() {
        assertEquals("₹1,23,45,678.00", 12345678.0.asMoney())
    }

    @Test
    fun `always shows two decimals`() {
        assertEquals("₹250.00", 250.0.asMoney())
    }

    @Test
    fun `puts the minus sign before the symbol`() {
        assertEquals("-₹250.00", (-250.0).asMoney())
    }

    @Test
    fun `groups the first thousand normally`() {
        assertEquals("₹1,000.00", 1000.0.asMoney())
    }

    @Test
    fun `leaves three digits or fewer ungrouped`() {
        assertEquals("₹0.00", 0.0.asMoney())
        assertEquals("₹99.50", 99.5.asMoney())
    }

    @Test
    fun `rounds to paise`() {
        assertEquals("₹10.01", 10.005.asMoney())
    }

    @Test
    fun `signs amounts by direction`() {
        assertTrue(10.0.asSignedMoney(isExpense = true).startsWith("-"))
        assertTrue(10.0.asSignedMoney(isExpense = false).startsWith("+"))
    }
}

class PeriodTest {

    private fun at(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, 14, 30)
        }.timeInMillis

    @Test
    fun `month starts on the first at midnight`() {
        val start = periodStart(Period.MONTH, at(2026, Calendar.MARCH, 17))
        val calendar = Calendar.getInstance().apply { timeInMillis = start }

        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.MARCH, calendar.get(Calendar.MONTH))
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun `quarter reaches back two whole months`() {
        val start = periodStart(Period.QUARTER, at(2026, Calendar.MARCH, 17))
        val calendar = Calendar.getInstance().apply { timeInMillis = start }

        assertEquals(Calendar.JANUARY, calendar.get(Calendar.MONTH))
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `year starts on the first day of the year`() {
        val start = periodStart(Period.YEAR, at(2026, Calendar.MARCH, 17))
        val calendar = Calendar.getInstance().apply { timeInMillis = start }

        assertEquals(Calendar.JANUARY, calendar.get(Calendar.MONTH))
        assertEquals(1, calendar.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun `all time has no lower bound`() {
        assertEquals(0L, periodStart(Period.ALL, at(2026, Calendar.MARCH, 17)))
    }
}
