package com.example.finest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.finest.presentation.screens.HomeScreen
import com.example.finest.presentation.screens.MainScreen
import com.example.finest.presentation.ui.theme.FinEstTheme
import com.example.finest.presentation.viewmodel.FinanceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinEstTheme {
                MainScreen(viewModel)
            }
        }
    }
}

