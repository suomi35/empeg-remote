package com.chasinglemons.empeg.empegapi

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URLEncoder

/**
 * Ktor-based implementation of [EmpegApi] against the emplayer/hijack
 * web server. The host is resolved on every call through [hostProvider]
 * so a player change in preferences takes effect immediately.
 */
class KtorEmpegApi(
    private val client: HttpClient,
    private val hostProvider: () -> String,
    private val parser: PlaylistXmlParser = PlaylistXmlParser()
) : EmpegApi {

    override suspend fun fetchPlaylist(fid: String): EmpegPlaylist? = withContext(Dispatchers.IO) {
        val response: String = client.get {
            url {
                playlistQuery(fid, ext = EXT_XML)
            }
        }.body()
        Timber.d("fetchPlaylist(fid=$fid) -> ${response.length} chars")
        parser.parse(response)
    }

    override suspend fun pressButton(button: String) = sendNodata(BUTTON_RAW to button)

    override suspend fun releaseButton(button: String) = sendNodata(BUTTON_RAW to "$button.R")

    override suspend fun sendSerial(command: String) = sendNodata(SERIAL to command)

    override suspend fun sendNotifyButton(command: String) = withContext(Dispatchers.IO) {
        client.get {
            url {
                protocol = URLProtocol.HTTP
                host = hostProvider()
                path(NOTIFY_PATH)
                parameters.append("button", command)
            }
        }
        Unit
    }

    override fun screenUrl(): String = EmpegApi.screenUrlFor(hostProvider())

    private suspend fun sendNodata(vararg params: Pair<String, String>) = withContext(Dispatchers.IO) {
        client.get {
            url {
                protocol = URLProtocol.HTTP
                host = hostProvider()
                params.forEach { (name, value) -> parameters.append(name, value) }
            }
        }
        Unit
    }

    private fun URLBuilder.playlistQuery(fid: String, ext: String) {
        protocol = URLProtocol.HTTP
        host = hostProvider()
        path("/")
        parameters.append("FID", fid)
        parameters.append("EXT", ext)
    }

    companion object {
        const val EXT_XML = ".xml"
        const val BUTTON_RAW = "BUTTONRAW"
        const val SERIAL = "SERIAL"
        const val NOTIFY_PATH = "/proc/empeg_notify"
        /** Screen image path; see [EmpegApi.SCREEN_PATH]. */
        const val SCREEN_PATH = EmpegApi.SCREEN_PATH

        /** Builds a play SERIAL command for the given FID ("#<fid>"). */
        fun serialPlay(fid: String) = "#$fid"

        /** Builds an append SERIAL command for the given FID ("#<fid>+"). */
        fun serialAppend(fid: String) = "#$fid+"

        /** Builds an insert SERIAL command for the given FID ("#<fid>!"). */
        fun serialInsert(fid: String) = "#$fid!"

        /** Builds an enqueue SERIAL command for the given FID ("#<fid>-"). */
        fun serialEnqueue(fid: String) = "#$fid-"

        fun encodePathComponent(value: String): String =
            URLEncoder.encode(value, "UTF-8").replace("+", "%20")

        /**
         * Encodes a SERIAL command value for use in a query string,
         * e.g. "#2cf0+" -> "%232cf0%2B".
         */
        fun encodeSerial(command: String): String =
            URLEncoder.encode(command, "UTF-8").replace("+", "%20")
    }
}
