package com.chasinglemons.empeg.playlist

import androidx.compose.runtime.mutableStateListOf
import com.chasinglemons.empeg.EmpegApplication
import com.chasinglemons.empeg.model.Playlist
import com.chasinglemons.empeg.model.PlaylistStatus
import com.chasinglemons.empeg.preferences.EmpegPreferences
import com.chasinglemons.empeg.util.Constants
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.http.appendEncodedPathSegments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlaylistRepository: KoinComponent {

    private val preferences: EmpegPreferences by inject()

    private val _playlist = MutableStateFlow(mutableStateListOf<Playlist>())
    val playlist = _playlist.asStateFlow()

    private val _playlistHistory = MutableStateFlow(mutableStateListOf<String>())
    val playlistHistory = _playlistHistory.asStateFlow()

    private val _showPlaylistHistory = MutableStateFlow(false)
    val showPlaylistHistory = _showPlaylistHistory.asStateFlow()

    suspend fun fetchPlaylist(playlistPath: String = "/?FID=101&EXT=.htm"): PlaylistStatus {
        println(">>> fetchPlaylist()")
        var playlistStatus = PlaylistStatus.LOADING

        withContext(Dispatchers.IO) {
            val response: String = EmpegApplication.ktorClient.get {
                url {
                    protocol = URLProtocol.HTTP
                    host = preferences.empegIp
                    appendEncodedPathSegments(playlistPath)
                }
            }.body<String>()

            // parse the result
            val list: MutableList<Playlist> = ArrayList()

            val doc: Document = Jsoup.parse(response)
            val trs = doc.getElementsByTag(Constants.HTML_TR)
            for (tr in trs) {

                val listElements: MutableList<String> = ArrayList()
                val listLinks: MutableList<String> = ArrayList()
                var name = ""
                var length = ""
                var type = ""
                var artist = ""
                var source = ""

                println(">>> TD -> ${tr.text()}")
                val tds = tr.getElementsByTag(Constants.HTML_TD)

                for (td in tds) {
                    println(">>> TD -> ${td.text()}")

                    val link = td.select(Constants.HTML_A).first()

                    if (link?.attr(Constants.HTML_HREF) != null) {
                        listLinks.add(link.attr(Constants.HTML_HREF))
                    }

                    if (td.elementSiblingIndex() == 5) {
                        name = td.text()
                    }
                    if (td.elementSiblingIndex() == 6) {
                        length = td.text()
                    }
                    if (td.elementSiblingIndex() == 7) {
                        type = td.text()
                    }
                    if (td.elementSiblingIndex() == 8) {
                        artist = td.text()
                    }
                    if (td.elementSiblingIndex() == 9) {
                        source = td.text()
                    }
                }

                // build the INSERT link
                val myInsert = listLinks[2].replace("-", "!")

                if (listLinks.size > 5) {
                    if (listLinks[5].endsWith(Constants.EXTENSION_MP3)) { // this is a song, not a dir
                        list.add(
                            Playlist(
                                name = name,
                                streamURL = listLinks[0],
                                playURL = listLinks[1],
                                insertURL = myInsert,
                                enqueueURL = listLinks[2],
                                appendURL = listLinks[3],
                                url = "none",
                                length = length,
                                type = type,
                                artist = artist,
                                source = source
                            )
                        )
                    } else {
                        list.add(
                            Playlist(
                                name = name,
                                streamURL = listLinks[0],
                                playURL = listLinks[1],
                                insertURL = myInsert,
                                enqueueURL = listLinks[2],
                                appendURL = listLinks[3],
                                url = listLinks[5],
                                length = length,
                                type = type,
                                artist = artist,
                                source = source
                            )
                        )
                    }
                } else {
                    // HEAD LIST
                    list.add(
                        Playlist(
                            name = name,
                            streamURL = listLinks[0],
                            playURL = listLinks[1],
                            insertURL = myInsert,
                            enqueueURL = listLinks[2],
                            appendURL = listLinks[4],
                            url = "head",
                            length = length,
                            type = type,
                            artist = artist,
                            source = source
                        )
                    )

                    // Log.i("PLAYLIST_EXPLORER","pListLinks.get(4) = "+pListLinks.get(4));
                    if (response[1].toString() == "add") {
                        _playlistHistory.value.add(listLinks[4])
                    }
                    if (name != Constants.ALL_MUSIC && listLinks.size > 1 /* if nothing is returned (player off or not configured)*/) {
                        _showPlaylistHistory.value = true
                    } else {
                        _showPlaylistHistory.value = false
                    }
                }
            }
            _playlist.value = mutableStateListOf(*list.toTypedArray())
        }
        return playlistStatus
    }
}