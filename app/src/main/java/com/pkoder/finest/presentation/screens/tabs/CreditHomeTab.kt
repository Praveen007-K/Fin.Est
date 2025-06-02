package com.pkoder.finest.presentation.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pkoder.finest.data.local.entities.CreditEntryEntity
import com.pkoder.finest.presentation.components.AmountTextField
import com.pkoder.finest.presentation.components.DropdownField
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel

@Composable
fun CreditScreen() {
    var incomeSource by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    val incomeSources = listOf("Salary", "Freelance", "Gift", "Other")
    val viewModel: FinanceViewModel = hiltViewModel()

    Column(Modifier.padding(16.dp)) {
        DropdownField("Income Source", incomeSources, incomeSource) {
            incomeSource = it
        }

        Spacer(Modifier.height(8.dp))

        AmountTextField("Amount", amount) {
            amount = it
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            viewModel.addCredit(
                CreditEntryEntity(
                    source = incomeSource,
                    amount = amount.toDouble()
                )
            )
        }) {
            Text("Submit")
        }
    }
}
