package com.chasinglemons.empeg.playlist

import com.chasinglemons.empeg.empegapi.EmpegApi
import com.chasinglemons.empeg.empegapi.EmpegItem
import com.chasinglemons.empeg.empegapi.KtorEmpegApi
import com.chasinglemons.empeg.model.Playlist
import com.chasinglemons.empeg.model.PlaylistStatus
import com.chasinglemons.empeg.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Loads playlists from the player and exposes them as [Playlist] models for
 * the UI. All player communication goes through [EmpegApi]; parsing of the
 * XML wire format happens in [com.chasinglemons.empeg.empegapi.PlaylistXmlParser].
 */
class PlaylistRepository(
    private val api: EmpegApi
) {

    private val _playlist = MutableStateFlow<List<Playlist>>(emptyList())
    val playlist: StateFlow<List<Playlist>> = _playlist.asStateFlow()

    private val _status = MutableStateFlow(PlaylistStatus.LOADING)
    val status: StateFlow<PlaylistStatus> = _status.asStateFlow()

    /**
     * Fetches the playlist with the given FID (root by default) and maps the
     * parsed XML items into UI models. On any network/parse failure the
     * status becomes [PlaylistStatus.ERROR] and the previous list is kept.
     */
    suspend fun fetchPlaylist(fid: String = EmpegApi.ROOT_FID): PlaylistStatus {
        _status.value = PlaylistStatus.LOADING
        val result = try {
            api.fetchPlaylist(fid)
        } catch (e: Exception) {
            Timber.e(e, "fetchPlaylist(fid=%s) failed", fid)
            null
        }

        if (result == null) {
            _status.value = PlaylistStatus.ERROR
            return _status.value
        }

        _playlist.value = result.items.map { it.toUiModel() }
        _status.value = PlaylistStatus.LOADED
        return _status.value
    }

    private fun EmpegItem.toUiModel(): Playlist {
        // Item command URLs follow the web lite grammar:
        // ?NODATA&SERIAL=%23<fid>[+|!|-] (append/insert/enqueue), play has no suffix.
        // The '#' is URL-encoded as %23, '+' as %2B.
        return Playlist(
            name = title,
            streamURL = "${KtorEmpegApi.encodePathComponent(title)}.m3u?FID=$tagFid&EXT=.m3u",
            playURL = "?NODATA&SERIAL=" + KtorEmpegApi.encodeSerial(KtorEmpegApi.serialPlay(fid)),
            insertURL = "?NODATA&SERIAL=" + KtorEmpegApi.encodeSerial(KtorEmpegApi.serialInsert(fid)),
            enqueueURL = "?NODATA&SERIAL=" + KtorEmpegApi.encodeSerial(KtorEmpegApi.serialEnqueue(fid)),
            appendURL = "?NODATA&SERIAL=" + KtorEmpegApi.encodeSerial(KtorEmpegApi.serialAppend(fid)),
            url = if (isPlaylist) "?FID=$tagFid&EXT=.xml" else Constants.NO_URL,
            // For tunes the human readable duration is what the UI wants;
            // playlists report their item count.
            length = if (isTune) (duration ?: length ?: "") else (length ?: ""),
            type = type,
            artist = artist ?: "",
            source = source ?: ""
        )
    }
}
