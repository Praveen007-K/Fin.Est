package com.pkoder.finest.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pkoder.finest.data.local.entities.CreditEntryEntity
import com.pkoder.finest.data.local.entities.DebitEntryEntity
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen() {
    val viewModel: FinanceViewModel = hiltViewModel()
    val debits by viewModel.debits.collectAsState()
    val credits by viewModel.credits.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Debit", "Credit")

    var editingDebit by remember { mutableStateOf<DebitEntryEntity?>(null) }
    var editingCredit by remember { mutableStateOf<CreditEntryEntity?>(null) }
    var deletingDebitId by remember { mutableStateOf<String?>(null) }
    var deletingCreditId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAllEntries()
    }

    // Delete confirmation dialogs
    deletingDebitId?.let { id ->
        DeleteConfirmDialog(
            onConfirm = {
                viewModel.deleteDebit(id)
                deletingDebitId = null
            },
            onDismiss = { deletingDebitId = null }
        )
    }

    deletingCreditId?.let { id ->
        DeleteConfirmDialog(
            onConfirm = {
                viewModel.deleteCredit(id)
                deletingCreditId = null
            },
            onDismiss = { deletingCreditId = null }
        )
    }

    // Edit bottom sheets
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
                0 -> {
                    if (debits.isEmpty()) {
                        EmptyState("No debit entries yet")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(debits.sortedByDescending { it.timestamp }) { debit ->
                                DebitHistoryCard(
                                    entry = debit,
                                    onEdit = { editingDebit = debit },
                                    onDelete = { deletingDebitId = debit.firestoreId }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (credits.isEmpty()) {
                        EmptyState("No credit entries yet")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(credits.sortedByDescending { it.timestamp }) { credit ->
                                CreditHistoryCard(
                                    entry = credit,
                                    onEdit = { editingCredit = credit },
                                    onDelete = { deletingCreditId = credit.firestoreId }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DebitHistoryCard(
    entry: DebitEntryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entry.timestamp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "Debit",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(entry.category, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${entry.bank} • ${entry.paymentMethod}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!entry.description.isNullOrEmpty()) {
                    Text(
                        entry.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "-₹${"%.2f".format(entry.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )

            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreditHistoryCard(
    entry: CreditEntryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entry.timestamp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "Credit",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(entry.source, style = MaterialTheme.typography.titleSmall)
                Text(
                    date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "+₹${"%.2f".format(entry.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDebitSheet(
    entry: DebitEntryEntity,
    onDismiss: () -> Unit,
    onSave: (DebitEntryEntity) -> Unit
) {
    var amount by remember { mutableStateOf(entry.amount.toString()) }
    var category by remember { mutableStateOf(entry.category) }
    var paymentMethod by remember { mutableStateOf(entry.paymentMethod) }
    var bank by remember { mutableStateOf(entry.bank) }
    var description by remember { mutableStateOf(entry.description ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Edit Debit Entry", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = { Text("Payment Method") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bank,
                onValueChange = { bank = it },
                label = { Text("Bank") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    onSave(
                        entry.copy(
                            amount = amount.toDoubleOrNull() ?: entry.amount,
                            category = category,
                            paymentMethod = paymentMethod,
                            bank = bank,
                            description = description.ifBlank { null }
                        )
                    )
                }) { Text("Save") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCreditSheet(
    entry: CreditEntryEntity,
    onDismiss: () -> Unit,
    onSave: (CreditEntryEntity) -> Unit
) {
    var amount by remember { mutableStateOf(entry.amount.toString()) }
    var source by remember { mutableStateOf(entry.source) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Edit Credit Entry", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = source,
                onValueChange = { source = it },
                label = { Text("Source") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    onSave(
                        entry.copy(
                            source = source,
                            amount = amount.toDoubleOrNull() ?: entry.amount
                        )
                    )
                }) { Text("Save") }
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Entry") },
        text = { Text("Are you sure you want to delete this entry? This cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}