package com.chasinglemons.empeg.model

sealed class ScreenType {
    data object RemoteScreen: ScreenType()
    data object PlaylistScreen: ScreenType()
}