package com.pkoder.finest.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pkoder.finest.presentation.navigation.BottomNavigationBar
import com.pkoder.finest.presentation.navigation.NavRoutes
import com.pkoder.finest.presentation.navigation.NavigationGraph
import com.pkoder.finest.presentation.screens.entry.AddEntrySheet
import com.pkoder.finest.presentation.ui.theme.WordmarkStyle
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import com.pkoder.finest.presentation.viewmodel.SmsViewModel

/**
 * App shell: one top bar, one bottom bar, one snackbar host and the add-entry FAB, so individual
 * screens are just content. All view models are hoisted here and passed down.
 *
 * The bar carries the `FIN.EST` wordmark on every screen — per-screen titles are part of the content
 * now (History's "History", Review's "Pending Review"), which is how the reference designs stack a
 * large in-page heading under a constant brand bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FinanceViewModel = hiltViewModel(),
    smsViewModel: SmsViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddSheet by remember { mutableStateOf(false) }

    val isSignIn = currentRoute == NavRoutes.SIGN_IN
    // Home offers the quick-action row instead, so the FAB would only duplicate it there.
    val showFab = currentRoute == NavRoutes.HISTORY

    val pending by smsViewModel.pendingTransactions.collectAsState()

    // Arrived from a captured-payment notification: jump to Review — unless the user still has to
    // sign in, in which case the review screen would have nothing to write to.
    val reviewRequest by smsViewModel.reviewRequest.collectAsState()
    LaunchedEffect(reviewRequest, currentRoute) {
        if (reviewRequest != null && currentRoute != null && !isSignIn &&
            currentRoute != NavRoutes.REVIEW
        ) {
            navController.navigate(NavRoutes.REVIEW) { launchSingleTop = true }
        }
    }

    // Sync failures surface here rather than in a Toast, and clear once shown.
    val errorMessage by viewModel.errorMessage.collectAsState()
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeErrorMessage()
        }
    }

    if (showAddSheet) {
        AddEntrySheet(
            onDismiss = { showAddSheet = false },
            onSaveDebit = {
                viewModel.addDebit(it)
                showAddSheet = false
            },
            onSaveCredit = {
                viewModel.addCredit(it)
                showAddSheet = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!isSignIn) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "FIN.EST",
                            style = WordmarkStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.navigate(NavRoutes.ABOUT) { launchSingleTop = true }
                        }) {
                            ProfileAvatar()
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (currentRoute != NavRoutes.REVIEW) {
                                navController.navigate(NavRoutes.REVIEW) { launchSingleTop = true }
                            }
                        }) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = if (pending.isEmpty()) "Review queue"
                                    else "${pending.size} captured payments to review",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (pending.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(8.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (!isSignIn) BottomNavigationBar(navController, smsViewModel)
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add transaction")
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavigationGraph(
                navController = navController,
                financeViewModel = viewModel,
                smsViewModel = smsViewModel,
                snackbarHostState = snackbarHostState,
                onAddEntry = { showAddSheet = true }
            )
        }
    }
}

/** Mint-ringed disc in the top bar; the designs use a photo, we have no avatar URL to load. */
@Composable
private fun ProfileAvatar() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Profile",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}
