package com.pkoder.finest.presentation.util

import java.util.Calendar

/** Date-range filter shared by the dashboard, history and insights. */
enum class Period(val label: String) {
    MONTH("This month"),
    QUARTER("3 months"),
    YEAR("This year"),
    ALL("All time")
}

/**
 * Inclusive lower bound for a period, or 0 for [Period.ALL].
 *
 * Pure so it can be unit-tested: `now` is a parameter rather than being read from the clock.
 */
fun periodStart(period: Period, now: Long = System.currentTimeMillis()): Long {
    if (period == Period.ALL) return 0L

    val calendar = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    when (period) {
        Period.MONTH -> calendar.set(Calendar.DAY_OF_MONTH, 1)
        Period.QUARTER -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.add(Calendar.MONTH, -2)
        }
        Period.YEAR -> calendar.set(Calendar.DAY_OF_YEAR, 1)
        Period.ALL -> Unit
    }
    return calendar.timeInMillis
}
