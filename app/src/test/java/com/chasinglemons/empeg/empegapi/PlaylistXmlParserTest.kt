package com.chasinglemons.empeg.empegapi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

/**
 * Fixture-driven tests for [PlaylistXmlParser]. The fixtures were captured
 * read-only from a live community player (see fixtures/ghostwheel/README.md)
 * and represent the real wire format of empeg web lite 0.95.
 */
class PlaylistXmlParserTest {

    private val parser = PlaylistXmlParser()

    private fun fixture(path: String): String {
        val stream = javaClass.getResourceAsStream(path)
            ?: error("Fixture not found: $path (is fixtures/ghostwheel on the test resource path?)")
        // The player serves ISO-8859-1; decode accordingly.
        return stream.readBytes().toString(Charset.forName("ISO-8859-1"))
    }

    @Test
    fun `parses root playlist with mixed items`() {
        val playlist = parser.parse(fixture("/playlists/FID_101.xml"))

        assertNotNull(playlist)
        playlist!!
        assertEquals("All Music", playlist.title)
        assertEquals("101", playlist.tagFid)
        assertEquals("100", playlist.fid)
        assertEquals("playlist", playlist.type)
        assertTrue(playlist.allowCommands)
        assertTrue(playlist.allowFiles)

        assertEquals(3, playlist.items.size)

        val subPlaylist = playlist.items[0]
        assertTrue(subPlaylist.isPlaylist)
        assertEquals("Misc", subPlaylist.title)
        assertEquals("171", subPlaylist.tagFid)
        assertEquals("170", subPlaylist.fid)
        assertEquals("7", subPlaylist.length)

        val tune = playlist.items[1]
        assertTrue(tune.isTune)
        assertEquals("Fanfare for the Common Man", tune.title)
        assertEquals("Aaron Copland", tune.artist)
        assertEquals("2cf0", tune.fid)
        assertEquals("2cf1", tune.tagFid)
        assertEquals("13:04", tune.duration)
        assertEquals("1/12", tune.tracknr)
        assertEquals("mp3", tune.codec)
    }

    @Test
    fun `parses sub playlist with several playlists and tunes`() {
        val playlist = parser.parse(fixture("/playlists/FID_171.xml"))

        assertNotNull(playlist)
        playlist!!
        assertEquals("Misc", playlist.title)
        assertEquals("171", playlist.tagFid)
        assertEquals(7, playlist.items.size)

        val subPlaylists = playlist.items.filter { it.isPlaylist }
        val tunes = playlist.items.filter { it.isTune }
        assertEquals(4, subPlaylists.size)
        assertEquals(3, tunes.size)
        assertTrue(tunes.all { it.artist != null && it.duration != null })
    }

    @Test
    fun `parses leaf playlists with a single tune`() {
        for (fid in listOf("1712", "2e31", "91e1", "f281")) {
            val playlist = parser.parse(fixture("/playlists/FID_$fid.xml"))
            assertNotNull("FID_$fid.xml should parse", playlist)
            playlist!!
            assertEquals(1, playlist.items.size)
            assertTrue(playlist.items[0].isTune)
        }
    }

    @Test
    fun `empty body from unknown FID returns null`() {
        // edge_bad_FID.xml is the HTTP-200 empty-body response for a bad FID
        assertNull(parser.parse(fixture("/edge_bad_FID.xml")))
        assertNull(parser.parse(""))
        assertNull(parser.parse("   \n  "))
    }

    @Test
    fun `garbage input returns null instead of throwing`() {
        assertNull(parser.parse("<html><body>not a playlist</body></html>"))
        assertNull(parser.parse("<?xml version=\"1.0\"?><unrelated/>"))
    }

    @Test
    fun `parses ISO-8859-1 encoded characters correctly`() {
        // The player declares ISO-8859-1; a Latin-1 byte sequence must
        // survive the round trip when decoded with the right charset.
        val xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
            "<playlist title=\"Café Del Mar\" fid=\"100\" tagfid=\"101\" type=\"playlist\" " +
            "allow_commands=\"1\" allow_files=\"1\">" +
            "<items><item><type>tune</type><fid>1</fid><tagfid>2</tagfid>" +
            "<title>Südseetraum</title><artist>Motörhead</artist></item></items></playlist>"
        val decoded = xml.toByteArray(Charset.forName("ISO-8859-1")).toString(Charset.forName("ISO-8859-1"))

        val playlist = parser.parse(decoded)
        assertNotNull(playlist)
        playlist!!
        assertEquals("Café Del Mar", playlist.title)
        assertEquals("Südseetraum", playlist.items[0].title)
        assertEquals("Motörhead", playlist.items[0].artist)
    }

    @Test
    fun `serial command helpers follow the web lite grammar`() {
        assertEquals("#2cf0", KtorEmpegApi.serialPlay("2cf0"))
        assertEquals("#2cf0+", KtorEmpegApi.serialAppend("2cf0"))
        assertEquals("#2cf0!", KtorEmpegApi.serialInsert("2cf0"))
        assertEquals("#2cf0-", KtorEmpegApi.serialEnqueue("2cf0"))
        assertEquals("%232cf0", KtorEmpegApi.encodeSerial("#2cf0"))
        assertEquals("%232cf0%2B", KtorEmpegApi.encodeSerial("#2cf0+"))
        assertEquals("All%20Music", KtorEmpegApi.encodePathComponent("All Music"))
    }

    @Test
    fun `root playlist flags are booleans not strings`() {
        val playlist = parser.parse(fixture("/playlists/FID_101.xml"))!!
        assertTrue(playlist.allowCommands)
        assertTrue(playlist.allowFiles)
        assertFalse(parser.parse(
            "<playlist allow_commands=\"0\" allow_files=\"0\" fid=\"1\" tagfid=\"2\" type=\"playlist\" title=\"x\">" +
                "<items></items></playlist>"
        )!!.allowCommands)
    }
}
