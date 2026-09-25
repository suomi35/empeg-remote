package empegsim

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.URLDecoder
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

/**
 * Empeg player simulator.
 *
 * Serves the XML playlist fixtures captured from a real player (see
 * fixtures/ghostwheel/README.md) and emulates enough of the emplayer/hijack
 * web interface for EmpegRemote to be developed and tested without hardware:
 *
 * - GET /?FID=<fid>&EXT=.xml  -> playlist XML (unknown FID: 200 + empty body)
 * - GET /                     -> root playlist XML (FID 101)
 * - GET /?NODATA&BUTTONRAW=<b>[.R]  -> button press/release
 * - GET /?NODATA&SERIAL=%23<fid>[+|!|-] -> play/append/insert/enqueue
 * - GET /proc/empeg_notify?button=<b> -> legacy button endpoint
 * - GET /proc/empeg_screen.{gif,png} -> live 128x32 VFD image of player state.
 *   Uses the player's own .bf fonts (grayscale, 2-bit shades) when available in
 *   <fixtures>/fonts, otherwise the 1-bit hijack kfont.
 * - UDP :8300 -> responds "name=<player name>" to discovery requests
 *
 * Run:  ./gradlew :simulator:run [--args="--port=8080 --fixtures=... --name=EmpegSim --font=medium"]
 *
 * --font=<name> forces a font from <fixtures>/fonts (medium, small, large, ...);
 * by default the best text font is picked automatically, ignoring the
 * digits-only graphics.bf. Fonts come from a firmware image via
 * tools/extract_player_fonts_from_upgrade.py or off a player on the LAN via
 * tools/fetch_player_fonts.sh.
 *
 * Note: the UDP responder binds port 8300, so run it on a machine other than
 * the one the app's discovery socket binds to (e.g. desktop + phone/emulator
 * with host networking), or the app's own bind on 8300 will conflict.
 */

// ---------------------------------------------------------------- data model

data class Item(
    val type: String,
    val fid: String,
    val tagFid: String,
    val title: String,
    val artist: String
)

data class Playlist(
    val title: String,
    val fid: String,
    val tagFid: String,
    val items: List<Item>
)

// ------------------------------------------------------------------- parser

/** Minimal parser for the playlist XML format; same shape as the app's parser. */
object PlaylistXml {
    fun parse(xml: String): Playlist? {
        if (xml.isBlank()) return null
        val doc = try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(InputSource(java.io.StringReader(xml)))
        } catch (e: Exception) {
            return null
        }
        val root = doc.documentElement ?: return null
        if (root.tagName != "playlist") return null

        val itemsEl = root.getElementsByTagName("items").item(0)
        val items = if (itemsEl == null) emptyList() else {
            val nodes = itemsEl.childNodes
            (0 until nodes.length).mapNotNull { n ->
                val el = nodes.item(n) as? org.w3c.dom.Element
                if (el == null || el.tagName != "item") null
                else Item(
                    type = text(el, "type"), fid = text(el, "fid"), tagFid = text(el, "tagfid"),
                    title = text(el, "title"), artist = text(el, "artist")
                )
            }
        }
        return Playlist(
            title = attr(root, "title"), fid = attr(root, "fid"),
            tagFid = attr(root, "tagfid"), items = items
        )
    }

    private fun attr(el: org.w3c.dom.Element, name: String) = el.getAttribute(name) ?: ""
    private fun text(el: org.w3c.dom.Element, tag: String): String {
        val nodes = el.getElementsByTagName(tag)
        return if (nodes.length > 0) nodes.item(0).textContent else ""
    }
}

// ------------------------------------------------------------- player state

class PlayerState(val name: String) {
    var playing: Item? = null
    var paused = false
    val queue = ArrayDeque<Item>()
    private val history = ArrayDeque<Item>()

    var volume: Int = DEFAULT_VOLUME
        private set

