package com.pkoder.finest.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pkoder.finest.domain.model.PendingTransaction
import com.pkoder.finest.domain.model.TransactionOptions
import com.pkoder.finest.domain.model.TransactionType
import com.pkoder.finest.presentation.components.AmountTextField
import com.pkoder.finest.presentation.components.ConfirmDialog
import com.pkoder.finest.presentation.components.DropdownField
import com.pkoder.finest.presentation.components.EmptyState
import com.pkoder.finest.presentation.ui.theme.Spacing
import com.pkoder.finest.presentation.ui.theme.moneyColors
import com.pkoder.finest.presentation.util.asDateTime
import com.pkoder.finest.presentation.util.asSignedMoney
import com.pkoder.finest.presentation.viewmodel.SmsViewModel

/**
 * The queue of transactions captured from bank SMS.
 *
 * A card can be approved as-is, or opened to correct what the parser guessed — which is exactly
 * where a notification tap lands, with the sheet already open.
 */
@Composable
fun ReviewScreen(viewModel: SmsViewModel) {
    val pending by viewModel.pendingTransactions.collectAsState()
    val highlightId by viewModel.reviewRequest.collectAsState()

    var editing by remember { mutableStateOf<PendingTransaction?>(null) }
    var confirmRejectAll by remember { mutableStateOf(false) }

    // Arriving from a notification: open that transaction straight away.
    LaunchedEffect(highlightId, pending) {
        val target = highlightId?.let { id -> pending.firstOrNull { it.id == id } }
        if (target != null && editing == null) editing = target
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.consumeReviewRequest() }
    }

    editing?.let { transaction ->
        PendingDetailSheet(
            transaction = transaction,
            onDismiss = { editing = null },
            onSaveDraft = {
                viewModel.update(it)
                editing = null
            },
            onApprove = {
                viewModel.approve(it)
                editing = null
            },
            onReject = {
                viewModel.dismiss(it.id)
                editing = null
            }
        )
    }

    if (confirmRejectAll) {
        ConfirmDialog(
            title = "Reject all?",
            message = "All ${pending.size} captured transactions will be discarded. This can't be undone.",
            confirmLabel = "Reject all",
            destructive = true,
            onConfirm = {
                viewModel.dismissAll()
                confirmRejectAll = false
            },
            onDismiss = { confirmRejectAll = false }
        )
    }

    if (pending.isEmpty()) {
        EmptyState(
            message = "Nothing to review",
            icon = Icons.Default.Notifications,
            hint = "Payment SMS from SBI, HDFC and BOB show up here automatically."
        )
        return
    }

    val listState = rememberLazyListState()
    LaunchedEffect(highlightId, pending) {
        val index = pending.indexOfFirst { it.id == highlightId }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (pending.size == 1) "1 transaction" else "${pending.size} transactions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { confirmRejectAll = true }) { Text("Reject all") }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.screen,
                end = Spacing.screen,
                bottom = Spacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            items(pending, key = { it.id }) { transaction ->
                PendingTransactionCard(
                    transaction = transaction,
                    highlighted = transaction.id == highlightId,
                    onOpen = { editing = transaction },
                    onApprove = { viewModel.approve(transaction) },
                    onReject = { viewModel.dismiss(transaction.id) }
                )
            }
        }
    }
}

@Composable
private fun PendingTransactionCard(
    transaction: PendingTransaction,
    highlighted: Boolean,
    onOpen: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isDebit = transaction.type == TransactionType.DEBIT
    val colors = moneyColors
    val accent = if (isDebit) colors.expense else colors.income
    var showRawSms by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "highlight"
    )

    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(if (highlighted) 2.dp else 1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDebit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (isDebit) "Expense" else "Income",
                        tint = accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isDebit) transaction.category.ifBlank { "Uncategorized" }
                        else transaction.source.ifBlank { transaction.bank },
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${transaction.bank} · ${transaction.paymentMethod}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = transaction.amount.asSignedMoney(isDebit),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent
                )
            }

            if (transaction.description.isNotBlank()) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Spacing.sm)
                )
            }

            Text(
                text = transaction.timestamp.asDateTime(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs)
            )

            TextButton(
                onClick = { showRawSms = !showRawSms },
                contentPadding = PaddingValues(vertical = Spacing.xs, horizontal = 0.dp)
            ) {
                Text(if (showRawSms) "Hide message" else "Show message")
            }
            AnimatedVisibility(visible = showRawSms) {
                Text(
                    text = transaction.rawSms,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.md),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onReject) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Reject")
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Button(onClick = onApprove) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Approve")
                }
            }
        }
    }
}

/** Correct what the parser guessed, then approve — or just save the edit for later. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingDetailSheet(
    transaction: PendingTransaction,
    onDismiss: () -> Unit,
    onSaveDraft: (PendingTransaction) -> Unit,
    onApprove: (PendingTransaction) -> Unit,
    onReject: (PendingTransaction) -> Unit
) {
    val isDebit = transaction.type == TransactionType.DEBIT

    var category by remember(transaction.id) { mutableStateOf(transaction.category) }
    var source by remember(transaction.id) { mutableStateOf(transaction.source.ifBlank { transaction.bank }) }
    var paymentMethod by remember(transaction.id) { mutableStateOf(transaction.paymentMethod) }
    var bank by remember(transaction.id) { mutableStateOf(transaction.bank) }
    var amount by remember(transaction.id) {
        mutableStateOf(
            if (transaction.amount % 1.0 == 0.0) transaction.amount.toLong().toString()
            else transaction.amount.toString()
        )
    }
    var description by remember(transaction.id) { mutableStateOf(transaction.description) }

    val parsedAmount = amount.toDoubleOrNull()
    val amountValid = parsedAmount != null && parsedAmount > 0.0

    fun edited() = transaction.copy(
        category = category,
        source = source,
        paymentMethod = paymentMethod,
        bank = bank,
        amount = parsedAmount ?: transaction.amount,
        description = description
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = if (isDebit) "Review expense" else "Review income",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = transaction.rawSms,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AmountTextField(
                label = "Amount",
                value = amount,
                onValueChange = { amount = it },
                isError = !amountValid,
                supportingText = if (!amountValid) "Enter an amount greater than zero" else null
            )

            if (isDebit) {
                DropdownField(
                    label = "Category",
                    options = TransactionOptions.withCurrent(TransactionOptions.expenseCategories, category),
                    selectedOption = category,
                    onOptionSelected = { category = it }
                )
                DropdownField(
                    label = "Payment method",
                    options = TransactionOptions.withCurrent(TransactionOptions.paymentMethods, paymentMethod),
                    selectedOption = paymentMethod,
                    onOptionSelected = { paymentMethod = it }
                )
                DropdownField(
                    label = "Bank",
                    options = TransactionOptions.withCurrent(TransactionOptions.banks, bank),
                    selectedOption = bank,
                    onOptionSelected = { bank = it }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                DropdownField(
                    label = "Source",
                    options = TransactionOptions.withCurrent(TransactionOptions.incomeSources, source),
                    selectedOption = source,
                    onOptionSelected = { source = it }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Button(
                onClick = { if (amountValid) onApprove(edited()) },
                enabled = amountValid,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Approve") }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedButton(
                    onClick = { onSaveDraft(edited()) },
                    enabled = amountValid,
                    modifier = Modifier.weight(1f)
                ) { Text("Save for later") }
                OutlinedButton(
                    onClick = { onReject(transaction) },
                    modifier = Modifier.weight(1f)
                ) { Text("Reject") }
            }
        }
    }
}
