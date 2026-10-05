package com.chasinglemons.empeg.playlist

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chasinglemons.empeg.model.Playlist
import com.chasinglemons.empeg.preferences.EmpegPreferences
import com.chasinglemons.empeg.util.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlaylistScreenViewModel: ViewModel(), KoinComponent {

    private val preferences: EmpegPreferences by inject()

    private val _empegIp = MutableStateFlow(preferences.empegIp)

    private val _lenColor = MutableStateFlow(Utils.convertStringToColor(preferences.lensColor))
    val lensColor = _lenColor.asStateFlow()

    private val _usePixelFont = MutableStateFlow(preferences.usePixelFont)
    val usePixelFont: StateFlow<Boolean> = _usePixelFont.asStateFlow()

    private val _playlist = MutableStateFlow(mutableStateListOf<Playlist>())
    val playlist = _playlist.asStateFlow()

    private val _playlistHistory = MutableStateFlow(mutableStateListOf<String>())
    val playlistHistory = _playlistHistory.asStateFlow()

    private val _showPlaylistHistory = MutableStateFlow(false)
    val showPlaylistHistory = _showPlaylistHistory.asStateFlow()

}