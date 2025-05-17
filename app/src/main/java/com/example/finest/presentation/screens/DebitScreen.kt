package com.example.finest.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.finest.presentation.components.DropdownField
import com.example.finest.presentation.components.AmountTextField

@Composable
fun DebitScreen() {
    var category by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val categories = listOf("Housing", "Food", "Transport", "Utilities", "Dependents", "Entertainment", "Health", "Finance")
    val paymentMethods = listOf("UPI", "Cash", "Card", "Net Banking")
    val banks = listOf("SBI", "BOB", "HDFC")

    Column(Modifier.padding(16.dp)) {
        DropdownField("Expense Category", categories, category) { category = it }

        Spacer(Modifier.height(8.dp))

        AmountTextField("Amount", amount) { amount = it }

        Spacer(Modifier.height(8.dp))

        DropdownField("Payment Method", paymentMethods, paymentMethod) { paymentMethod = it }

        Spacer(Modifier.height(8.dp))

        DropdownField("Bank", banks, bank) { bank = it }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            // Save data
        }) {
            Text("Submit")
        }
    }
}
