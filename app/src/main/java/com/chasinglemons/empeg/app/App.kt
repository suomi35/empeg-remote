package com.chasinglemons.empeg.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.chasinglemons.empeg.phone.ui.theme.EmpegRemoteTheme

@Composable
fun App(from: String?) {
    val navController = rememberNavController()

    EmpegRemoteTheme {
        AppScaffold(
            from = from,
            navController = navController
        )
    }
}