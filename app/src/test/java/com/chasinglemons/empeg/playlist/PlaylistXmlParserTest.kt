package com.chasinglemons.empeg.playlist

import com.chasinglemons.empeg.model.PlaylistType
import com.chasinglemons.empeg.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [PlaylistXmlParser] against the playlist XML format served by
 * empeg "web lite" firmware (fixtures captured in fixtures/ghostwheel).
 */
class PlaylistXmlParserTest {

    private val rootXml = """
        <?xml version="1.0" encoding="ISO-8859-1"?>
        <?xml-stylesheet type="text/xsl" href="./default.xsl"?>
        <playlist stylesheet="./default.xsl" host="empeg" allow_files="1" allow_commands="1" type="playlist" tagfid="101" fid="100" length="3" year="Various" options="0x0" genre="Various" title="All Music" artist="Various" source="Various" comment="">
        	<items>
        		<item>
        			<type>playlist</type>
        			<tagfid>171</tagfid>
        			<fid>170</fid>
        			<genre>Various</genre>
        			<title>Misc</title>
        			<artist>Various</artist>
        			<source>Various</source>
        			<length>7</length>
        		</item>
        		<item>
        			<type>tune</type>
        			<tagfid>2cf1</tagfid>
        			<fid>2cf0</fid>
        			<genre>Classical</genre>
        			<title>Fanfare for the Common Man</title>
        			<artist>Aaron Copland</artist>
        			<source>The Man And His Music</source>
        			<length>3050283</length>
        			<duration>13:04</duration>
        		</item>
        	</items>
        </playlist>
    """.trimIndent()

    @Test
    fun `root playlist parses header plus items`() {
        val list = PlaylistXmlParser.parse(rootXml)
        assertEquals(3, list.size)

        val header = list[0]
        assertEquals("All Music", header.name)
        assertEquals(Constants.PLAYLIST_HEAD, header.url)
        // Playing the header plays the whole playlist via its fid.
        assertEquals("/?NODATA&SERIAL=%23100", header.playURL)
    }

    @Test
    fun `playlist item navigates by tagfid`() {
        val item = PlaylistXmlParser.parse(rootXml)[1]
        assertEquals("Misc", item.name)
        assertEquals(PlaylistType.PLAYLIST, item.type)
        assertEquals("/?FID=171&EXT=.xml", item.url)
        assertEquals("7", item.length)
    }

    @Test
    fun `tune item gets command urls from fid`() {
        val item = PlaylistXmlParser.parse(rootXml)[2]
        assertEquals("Fanfare for the Common Man", item.name)
        assertEquals(PlaylistType.TUNE, item.type)
        assertEquals(Constants.PLAYLIST_NONE, item.url)
        assertEquals("/?NODATA&SERIAL=%232cf0", item.playURL)
        assertEquals("/?NODATA&SERIAL=%232cf0-", item.enqueueURL)
        assertEquals("/?NODATA&SERIAL=%232cf0!", item.insertURL)
        assertEquals("/?NODATA&SERIAL=%232cf0%2B", item.appendURL)
        // Tunes show the human-readable duration, not the byte count.
        assertEquals("13:04", item.length)
        assertEquals("Aaron Copland", item.artist)
        assertEquals("The Man And His Music", item.source)
    }

    @Test
    fun `empty body parses to empty list`() {
        // Real players answer unknown FIDs with HTTP 200 and an empty body.
        assertTrue(PlaylistXmlParser.parse("").isEmpty())
        assertTrue(PlaylistXmlParser.parse("   ").isEmpty())
    }

    @Test
    fun `non-playlist and malformed documents parse to empty list`() {
        assertTrue(PlaylistXmlParser.parse("<html><body>hi</body></html>").isEmpty())
        assertTrue(PlaylistXmlParser.parse("<playlist><items>").isEmpty())
    }

    @Test
    fun `item without fids is skipped`() {
        val xml = """
            <playlist tagfid="101" fid="100" title="All Music">
            	<items>
            		<item><type>tune</type><title>No ids</title></item>
            	</items>
            </playlist>
        """.trimIndent()
        val list = PlaylistXmlParser.parse(xml)
        assertEquals(1, list.size) // header only
    }
}
