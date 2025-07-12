package com.chasinglemons.empeg.app

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.chasinglemons.empeg.discovery.DiscoveryViewModel
import com.chasinglemons.empeg.discovery.DiscoveryScreen
import com.chasinglemons.empeg.remote.RemoteScreen
import com.chasinglemons.empeg.remote.RemoteViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier,
    startDestination: String,
    snackbarHostState: SnackbarHostState
) {

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("discovery") {
            val discoveryViewModel: DiscoveryViewModel = viewModel(
                factory = DiscoveryViewModel.Factory
            )

            DiscoveryScreen(
                viewModel = discoveryViewModel,
                onPlayerSet = {
                    navController.popBackStack()
                    navController.navigate("remote")
                              },
                snackbarHostState = snackbarHostState
            )
        }

        composable("remote") {
            val remoteViewmodel: RemoteViewModel = viewModel(
                factory = RemoteViewModel.Factory
            )

            RemoteScreen(
                viewModel = remoteViewmodel,
                snackbarHostState = snackbarHostState
            )
        }
        // composable("playlists")
    }
}
