package com.example.finest.presentation.screens.tabs

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.finest.data.local.entities.DebitEntryEntity
import com.example.finest.presentation.components.DropdownField
import com.example.finest.presentation.components.AmountTextField
import com.example.finest.presentation.viewmodel.FinanceViewModel

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
    val context = LocalContext.current


    val viewModel: FinanceViewModel = hiltViewModel()

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
            val dataNotEmpty : Boolean = category.isNotEmpty() && amount.isNotBlank() && paymentMethod.isNotEmpty() && bank.isNotEmpty();
            if(dataNotEmpty){
                viewModel.addDebit(
                    DebitEntryEntity(
                        category = category,
                        paymentMethod = paymentMethod,
                        bank = bank,
                        amount = amount.toDouble(),
                        description = description
                    )
                )

                Toast.makeText(context, "Successfully added data.", Toast.LENGTH_SHORT).show()

                //Clear fields after submit
                category = ""
                paymentMethod = ""
                bank = ""
                amount = ""
                description = ""

            }
            else{
                Toast.makeText(context, "Fields cannot be empty!", Toast.LENGTH_SHORT).show()
            }

        }) {
            Text("Submit")
        }
    }
}
