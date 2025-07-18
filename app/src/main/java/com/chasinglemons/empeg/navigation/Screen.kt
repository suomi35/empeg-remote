package com.chasinglemons.empeg.navigation

sealed class Screen(val route: String) {
    object Primary: Screen(route = "primary")
    object Discovery: Screen(route = "discovery")
    object Playlists: Screen(route = "playlists")
    object Settings: Screen(route = "settings")
}