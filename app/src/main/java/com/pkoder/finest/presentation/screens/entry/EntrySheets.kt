package com.pkoder.finest.presentation.screens.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pkoder.finest.data.local.entities.CreditEntryEntity
import com.pkoder.finest.data.local.entities.DebitEntryEntity
import com.pkoder.finest.domain.model.TransactionOptions
import com.pkoder.finest.presentation.components.AmountTextField
import com.pkoder.finest.presentation.components.DropdownField
import com.pkoder.finest.presentation.ui.theme.Spacing

/**
 * Add and edit both happen in a bottom sheet now, launched from the FAB or a history row, instead
 * of the old always-on-screen form. The field layout is shared, so the two paths cannot drift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntrySheet(
    onDismiss: () -> Unit,
    onSaveDebit: (DebitEntryEntity) -> Unit,
    onSaveCredit: (CreditEntryEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isExpense by remember { mutableStateOf(true) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        SheetBody(title = "New transaction") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = isExpense,
                    onClick = { isExpense = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Expense") }
                SegmentedButton(
                    selected = !isExpense,
                    onClick = { isExpense = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Income") }
            }

            if (isExpense) {
                DebitFields(entry = null, onSave = onSaveDebit, onCancel = onDismiss)
            } else {
                CreditFields(entry = null, onSave = onSaveCredit, onCancel = onDismiss)
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetBody(title = "Edit expense") {
            DebitFields(entry = entry, onSave = onSave, onCancel = onDismiss)
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetBody(title = "Edit income") {
            CreditFields(entry = entry, onSave = onSave, onCancel = onDismiss)
        }
    }
}

@Composable
private fun SheetBody(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg)
            .padding(bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}

@Composable
private fun DebitFields(
    entry: DebitEntryEntity?,
    onSave: (DebitEntryEntity) -> Unit,
    onCancel: () -> Unit
) {
    var category by remember { mutableStateOf(entry?.category ?: "") }
    var amount by remember { mutableStateOf(entry?.amount?.toPlainInput() ?: "") }
    var paymentMethod by remember { mutableStateOf(entry?.paymentMethod ?: "") }
    var bank by remember { mutableStateOf(entry?.bank ?: "") }
    var description by remember { mutableStateOf(entry?.description ?: "") }
    var showErrors by remember { mutableStateOf(false) }

    val parsedAmount = amount.toDoubleOrNull()
    val amountValid = parsedAmount != null && parsedAmount > 0.0

    DropdownField(
        label = "Category",
        options = TransactionOptions.withCurrent(TransactionOptions.expenseCategories, category),
        selectedOption = category,
        onOptionSelected = { category = it },
        isError = showErrors && category.isBlank()
    )
    AmountTextField(
        label = "Amount",
        value = amount,
        onValueChange = { amount = it },
        isError = showErrors && !amountValid,
        supportingText = if (showErrors && !amountValid) "Enter an amount greater than zero" else null
    )
    DropdownField(
        label = "Payment method",
        options = TransactionOptions.withCurrent(TransactionOptions.paymentMethods, paymentMethod),
        selectedOption = paymentMethod,
        onOptionSelected = { paymentMethod = it },
        isError = showErrors && paymentMethod.isBlank()
    )
    DropdownField(
        label = "Bank",
        options = TransactionOptions.withCurrent(TransactionOptions.banks, bank),
        selectedOption = bank,
        onOptionSelected = { bank = it },
        isError = showErrors && bank.isBlank()
    )
    OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Note (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    SheetActions(
        onCancel = onCancel,
        onSave = {
            val complete = category.isNotBlank() && paymentMethod.isNotBlank() &&
                bank.isNotBlank() && amountValid
            if (!complete) {
                showErrors = true
                return@SheetActions
            }
            val base = entry ?: DebitEntryEntity()
            onSave(
                base.copy(
                    category = category,
                    paymentMethod = paymentMethod,
                    bank = bank,
                    amount = parsedAmount!!,
                    // The column is nullable; storing "" made "has a note" checks lie.
                    description = description.ifBlank { null }
                )
            )
        }
    )
}

@Composable
private fun CreditFields(
    entry: CreditEntryEntity?,
    onSave: (CreditEntryEntity) -> Unit,
    onCancel: () -> Unit
) {
    var source by remember { mutableStateOf(entry?.source ?: "") }
    var amount by remember { mutableStateOf(entry?.amount?.toPlainInput() ?: "") }
    var showErrors by remember { mutableStateOf(false) }

    val parsedAmount = amount.toDoubleOrNull()
    val amountValid = parsedAmount != null && parsedAmount > 0.0

    DropdownField(
        label = "Source",
        options = TransactionOptions.withCurrent(TransactionOptions.incomeSources, source),
        selectedOption = source,
        onOptionSelected = { source = it },
        isError = showErrors && source.isBlank()
    )
    AmountTextField(
        label = "Amount",
        value = amount,
        onValueChange = { amount = it },
        isError = showErrors && !amountValid,
        supportingText = if (showErrors && !amountValid) "Enter an amount greater than zero" else null
    )

    SheetActions(
        onCancel = onCancel,
        onSave = {
            if (source.isBlank() || !amountValid) {
                showErrors = true
                return@SheetActions
            }
            val base = entry ?: CreditEntryEntity()
            onSave(base.copy(source = source, amount = parsedAmount!!))
        }
    )
}

@Composable
private fun SheetActions(onCancel: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onCancel) { Text("Cancel") }
        Button(
            onClick = onSave,
            modifier = Modifier.padding(start = Spacing.sm)
        ) { Text("Save") }
    }
}

/** `250.0` → `"250"`, `250.5` → `"250.5"` — avoids seeding the field with "250.0". */
private fun Double.toPlainInput(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()
