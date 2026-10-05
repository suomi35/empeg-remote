package com.chasinglemons.empeg.playlist

import com.chasinglemons.empeg.model.Playlist
import com.chasinglemons.empeg.model.PlaylistType
import com.chasinglemons.empeg.util.Constants
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses the playlist XML that the empeg "web lite" server (hijack/emplayer
 * firmware) returns for `?FID=<tagfid>&EXT=.xml`. Current firmware ignores
 * `EXT=.htm` and always answers with this XML format (its bundled
 * `default.xsl` renders it into the clickable web UI links), so the old
 * HTML-table scraping of the legacy weblite pages no longer works.
 *
 * The command URLs emitted here follow the grammar documented in
 * `fixtures/ghostwheel/README.md` / `default.xsl`:
 *  - browse sub-playlist: `/?FID=<tagfid>&EXT=.xml`
 *  - play:    `/?NODATA&SERIAL=%23<fid>`
 *  - append:  `/?NODATA&SERIAL=%23<fid>%2B`
 *  - insert:  `/?NODATA&SERIAL=%23<fid>!`
 *  - enqueue: `/?NODATA&SERIAL=%23<fid>-`
 *
 * The first returned row is a synthetic header row (url = PLAYLIST_HEAD)
 * carrying the playlist title, mirroring the old app's highlighted first row.
 * An unknown FID yields HTTP 200 with an empty body on a real player, so a
 * blank/invalid document parses to an empty list.
 */
object PlaylistXmlParser {

    fun parse(xml: String): List<Playlist> {
        if (xml.isBlank()) return emptyList()
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
            val root = doc.documentElement ?: return emptyList()
            if (root.tagName != "playlist") return emptyList()

            val list = ArrayList<Playlist>()

            val rootFid = root.getAttribute("fid")
            list.add(
                Playlist(
                    name = root.getAttribute("title").ifBlank { Constants.ALL_MUSIC },
                    streamURL = "",
                    playURL = commandUrl(rootFid, ""),
                    insertURL = "",
                    enqueueURL = "",
                    appendURL = "",
                    url = Constants.PLAYLIST_HEAD,
                    length = root.getAttribute("length"),
                    type = PlaylistType.UNKNOWN,
                    artist = root.getAttribute("artist"),
                    source = root.getAttribute("source")
                )
            )

            val items = doc.getElementsByTagName("item")
            for (i in 0 until items.length) {
                val el = items.item(i) as? Element ?: continue
                val type = PlaylistType.fromString(text(el, "type"))
                val fid = text(el, "fid")
                val tagFid = text(el, "tagfid")
                if (fid.isBlank() && tagFid.isBlank()) continue

                val url = when (type) {
                    PlaylistType.PLAYLIST -> "/?FID=$tagFid&EXT=.xml"
                    else -> Constants.PLAYLIST_NONE
                }
                val length = when (type) {
                    PlaylistType.TUNE -> text(el, "duration").ifBlank { text(el, "length") }
                    else -> text(el, "length")
                }

                list.add(
                    Playlist(
                        name = text(el, "title"),
                        streamURL = "",
                        playURL = commandUrl(fid, ""),
                        insertURL = commandUrl(fid, "!"),
                        enqueueURL = commandUrl(fid, "-"),
                        appendURL = commandUrl(fid, "%2B"),
                        url = url,
                        length = length,
                        type = type,
                        artist = text(el, "artist"),
                        source = text(el, "source")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun commandUrl(fid: String, action: String): String =
        if (fid.isBlank()) "" else "/?NODATA&SERIAL=%23$fid$action"

    private fun text(el: Element, tag: String): String {
        val nodes = el.getElementsByTagName(tag)
        return if (nodes.length > 0) nodes.item(0).textContent.trim() else ""
    }
}
