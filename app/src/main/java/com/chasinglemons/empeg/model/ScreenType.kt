package com.chasinglemons.empeg.model

sealed class ScreenType {
    object RemoteScreen: ScreenType()
    object PlaylistScreen: ScreenType()
}