package com.chasinglemons.empeg.empegapi

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * Captures the HTTP requests the app puts on the wire, because the player's
 * web interface is picky about two things that are easy to get wrong:
 *
 * - commands only run when the NODATA flag is present, and
 * - the SERIAL value's '#' must arrive percent-encoded, otherwise the player
 *   reads it as the start of a fragment and the command is lost (it then sees
 *   an empty value, as the simulator reported during bring-up).
 */
class EmpegWireFormatTest {

    private lateinit var server: ServerSocket
    private lateinit var client: HttpClient
    private lateinit var api: KtorEmpegApi
    private val requests = CopyOnWriteArrayList<String>()

    @Before
    fun setUp() {
        requests.clear()
        server = ServerSocket(0)
        thread(isDaemon = true) {
            while (true) {
                val socket = try {
                    server.accept()
                } catch (e: Exception) {
                    return@thread
                }
                thread(isDaemon = true) { serve(socket) }
            }
        }
        client = HttpClient(OkHttp)
        api = KtorEmpegApi(client, { "127.0.0.1:${server.localPort}" })
    }

    @After
    fun tearDown() {
        server.close()
        client.close()
    }

    @Test
    fun `serial play is nodata flagged and percent-encoded`() = runBlocking {
        api.sendSerial(KtorEmpegApi.serialPlay("2cf0"))

        assertEquals("GET /?NODATA=&SERIAL=%232cf0 HTTP/1.1", requests.single())
    }

    @Test
    fun `serial append keeps the plus suffix encoded`() = runBlocking {
        api.sendSerial(KtorEmpegApi.serialAppend("2cf0"))

        assertEquals("GET /?NODATA=&SERIAL=%232cf0%2B HTTP/1.1", requests.single())
    }

    @Test
    fun `button press is sent as a raw button parameter`() = runBlocking {
        api.pressButton("KnobLeft")

        assertEquals("GET /?NODATA=&BUTTONRAW=KnobLeft HTTP/1.1", requests.single())
    }

    @Test
    fun `browsing a playlist asks the player for the xml of that fid`() = runBlocking {
        // An empty body (HTTP 200) is what the player returns for a bad FID.
        api.fetchPlaylist("171")

        assertEquals("GET /?FID=171&EXT=.xml HTTP/1.1", requests.single())
    }

    /** Answers every request with an empty 200 and records the request line. */
    private fun serve(socket: Socket) {
        socket.use { sock ->
            val reader = sock.getInputStream().bufferedReader()
            val writer = sock.getOutputStream()
            while (true) {
                val line = reader.readLine() ?: return
                if (line.startsWith("GET ")) {
                    requests.add(line)
                }
                if (line.isEmpty()) {
                    writer.write(
                        (
                            "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/xml; charset=iso-8859-1\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n\r\n"
                            ).toByteArray()
                    )
                    writer.flush()
                    return
                }
            }
        }
    }
}
