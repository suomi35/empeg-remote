package com.chasinglemons.empeg.model

sealed class PlaylistState {
    object Loading : PlaylistState()
    object Loaded : PlaylistState()
    data class Error(val error: String?) : PlaylistState()
}