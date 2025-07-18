package com.chasinglemons.empeg.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chasinglemons.empeg.discovery.DiscoveryScreen
import com.chasinglemons.empeg.discovery.DiscoveryViewModel
import com.chasinglemons.empeg.navigation.Screen
import com.chasinglemons.empeg.remote.PrimaryScreen
import com.chasinglemons.empeg.remote.PrimaryScreenViewModel
import com.chasinglemons.empeg.ui.theme.EmpegRemoteTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : KoinComponent, ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: MainViewModel by inject()

        val from = intent.getStringExtra("from")
        println(">>> EmpegActivity: from = $from")

        val startDestination =
            if (viewModel.isEmpegConfigured()) {
                println(">>> navigating to REMOTE...")
                Screen.Primary.route
            } else {
                println(">>> no player in prefs, navigating to DISCOVERY...")
                Screen.Discovery.route
            }

        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()

            EmpegRemoteTheme {
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable(Screen.Discovery.route) {
                        val discoveryViewModel: DiscoveryViewModel = viewModel(
                            factory = DiscoveryViewModel.Factory
                        )

                        DiscoveryScreen(
                            navController = navController,
                            viewModel = discoveryViewModel
                        )
                    }

                    composable(Screen.Primary.route) {
                        val primaryScreenViewmodel: PrimaryScreenViewModel = viewModel(
                            factory = PrimaryScreenViewModel.Factory
                        )

                        PrimaryScreen(
                            navController = navController,
                            windowSizeClass = calculateWindowSizeClass(this@MainActivity),
                            viewModel = primaryScreenViewmodel
                        )
                    }

                    // composable("playlists")

                    composable(Screen.Settings.route) {
//                        val settingsViewmodel: SettingsViewModel = viewModel(
//                            factory = SettingsViewModel.Factory
//                        )
//
//                        SettingsScreen(
//                            navController = navController,
//                            viewModel = settingsViewmodel
//                        )
                    }
                }
            }
        }
    }
}