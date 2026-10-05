package com.chasinglemons.empeg.remote

import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chasinglemons.empeg.EmpegApplication
import com.chasinglemons.empeg.model.Playlist
import com.chasinglemons.empeg.model.PlaylistState
import com.chasinglemons.empeg.model.SwipeAction
import com.chasinglemons.empeg.playlist.PlaylistXmlParser
import com.chasinglemons.empeg.preferences.EmpegPreferences
import com.chasinglemons.empeg.util.Constants
import com.chasinglemons.empeg.util.Utils
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.http.appendEncodedPathSegments
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets

class PrimaryScreenViewModel : ViewModel(), KoinComponent {

    private val preferences: EmpegPreferences by inject()

    private val _empegIp = MutableStateFlow(preferences.empegIp)
    val empegIp = _empegIp.asStateFlow()

    private val _lenColor = MutableStateFlow(Utils.convertStringToColor(preferences.lensColor))
    val lensColor = _lenColor.asStateFlow()

    private val _persistentNotification = MutableStateFlow(preferences.persistentNotification)
    val persistentNotification: StateFlow<Boolean> = _persistentNotification.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(preferences.keepScreenOn)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _vibrate = MutableStateFlow(preferences.vibrate)
    val vibrate: StateFlow<Boolean> = _vibrate.asStateFlow()

    private val _showDisplay = MutableStateFlow(preferences.showDisplay)
    val showDisplay: StateFlow<Boolean> = _showDisplay.asStateFlow()

    private val _showDisplayBoard = MutableStateFlow(preferences.showDisplayBoard)
    val showDisplayBoard: StateFlow<Boolean> = _showDisplayBoard.asStateFlow()

    private val _usePixelFont = MutableStateFlow(preferences.usePixelFont)
    val usePixelFont: StateFlow<Boolean> = _usePixelFont.asStateFlow()

    private val _screenRefreshRate = MutableStateFlow(preferences.screenRefreshRate)
    val screenRefreshRate: StateFlow<Int> = _screenRefreshRate.asStateFlow()

    private val _discoveryTimeout = MutableStateFlow(preferences.discoveryTimeout)
    val discoveryTimeout: StateFlow<Int> = _discoveryTimeout.asStateFlow()

    private val _useKeyboard = MutableStateFlow(preferences.useKeyboard)
    val useKeyboard: StateFlow<Boolean> = _useKeyboard.asStateFlow()

    private val _swipeAction = MutableStateFlow(preferences.swipeAction)
    val swipeAction = _swipeAction.asStateFlow()

    private val _playlistState = MutableStateFlow<PlaylistState>(PlaylistState.Loading)
    val playlistState = _playlistState.asStateFlow()

    private val _playlist = MutableStateFlow(mutableStateListOf<Playlist>())
    val playlist = _playlist.asStateFlow()

    private val _playlistHistory = MutableStateFlow(mutableStateListOf<String>())
    val playlistHistory = _playlistHistory.asStateFlow()

    private val _commandError = MutableStateFlow<String?>(null)
    val commandError = _commandError.asStateFlow()

    private var fetchJob: Job? = null

    init {
        fetchPlaylist()
    }

    fun refreshFromPreferences() {
        val latestIp = preferences.empegIp
        val ipChanged = latestIp != _empegIp.value
        _empegIp.value = latestIp
        if (ipChanged && latestIp.isNotBlank()) {
            _playlistHistory.value = mutableStateListOf()
            fetchPlaylist()
        }
    }

    fun consumeCommandError() {
        _commandError.value = null
    }

