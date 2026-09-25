package com.chasinglemons.empeg.empegapi

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

/**
 * Parses the emplayer/hijack playlist XML (web lite 0.95 format) into
 * [EmpegPlaylist] objects. Uses javax.xml (DOM) which is available both on
 * Android and on the JVM, so it can be unit tested against captured
 * fixtures without an emulator.
 *
 * Notes on the wire format:
 * - Responses are ISO-8859-1 encoded; decoding is the caller's responsibility
 *   (Ktor applies the response charset automatically).
 * - Unknown FIDs return HTTP 200 with an EMPTY body; [parse] returns null
 *   for that case instead of throwing.
 */
class PlaylistXmlParser {

    fun parse(xml: String): EmpegPlaylist? {
        if (xml.isBlank()) return null

        val document: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(InputSource(StringReader(xml)))
        val root = document.documentElement
        if (root == null || root.tagName != TAG_PLAYLIST) return null

        val items = root.getElementsByTagName(TAG_ITEMS)
            .let { if (it.length > 0) it.item(0) as Element else null }
            ?.getElementsByTagName(TAG_ITEM)
            ?.let { nodes -> (0 until nodes.length).mapNotNull { parseItem(nodes.item(it) as? Element) } }
            ?: emptyList()

        return EmpegPlaylist(
            title = root.getAttribute("title"),
            fid = root.getAttribute("fid"),
            tagFid = root.getAttribute("tagfid"),
            type = root.getAttribute("type"),
            allowCommands = root.getAttribute("allow_commands") == "1",
            allowFiles = root.getAttribute("allow_files") == "1",
            items = items
        )
    }

    private fun parseItem(element: Element?): EmpegItem? {
        if (element == null || element.tagName != TAG_ITEM) return null
        return EmpegItem(
            type = element.childText("type") ?: "",
            fid = element.childText("fid") ?: "",
            tagFid = element.childText("tagfid") ?: "",
            title = element.childText("title") ?: "",
            artist = element.childText("artist"),
            source = element.childText("source"),
            year = element.childText("year"),
            genre = element.childText("genre"),
            comment = element.childText("comment"),
            length = element.childText("length"),
            tracknr = element.childText("tracknr"),
            duration = element.childText("duration"),
            bitrate = element.childText("bitrate"),
            samplerate = element.childText("samplerate"),
            codec = element.childText("codec"),
            offset = element.childText("offset")
        )
    }

    private fun Element.childText(tag: String): String? {
        val nodes = getElementsByTagName(tag)
        return if (nodes.length > 0) nodes.item(0).textContent else null
    }

    companion object {
        const val TAG_PLAYLIST = "playlist"
        const val TAG_ITEMS = "items"
        const val TAG_ITEM = "item"
    }
}
