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
    var paused: Boolean = false
    val queue = ArrayDeque<Item>()
    var volume: Int = 50

    fun play(item: Item) {
        playing = item
        paused = false
        queue.clear()
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
        queue.removeFirstOrNull()?.let {
            playing = it
            paused = false
            println("[state] NEXT: ${it.artist} - ${it.title} (queue=${queue.size})")
        } ?: println("[state] NEXT: queue empty, nothing to skip to")
    }

    fun previous() {
        println("[state] PREV (history not tracked; staying on ${playing?.title})")
    }

    fun togglePause() {
        if (playing != null) {
            paused = !paused
            println("[state] ${if (paused) "PAUSE" else "PLAY"}")
        }
    }

    fun nowPlayingLine(): String {
        val p = playing ?: return "Stopped"
        return "${p.artist} - ${p.title}"
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

    /** Renders the player state as a 128x32 image. With a .bf font loaded the
     *  player's own 2-bit-shade glyphs are used (grayscale PNG); otherwise the
     *  hijack kfont (1-bit). */
    fun renderImage(state: PlayerState, format: String): ByteArray {
        val font = bfFont
        if (font != null) return renderWithBfFont(state, font, format)
        val img = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_BINARY)
        drawString(img, 0, 0, state.name)
        drawString(img, 1, 0, state.nowPlayingLine())
        drawString(img, 2, 0, if (state.paused) "PAUSED" else "Queue: ${state.queue.size}")
        drawVolumeBar(img, 3, state.volume)
        val out = ByteArrayOutputStream()
        ImageIO.write(img, format, out)
        return out.toByteArray()
    }

    /** Renders using the player's own .bf font: variable-width glyphs with
     *  2-bit shades (0 blank, 3 brightest). The display is 32 rows tall, so the
     *  layout adapts to the font the player would use:
     *
     *  - a font short enough for three lines (medium.bf, 9px) draws them all:
     *    now playing, player name, then status + volume bar;
     *  - a taller font (large.bf, 18px) draws now playing big at the top and
     *    the two lower lines in [bfSmallFont] (small.bf, 6px), mirroring the
     *    real player's "big line plus small status" display;
     *  - with no smaller font available it falls back to one status line.
     */
    private fun renderWithBfFont(state: PlayerState, font: EmpegBfFont.BfFont, format: String): ByteArray {
        val img = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY)
        val status = if (state.paused) "PAUSED" else "Queue: ${state.queue.size}"
        val threeLines = 3 * font.height <= HEIGHT
        if (threeLines) {
            drawBfString(img, font, 0, 0, state.nowPlayingLine())
            drawBfString(img, font, 0, font.height, state.name)
            drawBfString(img, font, 0, HEIGHT - font.height, status)
            drawBfVolumeBar(img, font, HEIGHT - font.height, state.volume)
        } else {
            drawBfString(img, font, 0, 0, state.nowPlayingLine())
            val small = bfSmallFont
            val sh = small?.height ?: 0
            if (small != null && sh > 0 && font.height + 2 * sh <= HEIGHT) {
                drawBfString(img, small, 0, HEIGHT - 2 * sh, state.name)
                drawBfString(img, small, 0, HEIGHT - sh, status)
                drawBfVolumeBar(img, small, HEIGHT - sh, state.volume)
            } else if (small != null && sh > 0 && font.height + sh <= HEIGHT) {
                drawBfString(img, small, 0, HEIGHT - sh, status)
                drawBfVolumeBar(img, small, HEIGHT - sh, state.volume)
            }
        }
        val out = ByteArrayOutputStream()
        ImageIO.write(img, format, out)
        return out.toByteArray()
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

    private fun drawBfVolumeBar(img: BufferedImage, font: EmpegBfFont.BfFont, yTop: Int, volume: Int) {
        val segments = 20
        val filled = (volume * segments + 50) / 100
        val bar = buildString {
            repeat(segments) { i -> append(if (i < filled) '#' else '-') }
        }
        var col = 0
        drawBfString(img, font, col, yTop, "Vol ")
        col += font.measure("Vol ")
        drawBfString(img, font, col, yTop, bar)
        col += font.measure(bar)
        drawBfString(img, font, col, yTop, " $volume")
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

    private fun drawVolumeBar(img: BufferedImage, textRow: Int, volume: Int) {
        val segments = 20
        val filled = (volume * segments + 50) / 100
        val label = "Vol "
        var col = 0
        drawString(img, textRow, col, label)
        col += measure(label)
        repeat(segments) { i ->
            drawString(img, textRow, col, if (i < filled) "#" else "-")
            col += EmpegFont.charWidth(if (i < filled) '#' else '-')
        }
        drawString(img, textRow, col, " $volume")
    }

    private fun measure(text: String): Int = text.sumOf { EmpegFont.charWidth(it) }
}

// -------------------------------------------------------------- http server

class EmpegHttpServer(
    private val port: Int,
    private val fixturesDir: File,
    private val state: PlayerState
) {
    private val playlistCache = HashMap<String, Playlist?>()

    fun start() {
        val server = HttpServer.create(java.net.InetSocketAddress(port), 0)
        server.executor = Executors.newFixedThreadPool(4)
        server.createContext("/") { ex -> handle(ex) }
        server.createContext("/proc/empeg_screen") { ex ->
            // The real player serves both /proc/empeg_screen.png (weblite) and
            // .gif; serve whichever extension was requested.
            val isPng = ex.requestURI.path.endsWith(".png")
            val format = if (isPng) "png" else "gif"
            ex.responseHeaders.add("Content-Type", "image/${format}")
            val bytes = ScreenRenderer.renderImage(state, format)
            ex.sendResponseHeaders(200, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        server.start()
        println("[http] listening on port $port, fixtures from ${fixturesDir.absolutePath}")
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
        val press = !raw.endsWith(".R")
        val button = if (press) raw else raw.removeSuffix(".R")
        val verb = if (press) "PRESS" else "RELEASE"
        println("[cmd] BUTTON $verb: $button")
        if (press) when (button) {
            "Top" -> state.togglePause()
            "KnobRight" -> state.next()
            "KnobLeft" -> state.previous()
            else -> { /* Left/Right/Bottom/Knob: logged only */ }
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
        } else when (action) {
            "" -> state.play(item)
            "+" -> state.append(item)
            "!" -> state.insert(item)
            "-" -> state.append(item) // enqueue behaves like append in the sim
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

class DiscoveryResponder(private val port: Int, private val name: String) {
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
                        // everything is harmless and more forgiving.
                        val response = "name=$name".toByteArray()
                        socket.send(DatagramPacket(response, response.size, packet.address, packet.port))
                        println("[udp] answered discovery from ${packet.address.hostAddress}")
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
    DiscoveryResponder(8300, name).start()

    println("Ready. Point EmpegRemote at this machine's IP, or browse http://localhost:$port/")
    Thread.currentThread().join()
}
