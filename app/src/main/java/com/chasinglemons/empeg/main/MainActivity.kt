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
import com.chasinglemons.empeg.connect.ConnectScreen
import com.chasinglemons.empeg.connect.ConnectViewModel
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

        val startDestination =
            if (viewModel.getEmpegIp().isNotEmpty()) {
                Screen.Primary.route
            } else {
                Screen.Connect.route
            }

        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()

            EmpegRemoteTheme {
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable(Screen.Connect.route) {
                        val connectViewModel: ConnectViewModel = viewModel(
                            factory = ConnectViewModel.Factory
                        )

                        ConnectScreen(
                            navController = navController,
                            viewModel = connectViewModel,
                            empegIp = viewModel.getEmpegIp()
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
                }
            }
        }
    }
}