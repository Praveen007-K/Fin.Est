package com.pkoder.finest.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pkoder.finest.domain.model.PendingTransaction
import com.pkoder.finest.domain.model.TransactionType
import com.pkoder.finest.presentation.viewmodel.SmsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReviewScreen(viewModel: SmsViewModel) {
    val pending by viewModel.pendingTransactions.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Debit", "Credit")

    val debits = pending.filter { it.type == TransactionType.DEBIT }
    val credits = pending.filter { it.type == TransactionType.CREDIT }

    Scaffold(
        topBar = {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> ReviewList(
                    items = debits,
                    emptyMessage = "No pending debit transactions",
                    onApprove = { viewModel.approve(it) },
                    onReject = { viewModel.dismiss(it.id) }
                )
                1 -> ReviewList(
                    items = credits,
                    emptyMessage = "No pending credit transactions",
                    onApprove = { viewModel.approve(it) },
                    onReject = { viewModel.dismiss(it.id) }
                )
            }
        }
    }
}

@Composable
fun ReviewList(
    items: List<PendingTransaction>,
    emptyMessage: String,
    onApprove: (PendingTransaction) -> Unit,
    onReject: (PendingTransaction) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState(emptyMessage)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { transaction ->
                PendingTransactionCard(
                    transaction = transaction,
                    onApprove = { onApprove(transaction) },
                    onReject = { onReject(transaction) }
                )
            }
        }
    }
}

@Composable
fun PendingTransactionCard(
    transaction: PendingTransaction,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        .format(Date(transaction.timestamp))

    val isDebit = transaction.type == TransactionType.DEBIT
    val accentColor = if (isDebit) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.primary

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDebit) Icons.Default.ArrowUpward
                    else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isDebit) transaction.category else transaction.source,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${transaction.bank} • ${transaction.paymentMethod}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${if (isDebit) "-" else "+"}₹${"%.2f".format(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor
                )
            }

            if (transaction.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Raw SMS preview
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = transaction.rawSms,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Reject") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onApprove) { Text("Approve") }
            }
        }
    }
}