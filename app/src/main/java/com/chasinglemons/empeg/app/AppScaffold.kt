package com.chasinglemons.empeg.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(from: String?, navController: NavHostController) {
    val viewModel: AppViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val showTopBar by viewModel.showTopBar.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val startDestination =
        if (viewModel.isEmpegConfigured()) {
            println(">>> navigating to REMOTE...")
            "remote"
        } else {
            println(">>> no player in prefs, navigating to DISCOVERY...")
            "discovery"
        }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
//        topBar = { AppTopBar(scrollBehavior, visible = showTopBar) }, // Don't really need a topbar for now, but keeping it in case, hiding in AppViewModel
//        bottomBar = {
//            AppBottomBar(
//                navController = navController,
//                visible = showBottomBar,
//                unfollowersListState = unfollowersListState,
//                ignoreListState = ignoreListState
//            )
//        },
//        floatingActionButton = fab,
//        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        content = {
            AppNavigation(
                navController = navController,
                modifier = Modifier.padding(it),
                startDestination = startDestination,
                snackbarHostState = snackbarHostState
            )
        }
    )
}