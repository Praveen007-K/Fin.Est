package com.pkoder.finest.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pkoder.finest.domain.model.PendingTransaction
import com.pkoder.finest.domain.model.TransactionOptions
import com.pkoder.finest.domain.model.TransactionType
import com.pkoder.finest.presentation.components.AmountTextField
import com.pkoder.finest.presentation.components.ConfirmDialog
import com.pkoder.finest.presentation.components.DropdownField
import com.pkoder.finest.presentation.components.EmptyState
import com.pkoder.finest.presentation.components.GhostPillButton
import com.pkoder.finest.presentation.components.GlassCard
import com.pkoder.finest.presentation.components.MintPillButton
import com.pkoder.finest.presentation.components.ScreenHeader
import com.pkoder.finest.presentation.components.TonalIcon
import com.pkoder.finest.presentation.components.categoryVisual
import com.pkoder.finest.presentation.ui.theme.Mono
import com.pkoder.finest.presentation.ui.theme.Spacing
import com.pkoder.finest.presentation.util.asDate
import com.pkoder.finest.presentation.util.asSignedMoney
import com.pkoder.finest.presentation.util.asTime
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
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm)) {
                ScreenHeader(
                    title = "Pending Review",
                    subtitle = "Nothing needs your attention"
                )
            }
            EmptyState(
                message = "Nothing to review",
                icon = Icons.Default.RateReview,
                hint = "Payment SMS from SBI, HDFC and BOB show up here automatically."
            )
        }
        return
    }

    val listState = rememberLazyListState()
    LaunchedEffect(highlightId, pending) {
        val index = pending.indexOfFirst { it.id == highlightId }
        // +1 for the header item that precedes the cards.
        if (index >= 0) listState.animateScrollToItem(index + 1)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            top = Spacing.sm,
            bottom = Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item {
            // "Reject all" gets its own line: beside the subtitle it forced the sentence to wrap.
            Column(modifier = Modifier.fillMaxWidth()) {
                ScreenHeader(
                    title = "Pending Review",
                    subtitle = if (pending.size == 1) "1 transaction requires your attention"
                    else "${pending.size} transactions require your attention"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { confirmRejectAll = true }) {
                        Text(
                            text = "REJECT ALL",
                            style = Mono.label,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

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

@Composable
private fun PendingTransactionCard(
    transaction: PendingTransaction,
    highlighted: Boolean,
    onOpen: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isDebit = transaction.type == TransactionType.DEBIT
    val title = if (isDebit) transaction.category.ifBlank { "Uncategorized" }
    else transaction.source.ifBlank { transaction.bank }
    val visual = categoryVisual(title, isDebit)
    var showRawSms by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (highlighted) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        label = "highlight"
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = borderColor,
        borderWidth = if (highlighted) 2.dp else 1.dp,
        onClick = onOpen
    ) {
        Column(modifier = Modifier.padding(Spacing.card)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TonalIcon(icon = visual.icon, tint = visual.tint, size = 44.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.lg)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = (if (isDebit) "EXPENSE" else "INCOME") + " · CAPTURED",
                        style = Mono.label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = transaction.amount.asSignedMoney(isDebit),
                    style = Mono.amount,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = Spacing.lg)
            )

            DetailRow(label = "Date", value = transaction.timestamp.asDate())
            DetailRow(label = "Time", value = transaction.timestamp.asTime())
            DetailRow(label = "Bank", value = transaction.bank)
            if (isDebit) DetailRow(label = "Method", value = transaction.paymentMethod)
            if (transaction.description.isNotBlank()) {
                DetailRow(label = "Details", value = transaction.description)
            }

            TextButton(
                onClick = { showRawSms = !showRawSms },
                contentPadding = PaddingValues(vertical = Spacing.xs, horizontal = 0.dp)
            ) {
                Text(
                    text = if (showRawSms) "HIDE MESSAGE" else "SHOW MESSAGE",
                    style = Mono.label,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = showRawSms) {
                Text(
                    text = transaction.rawSms,
                    style = Mono.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                GhostPillButton(
                    text = "REJECT",
                    onClick = onReject,
                    icon = Icons.Default.Close,
                    contentColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                MintPillButton(
                    text = "APPROVE",
                    onClick = onApprove,
                    icon = Icons.Default.Check,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Label on the left, value right-aligned in tabular figures — the design's detail table. */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = Mono.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = Spacing.lg)
        )
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.card)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = if (isDebit) "Review expense" else "Review income",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = transaction.rawSms,
                style = Mono.body,
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
                    shape = MaterialTheme.shapes.medium,
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

            MintPillButton(
                text = "APPROVE",
                onClick = { if (amountValid) onApprove(edited()) },
                enabled = amountValid,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                GhostPillButton(
                    text = "SAVE FOR LATER",
                    onClick = { onSaveDraft(edited()) },
                    enabled = amountValid,
                    modifier = Modifier.weight(1f)
                )
                GhostPillButton(
                    text = "REJECT",
                    onClick = { onReject(transaction) },
                    contentColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
