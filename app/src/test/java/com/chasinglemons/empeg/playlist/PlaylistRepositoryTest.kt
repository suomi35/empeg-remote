package com.chasinglemons.empeg.playlist

import com.chasinglemons.empeg.empegapi.EmpegApi
import com.chasinglemons.empeg.empegapi.EmpegItem
import com.chasinglemons.empeg.empegapi.EmpegPlaylist
import com.chasinglemons.empeg.model.PlaylistStatus
import com.chasinglemons.empeg.util.Constants
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Tests the mapping of the player's XML playlist items (as produced by
 * [com.chasinglemons.empeg.empegapi.PlaylistXmlParser]) onto the models used by
 * the Playlists tab, plus the command grammar used for playback.
 */
class PlaylistRepositoryTest {

    private val api = FakeEmpegApi()
    private val repository = PlaylistRepository(api)

    @Test
    fun `maps items to ui models keeping browse and command fids apart`() = runBlocking {
        api.response = rootPlaylist()

        assertEquals(PlaylistStatus.LOADED, repository.fetchPlaylist(EmpegApi.ROOT_FID))
        assertEquals(EmpegApi.ROOT_FID, api.requestedFid)

        val items = repository.playlist.value
        assertEquals(2, items.size)

        val subPlaylist = items[0]
        assertEquals("Misc", subPlaylist.name)
        assertEquals("170", subPlaylist.fid)      // command fid
        assertEquals("171", subPlaylist.tagFid)   // browse fid
        assertEquals("?FID=171&EXT=.xml", subPlaylist.url)
        assertEquals("7", subPlaylist.length)     // item count for playlists

        val tune = items[1]
        assertEquals("Fanfare for the Common Man", tune.name)
        assertEquals("2cf0", tune.fid)
        assertEquals("2cf1", tune.tagFid)
        assertEquals("13:04", tune.length)        // duration for tunes
        assertEquals(Constants.NO_URL, tune.url)  // tunes are not browsable
        assertEquals("Fanfare%20for%20the%20Common%20Man.m3u?FID=2cf1&EXT=.m3u", tune.streamURL)
        assertEquals("?NODATA&SERIAL=%232cf0", tune.playURL)
        assertEquals("?NODATA&SERIAL=%232cf0%2B", tune.appendURL)
        // weblite links use a literal '!'; %21 decodes to the same command.
        assertEquals("?NODATA&SERIAL=%232cf0%21", tune.insertURL)
        assertEquals("?NODATA&SERIAL=%232cf0-", tune.enqueueURL)
    }

    @Test
    fun `empty body is reported as an empty playlist rather than an error`() = runBlocking {
        api.response = null

        assertEquals(PlaylistStatus.LOADED, repository.fetchPlaylist())
        assertTrue(repository.playlist.value.isEmpty())
    }

    @Test
    fun `unreachable player reports error and keeps the previous playlist`() = runBlocking {
        api.response = rootPlaylist()
        repository.fetchPlaylist()
        assertEquals(2, repository.playlist.value.size)

        api.failFetch = true
        assertEquals(PlaylistStatus.ERROR, repository.fetchPlaylist())
        assertEquals(2, repository.playlist.value.size)
    }

    @Test
    fun `play sends the serial command built from the item fid`() = runBlocking {
        assertTrue(repository.play("2cf0"))
        assertEquals(listOf("#2cf0"), api.serialCommands)
    }

    @Test
    fun `play reports failure when the player is unreachable`() = runBlocking {
        api.failCommands = true
        assertFalse(repository.play("2cf0"))
    }

    private fun rootPlaylist() = EmpegPlaylist(
        title = "All Music",
        fid = "100",
        tagFid = EmpegApi.ROOT_FID,
        type = EmpegItem.TYPE_PLAYLIST,
        allowCommands = true,
        allowFiles = true,
        items = listOf(
            item(
                type = EmpegItem.TYPE_PLAYLIST, fid = "170", tagFid = "171",
                title = "Misc", artist = "Various", source = "Various", length = "7"
            ),
            item(
                type = EmpegItem.TYPE_TUNE, fid = "2cf0", tagFid = "2cf1",
                title = "Fanfare for the Common Man", artist = "Aaron Copland",
                source = "The Man And His Music", length = "3050283", duration = "13:04"
            )
        )
    )

    private fun item(
        type: String,
        fid: String,
        tagFid: String,
        title: String,
        artist: String?,
        source: String?,
        length: String?,
        duration: String? = null,
    ) = EmpegItem(
        type = type,
        fid = fid,
        tagFid = tagFid,
        title = title,
        artist = artist,
        source = source,
        year = null,
        genre = null,
        comment = null,
        length = length,
        tracknr = null,
        duration = duration,
        bitrate = null,
        samplerate = null,
        codec = null,
        offset = null
    )

    /** In-memory stand-in for the player, mirroring the XML it would return. */
    private class FakeEmpegApi : EmpegApi {

        var response: EmpegPlaylist? = null
        var requestedFid: String? = null
        var failFetch = false
        var failCommands = false

        val serialCommands = mutableListOf<String>()
        val buttonPresses = mutableListOf<String>()

        override suspend fun fetchPlaylist(fid: String): EmpegPlaylist? {
            requestedFid = fid
            if (failFetch) throw IOException("player unreachable")
            return response
        }

        override suspend fun pressButton(button: String) {
            buttonPresses.add(button)
        }

        override suspend fun releaseButton(button: String) {
            buttonPresses.add("$button.R")
        }

        override suspend fun sendSerial(command: String) {
            if (failCommands) throw IOException("player unreachable")
            serialCommands.add(command)
        }

        override suspend fun sendNotifyButton(command: String) {
            buttonPresses.add(command)
        }

        override fun screenUrl(): String = "http://test.local${EmpegApi.SCREEN_PATH}"
    }
}
