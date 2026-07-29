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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.pkoder.finest.presentation.components.EmptyState
import com.pkoder.finest.presentation.components.TransactionRow
import com.pkoder.finest.presentation.screens.entry.EditCreditSheet
import com.pkoder.finest.presentation.screens.entry.EditDebitSheet
import com.pkoder.finest.presentation.ui.theme.Spacing
import com.pkoder.finest.presentation.util.Period
import com.pkoder.finest.presentation.util.periodStart
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import java.util.Calendar

private enum class TypeFilter(val label: String) {
    ALL("All"), EXPENSE("Expenses"), INCOME("Income")
}

/**
 * Full ledger with search, type/period filters and day grouping.
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
    val rows = remember(debits, credits, query, typeFilter, start, hiddenIds) {
        buildRows(debits, credits, query, typeFilter, start, hiddenIds)
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

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search category, bank or note") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen, vertical = Spacing.sm)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            TypeFilter.entries.forEach { filter ->
                FilterChip(
                    selected = typeFilter == filter,
                    onClick = { typeFilter = filter },
                    label = { Text(filter.label) }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Period.entries.forEach { option ->
                FilterChip(
                    selected = period == option,
                    onClick = { period = option },
                    label = { Text(option.label) }
                )
            }
        }

        if (rows.isEmpty()) {
            EmptyState(
                message = if (query.isBlank()) "Nothing in this period" else "No matches",
                icon = Icons.AutoMirrored.Filled.List,
                hint = if (query.isBlank()) "Try a wider date range, or add a transaction."
                else "Try a different search term."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(),
                contentPadding = PaddingValues(bottom = Spacing.xxl)
            ) {
                rows.forEach { group ->
                    item(key = "header-${group.dayKey}") {
                        DayHeader(timestamp = group.timestamp, total = group.net)
                    }
                    items(group.items, key = { it.id }) { row ->
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
                        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
                    }
                }
            }
        }
    }
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
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
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
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = { content() }
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
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}

/** One ledger line, either table. */
private sealed interface Row {
    val id: String
    val title: String
    val subtitle: String?
    val amount: Double
    val timestamp: Long
    val synced: Boolean

    data class Expense(val entry: DebitEntryEntity) : Row {
        override val id = entry.firestoreId
        override val title = entry.category
        override val subtitle = listOfNotNull(
            entry.bank.takeIf(String::isNotBlank),
            entry.paymentMethod.takeIf(String::isNotBlank),
            entry.description?.takeIf(String::isNotBlank)
        ).joinToString(" · ").ifBlank { null }
        override val amount = entry.amount
        override val timestamp = entry.timestamp
        override val synced = entry.synced
    }

    data class Income(val entry: CreditEntryEntity) : Row {
        override val id = entry.firestoreId
        override val title = entry.source
        override val subtitle = null
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
        .filter { row ->
            term.isEmpty() ||
                row.title.lowercase().contains(term) ||
                row.subtitle?.lowercase()?.contains(term) == true
        }
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
