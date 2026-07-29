package com.pkoder.finest.presentation.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pkoder.finest.auth.data.repository.AuthResult
import com.pkoder.finest.auth.presentation.viewmodel.AuthViewModel
import com.pkoder.finest.presentation.components.ConfirmDialog
import com.pkoder.finest.presentation.components.GhostPillButton
import com.pkoder.finest.presentation.components.GlassCard
import com.pkoder.finest.presentation.components.MonoChip
import com.pkoder.finest.presentation.components.TonalIcon
import com.pkoder.finest.presentation.ui.theme.Charcoal
import com.pkoder.finest.presentation.ui.theme.Mono
import com.pkoder.finest.presentation.ui.theme.Spacing
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel

/** Account, sync state and sign-out. Reached from the avatar in the top bar. */
@Composable
fun AboutScreen(
    authViewModel: AuthViewModel,
    financeViewModel: FinanceViewModel,
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val debits by financeViewModel.debits.collectAsState()
    val credits by financeViewModel.credits.collectAsState()
    val isRefreshing by financeViewModel.isRefreshing.collectAsState()
    var confirmSignOut by remember { mutableStateOf(false) }

    val user = (authState as? AuthResult.Success)?.userData
    val total = debits.size + credits.size
    val unsyncedCount = debits.count { !it.synced } + credits.count { !it.synced }

    if (confirmSignOut) {
        ConfirmDialog(
            title = "Sign out?",
            message = if (unsyncedCount > 0) {
                "$unsyncedCount transaction(s) haven't reached the cloud yet and will be lost. " +
                    "Force a sync first to upload them."
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen)
            .padding(bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        IdentityCard(
            name = user?.username?.takeIf { it.isNotBlank() && it != "null" } ?: "Signed in",
            email = user?.email ?: "—",
            initial = (user?.username?.firstOrNull() ?: user?.email?.firstOrNull() ?: '?')
                .uppercaseChar(),
            unsyncedCount = unsyncedCount
        )

        SyncCard(
            isRefreshing = isRefreshing,
            total = total,
            unsyncedCount = unsyncedCount,
            onForceSync = financeViewModel::refresh
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = Spacing.lg)) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = Spacing.card)
                )
                SettingRow(
                    icon = Icons.Default.Notifications,
                    tint = Charcoal.tertiaryFixed,
                    label = "Notifications",
                    value = "System settings",
                    onClick = { context.openNotificationSettings() }
                )
                SettingRow(
                    icon = Icons.Default.AccountBalance,
                    tint = Charcoal.primaryFixedDim,
                    label = "Banks recognised",
                    value = "SBI · HDFC · BOB"
                )
                SettingRow(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    tint = Charcoal.secondary,
                    label = "Transactions",
                    value = "$total"
                )
                SettingRow(
                    icon = Icons.Default.Info,
                    tint = MaterialTheme.colorScheme.primary,
                    label = "Version",
                    value = context.versionName()
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            contentAlignment = Alignment.Center
        ) {
            GhostPillButton(
                text = "SIGN OUT",
                onClick = { confirmSignOut = true },
                icon = Icons.AutoMirrored.Filled.Logout,
                contentColor = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun IdentityCard(
    name: String,
    email: String,
    initial: Char,
    unsyncedCount: Int
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.card),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Spacing.lg)
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Spacing.xs)
            )
            Row(
                modifier = Modifier.padding(top = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                MonoChip(text = "GOOGLE ACCOUNT", color = MaterialTheme.colorScheme.primary)
                MonoChip(
                    text = if (unsyncedCount == 0) "SYNCED" else "$unsyncedCount PENDING",
                    color = if (unsyncedCount == 0) MaterialTheme.colorScheme.primary
                    else Charcoal.tertiaryFixed
                )
            }
        }
    }
}

/**
 * Sync state and a manual trigger.
 *
 * The bar shows the share of rows that have reached Firestore — the design's "last synced" line has
 * no equivalent here because no sync timestamp is persisted, so it reports what is actually known.
 */
@Composable
private fun SyncCard(
    isRefreshing: Boolean,
    total: Int,
    unsyncedCount: Int,
    onForceSync: () -> Unit
) {
    val syncedFraction = if (total == 0) 1f else (total - unsyncedCount).toFloat() / total
    val allSynced = unsyncedCount == 0

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.card)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TonalIcon(
                    icon = if (allSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    tint = if (allSynced) MaterialTheme.colorScheme.primary
                    else Charcoal.tertiaryFixed,
                    size = 40.dp
                )
                Text(
                    text = "Cloud sync",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = Spacing.lg)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "STATUS",
                    style = Mono.labelWide,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when {
                        isRefreshing -> "Syncing…"
                        allSynced -> "Backed up"
                        else -> "$unsyncedCount to upload"
                    },
                    style = Mono.amountSmall,
                    color = if (allSynced) MaterialTheme.colorScheme.primary
                    else Charcoal.tertiaryFixed
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.md)
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(syncedFraction.coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }

            GhostPillButton(
                text = "FORCE SYNC NOW",
                onClick = onForceSync,
                enabled = !isRefreshing,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg)
            )
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.card, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TonalIcon(icon = icon, tint = tint, size = 36.dp)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.lg)
        )
        Text(
            text = value,
            style = Mono.label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = Spacing.xs)
                    .size(18.dp)
            )
        }
    }
}

/**
 * Opens this app's notification settings.
 *
 * The channel is created in `App`, so the switch the user needs is the system one — there is nothing
 * in-app to toggle, and inventing a preference that writes nowhere would be worse than linking out.
 */
private fun Context.openNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }.onFailure {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.fromParts("package", packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Read from the installed package rather than hard-coded, so it cannot drift from the build. */
private fun Context.versionName(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }
        .getOrNull()
        ?: "—"
