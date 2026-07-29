package com.pkoder.finest.presentation.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pkoder.finest.data.local.entities.CreditEntryEntity
import com.pkoder.finest.data.local.entities.DebitEntryEntity
import com.pkoder.finest.presentation.components.DayHeader
import com.pkoder.finest.presentation.components.DropdownPill
import com.pkoder.finest.presentation.components.EmptyState
import com.pkoder.finest.presentation.components.GlassCard
import com.pkoder.finest.presentation.components.ScreenHeader
import com.pkoder.finest.presentation.components.SectionHeader
import com.pkoder.finest.presentation.components.TransactionRow
import com.pkoder.finest.presentation.components.charts.BarDatum
import com.pkoder.finest.presentation.components.charts.PeriodBarChart
import com.pkoder.finest.presentation.screens.entry.EditCreditSheet
import com.pkoder.finest.presentation.screens.entry.EditDebitSheet
import com.pkoder.finest.presentation.ui.theme.Mono
import com.pkoder.finest.presentation.ui.theme.Spacing
import com.pkoder.finest.presentation.util.Period
import com.pkoder.finest.presentation.util.asMoneyWhole
import com.pkoder.finest.presentation.util.buckets
import com.pkoder.finest.presentation.util.periodStart
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import java.util.Calendar

private enum class TypeFilter(val label: String) {
    ALL("All"), EXPENSE("Expenses"), INCOME("Income")
}

/**
 * Full ledger with search, type/period filters, a spend-by-bucket chart and day grouping.
 *
 * Deletes are deferred: the row disappears immediately but the write happens only once the undo
 * snackbar goes away, so an accidental swipe costs nothing and no id is churned in Firestore.
 */