    fun sendCommand(command: String) {
        viewModelScope.launch {
            try {
                Timber.d("sendCommand: $command")
                withContext(Dispatchers.IO) {
                    EmpegApplication.ktorClient.get {
                        url {
                            protocol = URLProtocol.HTTP
                            host = _empegIp.value
                            appendEncodedPathSegments("/proc/empeg_notify?button=$command")
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleNetworkError(e, affectPlaylist = false)
            }
        }
    }

    fun sendMessage(message: Pair<Int, String>) {
        viewModelScope.launch {
            try {
                val encodedMessage =
                    URLEncoder.encode(message.second, StandardCharsets.UTF_8.toString())
                withContext(Dispatchers.IO) {
                    EmpegApplication.ktorClient.get {
                        url {
                            protocol = URLProtocol.HTTP
                            host = _empegIp.value
                            appendEncodedPathSegments("/proc/empeg_notify?button=NODATA&POPUP%20${message.first}%20$encodedMessage")
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleNetworkError(e, affectPlaylist = false)
            }
        }
    }

    fun playTune(url: String) {
        if (url.isBlank() || url == Constants.PLAYLIST_HEAD || url == Constants.PLAYLIST_NONE) {
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    EmpegApplication.ktorClient.get {
                        url {
                            protocol = URLProtocol.HTTP
                            host = _empegIp.value
                            appendEncodedPathSegments(url)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleNetworkError(e, affectPlaylist = false)
            }
        }
    }

    fun downloadTune(playlist: Playlist) {
        val relativeUrl = playlist.streamURL.ifBlank { playlist.url }
        if (relativeUrl.isBlank() ||
            relativeUrl == Constants.PLAYLIST_HEAD ||
            relativeUrl == Constants.PLAYLIST_NONE
        ) {
            return
        }
        val fullUrl = if (relativeUrl.startsWith("http", ignoreCase = true)) {
            relativeUrl
        } else {
            "http://${_empegIp.value}$relativeUrl"
        }
        val safeName = playlist.name
            .ifBlank { "empeg-track" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val fileName = if (safeName.endsWith(".mp3", ignoreCase = true)) safeName else "$safeName.mp3"
        try {
            val request = DownloadManager.Request(Uri.parse(fullUrl))
                .setTitle(playlist.name)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            EmpegApplication.appInstance
                .getSystemService(DownloadManager::class.java)
                ?.enqueue(request)
        } catch (e: Exception) {
            Timber.e(e, "Failed to enqueue download")
            _commandError.value = "Could not start download."
        }
    }

    fun fetchPlaylist(
        playlistPath: String = Constants.PLAYLIST_ROOT_PATH,
        ignoreHistory: Boolean = false
    ) {
        // Synthetic header rows carry the sentinel urls "head"/"none"; they are
        // play targets, not fetchable paths.
        if (playlistPath.isBlank() ||
            playlistPath == Constants.PLAYLIST_HEAD ||
            playlistPath == Constants.PLAYLIST_NONE
        ) {
            return
        }
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _playlistState.value = PlaylistState.Loading
            try {
                val response = withContext(Dispatchers.IO) {
                    val bytes: ByteArray = EmpegApplication.ktorClient.get {
                        url {
                            protocol = URLProtocol.HTTP
                            host = _empegIp.value
                            appendEncodedPathSegments(playlistPath)
                        }
                    }.body()
                    // The empeg web server always answers in ISO-8859-1.
                    String(bytes, StandardCharsets.ISO_8859_1)
                }

                _playlist.value =
                    mutableStateListOf(*PlaylistXmlParser.parse(response).toTypedArray())

                if (!ignoreHistory && playlistPath != Constants.PLAYLIST_ROOT_PATH) {
                    val history = _playlistHistory.value.toMutableList()
                    if (history.lastOrNull() != playlistPath) {
                        history.add(playlistPath)
                        _playlistHistory.value = history.toMutableStateList()
                    }
                }
                _playlistState.value = PlaylistState.Loaded
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleNetworkError(e, affectPlaylist = true)
            }
        }
    }

    private fun handleNetworkError(e: Exception, affectPlaylist: Boolean) {
        Timber.e(e, "Network error")
        val message = when (e) {
            is ConnectException,
            is NoRouteToHostException,
            is UnknownHostException ->
                "Network Error: Could not connect to the server. Please check your internet connection and the server address."
            is SocketTimeoutException ->
                "Network Error: Connection timed out."
            else ->
                "Network Error: ${e.localizedMessage ?: "Unexpected error"}"
        }
        if (affectPlaylist) {
            _playlistState.value = PlaylistState.Error(message)
        } else {
            _commandError.value = message
        }
    }

    fun navigateBackInPlaylistHistory() {
        val playlistHistoryList = _playlistHistory.value.toMutableList()
        if (playlistHistoryList.size > 1) {
            playlistHistoryList.removeAt(playlistHistoryList.lastIndex)
            _playlistHistory.value = playlistHistoryList.toMutableStateList()
            fetchPlaylist(playlistHistoryList[playlistHistoryList.lastIndex], true)
        }
    }

    fun updatePersistentNotification(enabled: Boolean) {
        preferences.persistentNotification = enabled
        _persistentNotification.value = enabled
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        preferences.keepScreenOn = enabled
        _keepScreenOn.value = enabled
    }

    fun updateVibrate(enabled: Boolean) {
        preferences.vibrate = enabled
        _vibrate.value = enabled
    }

    fun updateShowDisplay(enabled: Boolean) {
        preferences.showDisplay = enabled
        _showDisplay.value = enabled
    }

    fun updateUsePixelFont(enabled: Boolean) {
        preferences.usePixelFont = enabled
        _usePixelFont.value = enabled
    }

    fun updateUseKeyboard(enabled: Boolean) {
        preferences.useKeyboard = enabled
        _useKeyboard.value = enabled
    }

    fun updateDiscoveryTimeout(timeout: Int) {
        preferences.discoveryTimeout = timeout
        _discoveryTimeout.value = timeout
    }

    fun updateScreenRefreshRate(rate: Int) {
        preferences.screenRefreshRate = rate
        _screenRefreshRate.value = rate
    }

    fun updateSwipeAction(swipeAction: SwipeAction) {
        preferences.swipeAction = swipeAction.type
        _swipeAction.value = swipeAction.type
    }

    fun updateShowDisplayBoard(enabled: Boolean) {
        preferences.showDisplayBoard = enabled
        _showDisplayBoard.value = enabled
    }

    fun updateLensColor(newLensColor: Color) {
        preferences.lensColor = Utils.convertColorToString(newLensColor)
        _lenColor.value = newLensColor
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PrimaryScreenViewModel()
            }
        }
    }
}
