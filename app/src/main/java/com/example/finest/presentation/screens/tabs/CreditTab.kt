package com.example.finest.presentation.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.finest.presentation.components.AmountTextField
import com.example.finest.presentation.components.DropdownField

@Composable
fun CreditScreen() {
    var incomeSource by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    val incomeSources = listOf("Salary", "Freelance", "Gift", "Other")

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
            // Save data
        }) {
            Text("Submit")
        }
    }
}
