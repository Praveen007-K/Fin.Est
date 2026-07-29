package com.pkoder.finest.presentation.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Money and date formatting used across every screen and the notifications.
 *
 * `NumberFormat`/`SimpleDateFormat` are not thread-safe and notifications are built off the main
 * thread, so each formatter is held per thread instead of being shared.
 */

/**
 * Two decimals, no grouping — the lakh/crore separators are inserted by [groupIndianDigits].
 *
 * Neither shortcut works here: the JVM's `en-IN` currency format groups in thousands, and
 * `DecimalFormat` honours only one grouping size, so a `#,##,##0.00` pattern silently degrades to
 * `1,234,567`.
 */
private val plainAmount = ThreadLocal.withInitial {
    DecimalFormat("0.00", DecimalFormatSymbols(Locale.ENGLISH))
}

private const val RUPEE = "₹"

private fun dateFormat(pattern: String) = ThreadLocal.withInitial {
    SimpleDateFormat(pattern, Locale.getDefault())
}

private val dayMonthYear = dateFormat("dd MMM yyyy")
private val dayMonthYearTime = dateFormat("dd MMM yyyy, hh:mm a")
private val dayMonth = dateFormat("dd MMM")
private val timeOnly = dateFormat("hh:mm a")
private val monthYear = dateFormat("MMM yyyy")

/** e.g. `₹1,23,456.78`; a negative value reads `-₹250.00`, not `₹-250.00`. */
fun Double.asMoney(): String {
    val formatted = RUPEE + groupIndianDigits(plainAmount.get()!!.format(abs(this)))
    return if (this < 0) "-$formatted" else formatted
}

/**
 * Indian grouping: the last three whole digits, then pairs.
 * `123456.78` → `1,23,456.78`, `12345678.00` → `1,23,45,678.00`.
 */
internal fun groupIndianDigits(fixed: String): String {
    val whole = fixed.substringBefore('.')
    val fraction = fixed.substringAfter('.', missingDelimiterValue = "")
    val grouped = if (whole.length <= 3) {
        whole
    } else {
        val head = whole.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
        "$head,${whole.takeLast(3)}"
    }
    return if (fraction.isEmpty()) grouped else "$grouped.$fraction"
}

/**
 * Whole rupees, e.g. `₹46,559` — for summary tiles, chart tooltips and the donut centre, where the
 * paise cost more width than they carry meaning and would push a lakh figure into an ellipsis.
 */
fun Double.asMoneyWhole(): String {
    val formatted = RUPEE + groupIndianDigits(abs(this).roundToLong().toString())
    return if (this < 0) "-$formatted" else formatted
}

/** Money with an explicit direction, e.g. `-₹250.00` for an expense. */
fun Double.asSignedMoney(isExpense: Boolean): String =
    (if (isExpense) "-" else "+") + kotlin.math.abs(this).asMoney()

fun Long.asDate(): String = dayMonthYear.get()!!.format(Date(this))

fun Long.asDateTime(): String = dayMonthYearTime.get()!!.format(Date(this))

fun Long.asTime(): String = timeOnly.get()!!.format(Date(this))

fun Long.asMonthYear(): String = monthYear.get()!!.format(Date(this))

/** `Today` / `Yesterday` / `04 Mar` / `04 Mar 2025` — for list section headers. */
fun Long.asRelativeDay(now: Long = System.currentTimeMillis()): String {
    val then = Calendar.getInstance().apply { timeInMillis = this@asRelativeDay }
    val today = Calendar.getInstance().apply { timeInMillis = now }

    if (then.sameDayAs(today)) return "Today"

    today.add(Calendar.DAY_OF_YEAR, -1)
    if (then.sameDayAs(today)) return "Yesterday"

    val sameYear = then.get(Calendar.YEAR) == Calendar.getInstance()
        .apply { timeInMillis = now }
        .get(Calendar.YEAR)
    return if (sameYear) dayMonth.get()!!.format(Date(this)) else asDate()
}

private fun Calendar.sameDayAs(other: Calendar): Boolean =
    get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
