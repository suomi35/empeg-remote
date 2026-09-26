package com.chasinglemons.empeg.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chasinglemons.empeg.empegapi.EmpegApi
import com.chasinglemons.empeg.empegapi.EmpegItem
import com.chasinglemons.empeg.model.Playlist
import com.chasinglemons.empeg.model.PlaylistStatus
import com.chasinglemons.empeg.preferences.EmpegPreferences
import com.chasinglemons.empeg.util.Constants
import com.chasinglemons.empeg.util.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Drives the Playlists tab.
 *
 * The playlist of the player selected in [EmpegPreferences] is read through
 * [PlaylistRepository] (which parses the XML served by the empeg/hijack web
 * interface). Tapping a sub playlist descends into it, tapping a tune plays it
 * on the player. Visited playlists are kept in a breadcrumb so the user can
 * climb back up to the root.
 */
class PlaylistScreenViewModel : ViewModel(), KoinComponent {

    private val repository: PlaylistRepository by inject()
    private val preferences: EmpegPreferences by inject()

    private val _empegIp = MutableStateFlow(preferences.empegIp)
    val empegIp = _empegIp.asStateFlow()

    private val _lenColor = MutableStateFlow(Utils.convertStringToColor(preferences.lensColor))
    val lensColor = _lenColor.asStateFlow()

    val playlist: StateFlow<List<Playlist>> = repository.playlist
    val status: StateFlow<PlaylistStatus> = repository.status

    /** Title of the playlist on screen, e.g. "All Music". */
    private val _title = MutableStateFlow(Constants.ALL_MUSIC)
    val title = _title.asStateFlow()

    /** True while a parent playlist is available to climb back to. */
    private val _canGoUp = MutableStateFlow(false)
    val canGoUp = _canGoUp.asStateFlow()

    /** Visited playlists, oldest first; the last entry is the current one. */
    private val breadcrumb = mutableListOf(Level(EmpegApi.ROOT_FID, Constants.ALL_MUSIC))

    private var loadedOnce = false

    /** Loads the root playlist once, the first time the tab is shown. */
    fun loadIfNeeded() {
        if (loadedOnce) return
        loadedOnce = true
        refresh()
    }

    /** (Re)reads the playlist currently on screen. */
    fun refresh() {
        viewModelScope.launch { repository.fetchPlaylist(currentFid()) }
    }

    /**
     * Opens [item]: sub playlists are browsed into, tunes are played on the
     * player (SERIAL "#<fid>").
     */
    fun onItemClick(item: Playlist) {
        if (item.type == EmpegItem.TYPE_PLAYLIST) {
            breadcrumb.add(Level(item.tagFid, item.name))
            applyBreadcrumb()
            refresh()
        } else {
            viewModelScope.launch {
                if (!repository.play(item.fid)) {
                    Timber.w("player %s refused play of %s (%s)", _empegIp.value, item.name, item.fid)
                }
            }
        }
    }

    /** Returns to the parent playlist; does nothing at the root. */
    fun goUp() {
        if (breadcrumb.size <= 1) return
        breadcrumb.removeAt(breadcrumb.lastIndex)
        applyBreadcrumb()
        refresh()
    }

    private fun currentFid(): String = breadcrumb.last().fid

    private fun applyBreadcrumb() {
        _title.value = breadcrumb.last().title
        _canGoUp.value = breadcrumb.size > 1
    }

    private data class Level(val fid: String, val title: String)


    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PlaylistScreenViewModel()
            }
        }
    }
}