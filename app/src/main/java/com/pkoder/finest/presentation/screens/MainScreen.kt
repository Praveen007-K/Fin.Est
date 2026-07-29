package com.pkoder.finest.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pkoder.finest.presentation.navigation.BottomNavigationBar
import com.pkoder.finest.presentation.navigation.NavRoutes
import com.pkoder.finest.presentation.navigation.NavigationGraph
import com.pkoder.finest.presentation.screens.entry.AddEntrySheet
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import com.pkoder.finest.presentation.viewmodel.SmsViewModel

/**
 * App shell: one top bar, one bottom bar, one snackbar host and the add-entry FAB, so individual
 * screens are just content. All view models are hoisted here and passed down.
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
    val showFab = currentRoute == NavRoutes.HOME || currentRoute == NavRoutes.HISTORY

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
        topBar = {
            if (!isSignIn) {
                CenterAlignedTopAppBar(
                    title = { Text(titleFor(currentRoute)) },
                    actions = {
                        if (currentRoute != NavRoutes.ABOUT) {
                            IconButton(onClick = {
                                navController.navigate(NavRoutes.ABOUT) { launchSingleTop = true }
                            }) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
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
                FloatingActionButton(onClick = { showAddSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add transaction")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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

private fun titleFor(route: String?): String = when (route) {
    NavRoutes.HISTORY -> "History"
    NavRoutes.STATS -> "Insights"
    NavRoutes.REVIEW -> "Review"
    NavRoutes.ABOUT -> "Profile"
    else -> "Fin.Est"
}