    /** When the volume last changed, so the screen can flash a volume overlay
     *  the way the real player does when you touch the volume buttons. */
    var volumeChangedAt: Long = 0L
        private set

    /** The last command that arrived, and when - echoed on screen briefly so
     *  presses from the Android app are visible at a glance. */
    var lastCommand: String? = null
        private set
    var lastCommandAt: Long = 0L
        private set

    fun noteCommand(command: String) {
        lastCommand = command
        lastCommandAt = System.nanoTime()
    }

    fun play(item: Item) {
        playing = item
        paused = false
        queue.clear()
        history.clear()
        println("[state] PLAY: ${item.artist} - ${item.title}")
    }

    fun append(item: Item) {
        queue.addLast(item)
        println("[state] APPEND: ${item.artist} - ${item.title} (queue=${queue.size})")
    }

    fun insert(item: Item) {
        // Insert right after whatever is playing (empeg "insert" semantics).
        if (playing != null && queue.isNotEmpty()) {
            val next = queue.removeFirst()
            queue.addFirst(item); queue.addFirst(next)
        } else {
            queue.addFirst(item)
        }
        println("[state] INSERT: ${item.artist} - ${item.title} (queue=${queue.size})")
    }

    fun next() {
        val current = playing
        val upcoming = queue.removeFirstOrNull()
        if (upcoming == null) {
            println("[state] NEXT: queue empty, staying on ${current?.title ?: "nothing"}")
            return
        }
        if (current != null) history.addLast(current)
        playing = upcoming
        paused = false
        println("[state] NEXT: ${upcoming.artist} - ${upcoming.title} (queue=${queue.size})")
    }

    fun previous() {
        val back = history.removeLastOrNull()
        if (back == null) {
            println("[state] PREV: no history yet (staying on ${playing?.title ?: "nothing"})")
            return
        }
        playing?.let { queue.addFirst(it) } // the track we were on goes back in the queue
        playing = back
        paused = false
        println("[state] PREV: ${back.artist} - ${back.title} (queue=${queue.size})")
    }

    fun togglePause() {
        if (playing == null) {
            println("[state] PAUSE ignored: nothing playing")
            return
        }
        paused = !paused
        println("[state] ${if (paused) "PAUSE" else "PLAY"}")
    }

    fun volumeUp(step: Int = 1) = setVolume(this.volume + step)

    fun volumeDown(step: Int = 1) = setVolume(this.volume - step)

    fun setVolume(value: Int) {
        val clamped = value.coerceIn(MIN_VOLUME, MAX_VOLUME)
        volume = clamped
        volumeChangedAt = System.nanoTime()
        println("[state] VOLUME: $clamped/$MAX_VOLUME")
    }

    /** True for a short while after a volume change (drives the overlay). */
    fun volumeOverlayActive(): Boolean = System.nanoTime() - volumeChangedAt < VOLUME_OVERLAY_NANOS

    /** The command to echo on screen, or null when there is nothing recent. */
    fun recentCommand(): String? =
        lastCommand?.takeIf { System.nanoTime() - lastCommandAt < COMMAND_ECHO_NANOS }

    fun nowPlayingLine(): String {
        val p = playing ?: return "Stopped"
        return "${p.artist} - ${p.title}"
    }

    fun stateWord(): String = when {
        playing == null -> "Stopped"
        paused -> "Paused"
        else -> "Playing"
    }

    companion object {
        const val MIN_VOLUME = 0
        const val MAX_VOLUME = 100
        const val DEFAULT_VOLUME = 50
        private const val VOLUME_OVERLAY_NANOS = 1_500_000_000L
        private const val COMMAND_ECHO_NANOS = 1_200_000_000L
    }
}

// ------------------------------------------------------------------ screen

object ScreenRenderer {
    internal const val WIDTH = 128   // EMPEG_SCREEN_COLS
    internal const val HEIGHT = 32   // EMPEG_SCREEN_ROWS
    private const val TEXT_ROWS = HEIGHT / EmpegFont.HEIGHT

