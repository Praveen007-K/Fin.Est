package com.example.finest.presentation.screens.details

import android.widget.Button
import android.widget.Spinner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun Details(){
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = { expanded = !expanded }) {
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // First section
            DropdownMenuItem(
                text = { Text("Profile") },
                onClick = { expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = { expanded = false }
            )

            HorizontalDivider()

            // Second section
            DropdownMenuItem(
                text = { Text("Send Feedback") },
                onClick = { expanded = false }
            )

            HorizontalDivider()

            // Third section
            DropdownMenuItem(
                text = { Text("About") },
                onClick = { expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Help") },
                onClick = { expanded = false }
            )
        }
    }
}