@Composable
fun HistoryScreen(
    viewModel: FinanceViewModel,
    snackbarHostState: SnackbarHostState
) {
    val debits by viewModel.debits.collectAsState()
    val credits by viewModel.credits.collectAsState()
    // Rows hidden while their undo window is open; owned by the view model so the countdown
    // survives navigating away from this tab.
    val hiddenIds by viewModel.pendingDeletions.collectAsState()

    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(TypeFilter.ALL) }
    var period by remember { mutableStateOf(Period.MONTH) }

    var editingDebit by remember { mutableStateOf<DebitEntryEntity?>(null) }
    var editingCredit by remember { mutableStateOf<CreditEntryEntity?>(null) }

    val start = remember(period) { periodStart(period) }
    val groups = remember(debits, credits, query, typeFilter, start, hiddenIds) {
        buildRows(debits, credits, query, typeFilter, start, hiddenIds)
    }
    // The chart always reflects spending across the whole period, not the search results — it is a
    // frame of reference for the list, so filtering it would make the two disagree.
    val spendBuckets = remember(debits, period, start) {
        period.buckets(debits.filter { it.timestamp >= start }.map { it.timestamp to it.amount })
            .map { BarDatum(it.label, it.total, it.total.asMoneyWhole()) }
    }

    // Shared by the swipe gesture and the delete button.
    fun deleteWithUndo(row: Row) {
        viewModel.deleteWithUndo(row.id, row is Row.Expense) {
            snackbarHostState.showSnackbar(
                message = "Transaction deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            ) == SnackbarResult.ActionPerformed
        }
    }

    editingDebit?.let { entry ->
        EditDebitSheet(
            entry = entry,
            onDismiss = { editingDebit = null },
            onSave = {
                viewModel.updateDebit(it)
                editingDebit = null
            }
        )
    }
    editingCredit?.let { entry ->
        EditCreditSheet(
            entry = entry,
            onDismiss = { editingCredit = null },
            onSave = {
                viewModel.updateCredit(it)
                editingCredit = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize(),
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            top = Spacing.sm,
            // Clears the FAB, which would otherwise sit on top of the last row.
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item {
            ScreenHeader(
                title = "History",
                subtitle = "Your recent financial activity",
                trailing = {
                    DropdownPill(
                        selected = period.label,
                        options = Period.entries.map { it.label },
                        onSelect = { label ->
                            period = Period.entries.first { it.label == label }
                        }
                    )
                }
            )
        }

        item {
            SearchField(
                query = query,
                onQueryChange = { query = it }
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                TypeFilter.entries.forEach { filter ->
                    MonoFilterChip(
                        label = filter.label,
                        selected = typeFilter == filter,
                        onClick = { typeFilter = filter }
                    )
                }
            }
        }

        if (spendBuckets.size > 1) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.card)) {
                        Text(
                            text = "SPENT PER ${spendBuckets.first().bucketNoun()}",
                            style = Mono.labelWide,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PeriodBarChart(
                            data = spendBuckets,
                            chartHeight = 140.dp,
                            modifier = Modifier.padding(top = Spacing.lg)
                        )
                    }
                }
            }
        }

        item { SectionHeader(title = "Transactions") }

        if (groups.isEmpty()) {
            item {
                EmptyState(
                    message = if (query.isBlank()) "Nothing in this period" else "No matches",
                    icon = Icons.AutoMirrored.Filled.List,
                    hint = if (query.isBlank()) "Try a wider date range, or add a transaction."
                    else "Try a different search term.",
                    modifier = Modifier.height(280.dp)
                )
            }
        } else {
            groups.forEach { group ->
                item(key = "header-${group.dayKey}") {
                    DayHeader(timestamp = group.timestamp, total = group.net)
                }
                item(key = "card-${group.dayKey}") {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        group.items.forEachIndexed { index, row ->
                            SwipeToDeleteRow(onDelete = { deleteWithUndo(row) }) {
                                HistoryRow(
                                    row = row,
                                    onEdit = {
                                        when (row) {
                                            is Row.Expense -> editingDebit = row.entry
                                            is Row.Income -> editingCredit = row.entry
                                        }
                                    },
                                    onDelete = { deleteWithUndo(row) }
                                )
                            }
                            if (index != group.items.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant
                                        .copy(alpha = 0.5f),
                                    modifier = Modifier.padding(horizontal = Spacing.lg)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Dark 12dp field with a mint focus ring, per the design's input rule. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Search category, bank or note",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MonoFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, style = Mono.label) },
        shape = MaterialTheme.shapes.large,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

/**
 * Swipe either way to delete. The row also keeps an explicit delete button — swiping is not
 * discoverable and is not reachable with TalkBack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState()

    // The row is removed from the list by the hidden-id filter, so reset the gesture state
    // instead of leaving the box stuck open if the delete is undone.
    LaunchedEffect(state.currentValue) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) {
            onDelete()
            state.reset()
        }
    }

    SwipeToDismissBox(
        state = state,
        // The row content must be opaque: `backgroundContent` is painted underneath at all times,
        // so a transparent row showed the delete tint through even at rest.
        content = {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
                content()
            }
        },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                    .padding(horizontal = Spacing.xl),
                contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Alignment.CenterEnd
                } else {
                    Alignment.CenterStart
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@Composable
private fun HistoryRow(
    row: Row,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    TransactionRow(
        title = row.title,
        subtitle = row.subtitle,
        amount = row.amount,
        timestamp = row.timestamp,
        isExpense = row is Row.Expense,
        synced = row.synced,
        onClick = onEdit,
        trailing = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete transaction",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}

/** `SPENT PER WEEK` / `PER MONTH` — reads the noun off the bucket labels the period produced. */
private fun BarDatum.bucketNoun(): String = when {
    label.startsWith("W") -> "WEEK"
    label.startsWith("Q") -> "QUARTER"
    label.length == 4 && label.all(Char::isDigit) -> "YEAR"
    else -> "MONTH"
}

/** One ledger line, either table. */
private sealed interface Row {
    val id: String
    val title: String

    /**
     * What the row shows. Bank and method only — a free-text note can be any length, and it is a
     * tap away in the edit sheet.
     */
    val subtitle: String?

    /** Everything the search box matches against, which is more than [subtitle] displays. */
    val searchText: String
    val amount: Double
    val timestamp: Long
    val synced: Boolean

    data class Expense(val entry: DebitEntryEntity) : Row {
        override val id = entry.firestoreId
        override val title = entry.category
        override val subtitle = listOfNotNull(
            entry.bank.takeIf(String::isNotBlank),
            entry.paymentMethod.takeIf(String::isNotBlank)
        ).joinToString(" • ").ifBlank { null }
        override val searchText = listOfNotNull(
            entry.category,
            entry.bank,
            entry.paymentMethod,
            entry.description
        ).joinToString(" ").lowercase()
        override val amount = entry.amount
        override val timestamp = entry.timestamp
        override val synced = entry.synced
    }

    data class Income(val entry: CreditEntryEntity) : Row {
        override val id = entry.firestoreId
        override val title = entry.source
        override val subtitle = null
        override val searchText = entry.source.lowercase()
        override val amount = entry.amount
        override val timestamp = entry.timestamp
        override val synced = entry.synced
    }
}

private data class DayGroup(
    val dayKey: String,
    val timestamp: Long,
    val net: Double,
    val items: List<Row>
)

private fun buildRows(
    debits: List<DebitEntryEntity>,
    credits: List<CreditEntryEntity>,
    query: String,
    typeFilter: TypeFilter,
    start: Long,
    hiddenIds: Set<String>
): List<DayGroup> {
    val term = query.trim().lowercase()

    val rows = buildList<Row> {
        if (typeFilter != TypeFilter.INCOME) addAll(debits.map { Row.Expense(it) })
        if (typeFilter != TypeFilter.EXPENSE) addAll(credits.map { Row.Income(it) })
    }
        .filter { it.timestamp >= start }
        .filterNot { it.id in hiddenIds }
        .filter { row -> term.isEmpty() || row.searchText.contains(term) }
        .sortedByDescending { it.timestamp }

    return rows.groupBy { it.timestamp.dayKey() }
        .map { (dayKey, items) ->
            DayGroup(
                dayKey = dayKey,
                timestamp = items.first().timestamp,
                // Net for the day: income minus expenses.
                net = items.sumOf { if (it is Row.Expense) -it.amount else it.amount },
                items = items
            )
        }
}

private fun Long.dayKey(): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = this@dayKey }
    return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
}