    /** The player's own font (.bf files from the player's /empeg/lib/fonts/
     *  directory), if the user has supplied font files; null -> fall back to
     *  the hijack kfont. */
    var bfFont: EmpegBfFont.BfFont? = null

    /** Smaller companion font (usually small.bf) for the lower text lines when
     *  [bfFont] is too tall to fit three lines, as on the real player, which
     *  shows now-playing large and everything else small. */
    var bfSmallFont: EmpegBfFont.BfFont? = null

    /** File name (minus .bf) of [bfFont], for logs and the screen text view. */
    var bfFontName: String = "?"

    /** Renders the player state as a 128x32 image. With a .bf font loaded the
     *  player's own 2-bit-shade glyphs are used (grayscale PNG); otherwise the
     *  hijack kfont (1-bit). */
    fun renderImage(state: PlayerState, format: String): ByteArray {
        val font = bfFont
        if (font != null) return renderWithBfFont(state, font, format)
        val img = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_BINARY)
        if (state.volumeOverlayActive()) {
            drawString(img, 0, 0, "Vol ${state.volume}")
            drawString(img, 1, 0, volumeBar(state.volume, barSegments()))
        } else {
            drawString(img, 0, 0, state.nowPlayingLine())
            drawString(img, 1, 0, commandOrName(state))
            drawString(img, 2, 0, "${state.stateWord()}  q:${state.queue.size}")
            drawString(img, 3, 0, "Vol ${state.volume} ${volumeBar(state.volume, 12)}")
        }
        val out = ByteArrayOutputStream()
        ImageIO.write(img, format, out)
        return out.toByteArray()
    }

    /** The screen as plain text, for the /proc/empeg_screen.txt debugging
     *  endpoint: exactly what the image shows, without decoding a PNG. */
    fun describe(state: PlayerState): String = buildString {
        val font = bfFont
        appendLine("empeg simulator screen (${WIDTH}x${HEIGHT})")
        appendLine("  font        : " + (font?.let { "${bfFontName}.bf, ${it.height}px" } ?: "hijack kfont (1-bit)"))
        appendLine("  overlay     : " + if (state.volumeOverlayActive()) "VOLUME (flashing)" else "none")
        appendLine("  now playing : ${state.nowPlayingLine()}")
        appendLine("  state       : ${state.stateWord()}   queue: ${state.queue.size}")
        appendLine("  volume      : ${state.volume}/${PlayerState.MAX_VOLUME}")
        append("  last command: " + (state.recentCommand() ?: state.lastCommand ?: "-"))
        state.recentCommand() ?: append(" (expired)")
    }

    /** The command echo or the player name, whichever line 2 should show. */
    private fun commandOrName(state: PlayerState): String =
        state.recentCommand()?.let { "> $it" } ?: "${state.name}  ${state.stateWord()}"

    /** Renders using the player's own .bf font: variable-width glyphs with
     *  2-bit shades (0 blank, 3 brightest). The display is 32 rows tall, so the
     *  layout adapts to the font the player would use:
     *
     *  - a font short enough for three lines (medium.bf, 9px) draws them all:
     *    now playing (scrolling when too wide, like the real player), the
     *    player name / last command, then status and the volume bar;
     *  - a taller font (large.bf, 18px) draws now playing big at the top and
     *    the lower lines in [bfSmallFont] (small.bf, 6px), mirroring the
     *    real player's "big line plus small status" display;
     *  - a volume change flashes a "Vol <n>" overlay just like the firmware.
     */
    private fun renderWithBfFont(state: PlayerState, font: EmpegBfFont.BfFont, format: String): ByteArray {
        val img = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY)
        if (state.volumeOverlayActive()) {
            val small = bfSmallFont ?: font
            drawBfString(img, font, 0, 0, "Vol ${state.volume}")
            drawBfString(img, small, 0, HEIGHT - small.height, volumeBar(state.volume, bfBarSegments(small)))
        } else {
            val status = "${state.stateWord()}  q:${state.queue.size}"
            val threeLines = 3 * font.height <= HEIGHT
            if (threeLines) {
                drawBfStringScrolling(img, font, 0, 0, state.nowPlayingLine())
                drawBfString(img, font, 0, font.height, commandOrName(state))
                drawBfString(img, font, 0, HEIGHT - font.height, "${status}  Vol ${state.volume}")
            } else {
                drawBfStringScrolling(img, font, 0, 0, state.nowPlayingLine())
                val small = bfSmallFont
                val sh = small?.height ?: 0
                when {
                    small != null && sh > 0 && font.height + 2 * sh <= HEIGHT -> {
                        drawBfString(img, small, 0, HEIGHT - 2 * sh, commandOrName(state))
                        drawBfString(img, small, 0, HEIGHT - sh, "$status  Vol ${state.volume}")
                    }
                    small != null && sh > 0 && font.height + sh <= HEIGHT ->
                        drawBfString(img, small, 0, HEIGHT - sh, "$status  Vol ${state.volume}")
                    else -> Unit
                }
            }
        }
        val out = ByteArrayOutputStream()
        ImageIO.write(img, format, out)
        return out.toByteArray()
    }

    /** A "#"/"-" bar of [segments] characters for the given volume. */
    private fun volumeBar(volume: Int, segments: Int): String {
        val filled = (volume * segments + 50) / 100
        return buildString { repeat(segments) { i -> append(if (i < filled) '#' else '-') } }
    }

    /** How many bar segments fit on one 8px hijack-kfont row. */
    private fun barSegments(): Int {
        val width = EmpegFont.charWidth('#')
        return ((WIDTH - measure("Vol 100 ")) / width).coerceAtLeast(4)
    }

    /** How many bar segments fit on one .bf font row, before the volume number. */
    private fun bfBarSegments(font: EmpegBfFont.BfFont): Int {
        val width = font.charWidth('#').coerceAtLeast(1)
        return ((WIDTH - font.measure("Vol 100 ")) / width).coerceAtLeast(4)
    }

    private fun drawBfString(
        img: BufferedImage,
        font: EmpegBfFont.BfFont,
        col0: Int,
        yTop: Int,
        text: String,
    ) {
        var col = col0
        for (ch in text) {
            val glyph = font.glyph(ch) ?: continue
            if (col + glyph.width > WIDTH) break
            for (y in 0 until font.height) {
                if (yTop + y >= HEIGHT) break
                for (x in 0 until glyph.width) {
                    val shade = glyph[x, y]
                    if (shade == 0) continue
                    img.setRGB(col + x, yTop + y, SHADE_RGB[shade])
                }
            }
            col += glyph.width
        }
    }

    private val SHADE_RGB = intArrayOf(0, 0xFF404040.toInt(), 0xFF9A9A9A.toInt(), -1)

    /** Draws a line of .bf text, scrolling it right-to-left when it is wider
     *  than the display (the real player scrolls long titles too). The offset
     *  comes from wall-clock time, so successive screen requests animate. */
    private fun drawBfStringScrolling(
        img: BufferedImage,
        font: EmpegBfFont.BfFont,
        col0: Int,
        yTop: Int,
        text: String,
    ) {
        val width = font.measure(text)
        if (width <= WIDTH - col0) {
            drawBfString(img, font, col0, yTop, text)
            return
        }
        val gap = font.measure("   ")
        val period = width + gap
        val offset = ((System.nanoTime() / 1_000_000L) / SCROLL_MS_PER_PIXEL % period).toInt()
        // Draw the tail of the line, then the head so the text wraps around.
        drawBfStringClipped(img, font, col0 - offset, yTop, text)
        drawBfStringClipped(img, font, col0 - offset + period, yTop, text)
    }

    /** Like [drawBfString] but allows a negative starting column, so scrolling
     *  can start off the left edge. */
    private fun drawBfStringClipped(
        img: BufferedImage,
        font: EmpegBfFont.BfFont,
        col0: Int,
        yTop: Int,
        text: String,
    ) {
        var col = col0
        for (ch in text) {
            val glyph = font.glyph(ch) ?: continue
            if (col >= WIDTH) break
            for (y in 0 until font.height) {
                if (yTop + y >= HEIGHT) break
                for (x in 0 until glyph.width) {
                    val px = col + x
                    if (px < 0 || px >= WIDTH) continue
                    val shade = glyph[x, y]
                    if (shade == 0) continue
                    img.setRGB(px, yTop + y, SHADE_RGB[shade])
                }
            }
            col += glyph.width
        }
    }


    /** Draws text in the authentic hijack variable-width font at a text row
     *  (8-pixel rows). Mirrors hijack.c draw_char: bit 0 of each column byte
     *  is the top pixel row. Off-screen characters are dropped. */
    private fun drawString(img: BufferedImage, textRow: Int, col0: Int, text: String) {
        var col = col0
        for (ch in text) {
            val w = EmpegFont.charWidth(ch)
            if (col + w > WIDTH) break
            val glyph = EmpegFont.glyph(ch)
            for (cx in 0 until w) {
                val bits = glyph[cx]
                if (bits == 0) continue
                for (row in 0 until EmpegFont.HEIGHT) {
                    if (bits and (1 shl row) != 0) {
                        img.setRGB(col + cx, textRow * EmpegFont.HEIGHT + row, -1) // white
                    }
                }
            }
            col += w
        }
    }

    private fun measure(text: String): Int = text.sumOf { EmpegFont.charWidth(it) }

    /** Scroll speed of the now-playing line, in milliseconds per pixel. */
    private const val SCROLL_MS_PER_PIXEL = 90L
}

