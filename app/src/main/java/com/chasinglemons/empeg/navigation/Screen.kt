package com.chasinglemons.empeg.navigation

import androidx.navigation.NavController

sealed class Screen(val route: String) {
    data object Primary : Screen(route = "primary")
    data object Connect : Screen(route = "connect")
}

fun NavController.navigateToPrimaryAfterConnect() {
    if (!popBackStack(Screen.Primary.route, inclusive = false)) {
        navigate(Screen.Primary.route) {
            popUpTo(Screen.Connect.route) { inclusive = true }
            launchSingleTop = true
        }
    }
}

fun NavController.navigateToConnect() {
    navigate(Screen.Connect.route) {
        launchSingleTop = true
    }
}
