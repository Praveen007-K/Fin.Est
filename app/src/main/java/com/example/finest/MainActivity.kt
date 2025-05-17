package com.example.finest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.finest.presentation.screens.HomeScreen
import com.example.finest.presentation.screens.MainScreen
import com.example.finest.presentation.ui.theme.FinEstTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinEstTheme {
                MainScreen()
            }
        }
    }
}