// -------------------------------------------------------------- http server

class EmpegHttpServer(
    private val port: Int,
    private val fixturesDir: File,
    private val state: PlayerState
) {
    private val playlistCache = HashMap<String, Playlist?>()
    private var lastScreenSignature: String? = null

    fun start() {
        val server = HttpServer.create(java.net.InetSocketAddress(port), 0)
        server.executor = Executors.newFixedThreadPool(4)
        server.createContext("/") { ex -> handle(ex) }
        server.createContext("/proc/empeg_screen") { ex ->
            // The real player serves /proc/empeg_screen.png (what weblite and
            // the Android app poll) and hijack also serves .gif; serve whichever
            // extension was asked for. ".txt" is a simulator-only debugging view
            // that describes the screen in words.
            when {
                ex.requestURI.path.endsWith(".txt") -> {
                    val text = ScreenRenderer.describe(state)
                    logScreenChange(text)
                    val bytes = text.toByteArray()
                    ex.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
                    ex.sendResponseHeaders(200, bytes.size.toLong())
                    ex.responseBody.use { it.write(bytes) }
                }
                else -> {
                    val format = if (ex.requestURI.path.endsWith(".png")) "png" else "gif"
                    ex.responseHeaders.add("Content-Type", "image/$format")
                    val bytes = ScreenRenderer.renderImage(state, format)
                    logScreenChange(if (format == "png") bytes.contentHashCode().toString() else null)
                    ex.sendResponseHeaders(200, bytes.size.toLong())
                    ex.responseBody.use { it.write(bytes) }
                }
            }
        }
        server.start()
        println("[http] listening on port $port, fixtures from ${fixturesDir.absolutePath}")
    }

    /** Logs screen fetches only when the rendered content actually changes, so
     *  the app's ~10 Hz polling does not flood the console but a new frame
     *  (volume change, track change, button echo) is visible. */
    private fun logScreenChange(signature: String?) {
        if (signature == null) return
        if (signature == lastScreenSignature) return
        lastScreenSignature = signature
        val overlay = if (state.volumeOverlayActive()) " [VOLUME OVERLAY]" else ""
        println("[screen] ${state.nowPlayingLine()} | ${state.stateWord()} | vol ${state.volume}$overlay")
    }

    private fun handle(ex: HttpExchange) {
        try {
            val query = ex.requestURI.rawQuery ?: ""
            val params = parseQuery(query)
            val fid = params["FID"]
            when {
                fid != null -> servePlaylist(ex, fid)
                params.containsKey("NODATA") -> when {
                    params.containsKey("BUTTONRAW") -> handleButton(params["BUTTONRAW"] ?: "", ex)
                    params.containsKey("SERIAL") -> handleSerial(params["SERIAL"] ?: "", ex)
                    else -> respondEmpty(ex)
                }
                ex.requestURI.path == "/proc/empeg_notify" -> {
                    println("[cmd] notify button=" + params["button"])
                    respondEmpty(ex)
                }
                ex.requestURI.path == "/" && query.isEmpty() -> servePlaylist(ex, "101")
                else -> {
                    println("[http] unhandled: ${ex.requestURI}")
                    respondEmpty(ex)
                }
            }
        } catch (e: Exception) {
            println("[http] error handling ${ex.requestURI}: $e")
            try { ex.sendResponseHeaders(500, -1) } catch (_: Exception) {}
        }
    }

    private fun servePlaylist(ex: HttpExchange, fid: String) {
        val file = File(fixturesDir, "playlists/FID_$fid.xml")
        if (!file.isFile) {
            // Real behavior: unknown FID -> HTTP 200 with an empty body.
            println("[http] playlist FID=$fid -> 200 EMPTY (unknown)")
            ex.sendResponseHeaders(200, -1)
            ex.responseBody.close()
            return
        }
        val bytes = file.readBytes()
        ex.responseHeaders.add("Content-Type", "text/xml; charset=iso-8859-1")
        ex.sendResponseHeaders(200, bytes.size.toLong())
        ex.responseBody.use { it.write(bytes) }
        println("[http] playlist FID=$fid -> ${bytes.size} bytes")
    }

    private fun handleButton(raw: String, ex: HttpExchange) {
        // The app sends "VolUp" (press), "VolUp.R" (release) and "VolUp.L"
        // (long press); weblite sends "<Button>" and "<Button>.R".
        val release = raw.endsWith(".R")
        val stripped = raw.removeSuffix(".R")
        val long = stripped.endsWith(".L")
        val button = stripped.removeSuffix(".L")
        val kind = if (release) "RELEASE" else if (long) "LONG" else "PRESS"
        println("[cmd] BUTTON $kind: $button")
        if (release) {
            respondEmpty(ex)
            return
        }
        val step = if (long) LONG_PRESS_VOLUME_STEP else 1
        val hold = if (long) " (hold)" else ""
        state.noteCommand("$button$hold")
        when (button) {
            "VolUp" -> state.volumeUp(step)
            "VolDown" -> state.volumeDown(step)
            "Play", "Top", "Knob" -> state.togglePause()
            "NextTrack", "KnobRight" -> state.next()
            "PrevTrack", "KnobLeft" -> state.previous()
            else -> { /* menu/number/visual buttons: echoed on screen, no state change */ }
        }
        respondEmpty(ex)
    }

    private fun handleSerial(raw: String, ex: HttpExchange) {
        // SERIAL values arrive URL-encoded, e.g. %232cf0+ / %232cf0%21 / %232cf0-
        val decoded = URLDecoder.decode(raw, "UTF-8")
        val body = decoded.dropWhile { it == '#' }
        val fid = body.trimEnd('+', '!', '-')
        val action = if (fid.isEmpty()) "" else body.removePrefix(fid)
        val item = findItem(fid)
        val actionName = action.ifEmpty { "play" }
        println("[cmd] SERIAL: '$decoded' (fid=$fid action='$actionName')")
        if (item == null) {
            println("[cmd]   -> unknown fid $fid")
            state.noteCommand("SERIAL $decoded (unknown fid)")
        } else {
            state.noteCommand("${actionName.uppercase()} ${item.title}")
            when (action) {
                "" -> state.play(item)
                "+" -> state.append(item)
                "!" -> state.insert(item)
                "-" -> state.append(item) // enqueue behaves like append in the sim
            }
        }
        respondEmpty(ex)
    }

    private fun findItem(fid: String): Item? {
        // Search every playlist fixture we have.
        val dir = File(fixturesDir, "playlists")
        dir.listFiles { f -> f.name.endsWith(".xml") }?.forEach { f ->
            val key = f.absolutePath
            val playlist = playlistCache.getOrPut(key) {
                PlaylistXml.parse(f.readText(Charsets.ISO_8859_1))
            }
            // Playlists themselves (e.g. leaf playlist fid 2e31 containing track 2e30)
            // resolve to their first item, mirroring "play a playlist" on a real player.
            if (playlist?.fid?.equals(fid, ignoreCase = true) == true ||
                playlist?.tagFid?.equals(fid, ignoreCase = true) == true
            ) {
                return playlist.items.firstOrNull()
            }
            playlist?.items?.firstOrNull { it.fid.equals(fid, ignoreCase = true) }?.let { return it }
        }
        return null
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        return query.split('&').mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx < 0) pair to "" else pair.substring(0, idx) to pair.substring(idx + 1)
        }.toMap()
    }

    private fun respondEmpty(ex: HttpExchange) {
        ex.sendResponseHeaders(200, -1)
        ex.responseBody.close()
    }
}

