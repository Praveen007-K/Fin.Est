package com.pkoder.finest.presentation.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

/** One column of the history chart. */
data class Bucket(val label: String, val total: Double)

/**
 * Splits a period into the columns the bar chart draws: weeks inside a month, months inside a
 * quarter, quarters inside a year, years for all time.
 *
 * Only elapsed buckets are emitted, so the chart never carries empty columns for the rest of the
 * month. Pure, with `now` injected, for the same reason as [periodStart].
 *
 * @param entries `(timestamp, amount)` pairs; anything before the period start is ignored.
 */
fun Period.buckets(
    entries: List<Pair<Long, Double>>,
    now: Long = System.currentTimeMillis()
): List<Bucket> {
    val start = periodStart(this, now)
    val relevant = entries.filter { it.first >= start }

    return when (this) {
        Period.MONTH -> {
            val elapsed = weekOfMonth(now) + 1
            val totals = DoubleArray(elapsed)
            relevant.forEach { (timestamp, amount) ->
                val index = weekOfMonth(timestamp)
                if (index in totals.indices) totals[index] += amount
            }
            totals.mapIndexed { index, total -> Bucket("W${index + 1}", total) }
        }

        Period.QUARTER -> monthBuckets(start, now, relevant)

        Period.YEAR -> {
            val elapsed = field(now, Calendar.MONTH) / 3 + 1
            val totals = DoubleArray(elapsed)
            relevant.forEach { (timestamp, amount) ->
                val index = field(timestamp, Calendar.MONTH) / 3
                if (index in totals.indices) totals[index] += amount
            }
            totals.mapIndexed { index, total -> Bucket("Q${index + 1}", total) }
        }

        Period.ALL -> relevant
            .groupBy { field(it.first, Calendar.YEAR) }
            .toSortedMap()
            .map { (year, rows) -> Bucket(year.toString(), rows.sumOf { it.second }) }
    }
}

/** Consecutive months from `start` up to and including the month of `now`, labelled `MMM`. */
private fun monthBuckets(
    start: Long,
    now: Long,
    entries: List<Pair<Long, Double>>
): List<Bucket> {
    val monthLabel = SimpleDateFormat("MMM", Locale.getDefault())
    val cursor = Calendar.getInstance().apply {
        timeInMillis = start
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val last = monthKey(now)

    val totals = entries.groupBy { monthKey(it.first) }
        .mapValues { (_, rows) -> rows.sumOf { it.second } }

    return buildList {
        while (true) {
            val key = cursor.get(Calendar.YEAR) * 12 + cursor.get(Calendar.MONTH)
            if (key > last) break
            add(Bucket(monthLabel.format(Date(cursor.timeInMillis)), totals[key] ?: 0.0))
            cursor.add(Calendar.MONTH, 1)
        }
    }
}

/** Sortable `year * 12 + month`, so consecutive months are consecutive integers across a new year. */
private fun monthKey(timestamp: Long): Int =
    field(timestamp, Calendar.YEAR) * 12 + field(timestamp, Calendar.MONTH)

/** 0-based week of the month: days 1–7 are week 0, 8–14 week 1, and so on. */
private fun weekOfMonth(timestamp: Long): Int = (field(timestamp, Calendar.DAY_OF_MONTH) - 1) / 7

private fun field(timestamp: Long, field: Int): Int =
    Calendar.getInstance().apply { timeInMillis = timestamp }.get(field)
