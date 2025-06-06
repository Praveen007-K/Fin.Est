package com.pkoder.finest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.pkoder.finest.presentation.screens.MainScreen
import com.pkoder.finest.presentation.ui.theme.FinEstTheme
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels()
    val db = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinEstTheme {
                MainScreen(viewModel)
            }
        }
    }
}