// ---------------------------------------------------------------- discovery

class DiscoveryResponder(private val port: Int, private val name: String, private val httpPort: Int) {
    fun start() {
        Thread {
            try {
                val socket = DatagramSocket(port)
                socket.broadcast = true
                socket.reuseAddress = true
                println("[udp] discovery responder on port $port")
                val buf = ByteArray(1024)
                while (true) {
                    val packet = DatagramPacket(buf, buf.size)
                    socket.receive(packet)
                    val received = String(packet.data, 0, packet.length)
                    println("[udp] packet from ${packet.address.hostAddress}:${packet.port}: '${received.trim()}'")
                    if (received.isNotBlank()) {
                        // Reply to any probe (the app sends "?"); replying to
                        // everything is harmless and more forgiving. A real
                        // player only sends "name=<name>"; the extra port field
                        // tells EmpegRemote which port to use, so discovery of a
                        // simulator on 8080/8099 configures itself.
                        val response = "name=$name port=$httpPort".toByteArray()
                        socket.send(DatagramPacket(response, response.size, packet.address, packet.port))
                        println("[udp] answered discovery from ${packet.address.hostAddress} -> ${String(response)}")
                    }
                }
            } catch (e: Exception) {
                println("[udp] discovery responder stopped: $e")
            }
        }.apply { isDaemon = true; start() }
    }
}

