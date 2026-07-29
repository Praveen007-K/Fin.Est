package com.pkoder.finest.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pkoder.finest.auth.data.repository.AuthResult
import com.pkoder.finest.auth.presentation.viewmodel.AuthViewModel
import com.pkoder.finest.presentation.components.ConfirmDialog
import com.pkoder.finest.presentation.ui.theme.Spacing
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel

/** Account, sync state and sign-out. Reached from the avatar in the top bar. */
@Composable
fun AboutScreen(
    authViewModel: AuthViewModel,
    financeViewModel: FinanceViewModel,
    onSignOut: () -> Unit = {}
) {
    val authState by authViewModel.authState.collectAsState()
    val debits by financeViewModel.debits.collectAsState()
    val credits by financeViewModel.credits.collectAsState()
    val isRefreshing by financeViewModel.isRefreshing.collectAsState()
    var confirmSignOut by remember { mutableStateOf(false) }

    val user = (authState as? AuthResult.Success)?.userData
    val unsyncedCount = debits.count { !it.synced } + credits.count { !it.synced }

    if (confirmSignOut) {
        ConfirmDialog(
            title = "Sign out?",
            message = if (unsyncedCount > 0) {
                "$unsyncedCount transaction(s) haven't reached the cloud yet and will be lost. " +
                    "Pull to refresh on the home screen first to upload them."
            } else {
                "Your cloud data stays safe and comes back when you sign in again."
            },
            confirmLabel = "Sign out",
            destructive = true,
            onConfirm = {
                confirmSignOut = false
                onSignOut()
            },
            onDismiss = { confirmSignOut = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (user?.username?.firstOrNull() ?: user?.email?.firstOrNull() ?: '?')
                            .uppercaseChar()
                            .toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.padding(start = Spacing.lg)) {
                    Text(
                        text = user?.username?.takeIf { it.isNotBlank() && it != "null" } ?: "Signed in",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user?.email ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (unsyncedCount == 0) Icons.Default.Cloud else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = when {
                            isRefreshing -> "Syncing…"
                            unsyncedCount == 0 -> "Everything is backed up"
                            else -> "$unsyncedCount waiting to upload"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = Spacing.md)
                    ) 
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
                InfoRow(label = "Transactions", value = "${debits.size + credits.size}")
                InfoRow(label = "Version", value = "1.0")
            }
        }

        OutlinedButton(
            onClick = { confirmSignOut = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(text = "Sign out", modifier = Modifier.padding(start = Spacing.sm))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