// --------------------------------------------------------------------- main

/** Base names of the fonts the screen renderer prefers, best first. The player
 *  itself ships graphics/medium/small/large.bf in /empeg/lib/fonts; the hijack
 *  and "visual" names are accepted for fonts people copy in by hand. */
private val FONT_PREFERENCE = listOf(
    "medium", "player-medium", "visual-medium",
    "small", "player-small", "visual-small",
    "large", "player-large", "visual-large",
    "graphics", "graphics-large", "graphics_large",
)

/** How many volume steps the app's long-press on VolUp/VolDown applies; the
 *  real player ramps the volume while the button is held. */
private const val LONG_PRESS_VOLUME_STEP = 5

fun main(argv: Array<String>) {
    var port = 8080
    var fixtures = "fixtures/ghostwheel"
    var name = "EmpegSim"
    var fontName: String? = null

    for (arg in argv) {
        when {
            arg.startsWith("--port=") -> port = arg.substringAfter('=').toInt()
            arg.startsWith("--fixtures=") -> fixtures = arg.substringAfter('=')
            arg.startsWith("--name=") -> name = arg.substringAfter('=')
            arg.startsWith("--font=") -> fontName = arg.substringAfter('=')
        }
    }

    println("=== Empeg Simulator ('$name') ===")
    println("HTTP port: $port  fixtures: $fixtures")
    println("Screen: http://localhost:$port/proc/empeg_screen.gif")

    val state = PlayerState(name)
    // Load the player's own fonts (.bf files from the player's /empeg/lib/fonts
    // directory) if they are present in <fixtures>/fonts - extract them with
    // tools/extract_player_fonts_from_upgrade.py (offline, from a firmware
    // image) or fetch_player_fonts.sh (from a player on the LAN).
    val fonts = EmpegBfFont.loadAll(File(fixtures, "fonts"))
    // Fonts that can actually draw text: graphics.bf and friends only hold a
    // handful of digits/punctuation glyphs, which would render letters as
    // nothing at all, so they are never chosen for the text lines.
    val textFonts = fonts.filterValues { it.textCoverage() >= 20 }
    val automatic = textFonts.entries.sortedWith(
        compareBy<Map.Entry<String, EmpegBfFont.BfFont>> {
            FONT_PREFERENCE.indexOf(it.key.lowercase()).let { rank -> if (rank < 0) Int.MAX_VALUE else rank }
        }.thenBy { it.value.height }
    ).firstOrNull()
    val chosen = if (fontName == null) {
        automatic
    } else {
        val key = fontName.removeSuffix(".bf").lowercase()
        fonts.entries.firstOrNull { it.key.lowercase() == key }.also { hit ->
            if (hit == null) {
                println("Font '$fontName' not found in $fixtures/fonts (available: ${fonts.keys.sorted()})")
                if (automatic != null) println("  using ${automatic.key}.bf instead")
            } else if (hit.value.textCoverage() < 20) {
                println("Warning: ${hit.key}.bf has only ${hit.value.textCoverage()} text glyphs; " +
                    "letters will not render")
            }
        } ?: automatic
    }
    ScreenRenderer.bfFont = chosen?.value
    ScreenRenderer.bfFontName = chosen?.key ?: "kfont"
    // Companion font for the lower lines when the chosen font is too tall for
    // three of its own lines (large.bf, 18px): the real player pairs it with
    // small.bf.
    ScreenRenderer.bfSmallFont = chosen?.let { primary ->
        if (3 * primary.value.height > ScreenRenderer.HEIGHT) {
            textFonts.entries
                .filter { it.key != primary.key && it.value.height < primary.value.height }
                .minByOrNull { it.value.height }?.value
        } else null
    }
    if (chosen != null) {
        val small = ScreenRenderer.bfSmallFont
        println(
            "Player font: ${chosen.key}.bf - ${chosen.value.height}px glyphs, " +
                "${chosen.value.textCoverage()} printable chars (${fonts.size} .bf file(s) in $fixtures/fonts)"
        )
        if (small != null) println("  lower lines: ${small.height}px glyphs")
    } else {
        println("No usable .bf fonts in $fixtures/fonts - using hijack kfont for the screen")
    }
    EmpegHttpServer(port, File(fixtures), state).start()
    DiscoveryResponder(8300, name, port).start()

    val lan = localLanAddress()
    println("Ready. In EmpegRemote, enter the player as \"<host>:<port>\" (the app appends the port itself only if you give it).")
    if (lan != null) println("  display: http://$lan:$port/proc/empeg_screen.png   (this is what the app polls)")
    println("  text view of the same screen: http://localhost:$port/proc/empeg_screen.txt")
    Thread.currentThread().join()
}

/** Best-guess LAN IPv4 of this machine, so the log can print a URL the phone
 *  can actually reach (localhost is useless there). */
private fun localLanAddress(): String? = runCatching {
    java.net.NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<java.net.Inet4Address>()
        .firstOrNull { it.isSiteLocalAddress }
        ?.hostAddress
}.getOrNull()
