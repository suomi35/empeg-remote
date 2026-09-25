package empegsim

import java.io.DataInputStream
import java.io.File

/**
 * Parser for the empeg player's own font files (".bf" files, found on the
 * player's disk under /empeg/lib/fonts/).
 *
 * Format (deciphered from Richard Kirkby's vfdlib vfdlib_registerFont):
 *
 *   header (little-endian):
 *     char  identifier[4]  "EFNT"
 *     int   fileSize
 *     int   version         (1 or 2)
 *     int   maxWidth
 *     int   unknown         (always 32: bits per scanline record)
 *     int   height          (pixel height of every glyph)
 *     int   firstIndex      (char code of the first glyph in the table)
 *     int   numOfCharacters (size of the character table)
 *   [version 2 only:]
 *     int   numactual       (actual number of glyphs)
 *     int   offset          (file offset of the mapping table)
 *     ushort mappingTable[numactual]  (maps sequential index -> table slot)
 *   then, for each table slot i (0..numOfCharacters-1):
 *     int   width           (pixel width of this glyph; 0 = blank/unused)
 *     height x int          scanlines; each is 32 bits of 2-bit pixels,
 *                           LSB-first: bits 0-1 = leftmost pixel, shade 0..3
 */
object EmpegBfFont {

    /** One glyph: `width` x `height` pixels, values 0..3 (0 = blank). */
    class Glyph(val width: Int, val height: Int, val pixels: Array<IntArray>) {
        /** Pixel shade at (x, y), 0 (blank) .. 3 (brightest). */
        operator fun get(x: Int, y: Int) = pixels[y][x]
    }

    class BfFont(
        val height: Int,
        val firstIndex: Int,
        val numOfCharacters: Int,
        private val glyphs: Array<Glyph?>,
    ) {
        fun glyph(ch: Char): Glyph? {
            val idx = ch.code - firstIndex
            return if (idx in glyphs.indices) glyphs[idx] else null
        }

        fun charWidth(ch: Char): Int = glyph(ch)?.width ?: 0

        fun measure(text: String): Int = text.sumOf { charWidth(it) }

        /** How many printable ASCII characters (32..126) this font can render.
         *  Text fonts cover most of the range; the player's `graphics.bf` holds
         *  only digits and punctuation (14 glyphs), so this is what tells a
         *  usable display font apart from an icon/digit-only font. */
        fun textCoverage(): Int = (32..126).count { charWidth(it.toChar()) > 0 }
    }

    fun load(file: File): BfFont {
        val raw = file.readBytes()
        DataInputStream(raw.inputStream()).use { din ->
            fun i32(): Int {
                val b = ByteArray(4)
                din.readFully(b)
                return (b[0].toInt() and 0xFF) or ((b[1].toInt() and 0xFF) shl 8) or
                    ((b[2].toInt() and 0xFF) shl 16) or ((b[3].toInt() and 0xFF) shl 24)
            }
            fun u16(): Int {
                val b = ByteArray(2)
                din.readFully(b)
                return (b[0].toInt() and 0xFF) or ((b[1].toInt() and 0xFF) shl 8)
            }

            val ident = ByteArray(4)
            din.readFully(ident)
            check(ident.decodeToString() == "EFNT") {
                "${file.name}: not an EFNT font (got ${ident.decodeToString()})"
            }
            val fileSize = i32()
            val version = i32()
            val maxWidth = i32()
            val bitsPerScanline = i32() // always 32
            val height = i32()
            val firstIndex = i32()
            val numOfCharacters = i32()
            require(height in 1..64) { "${file.name}: implausible height $height" }
            require(numOfCharacters in 1..1024) { "${file.name}: implausible char count $numOfCharacters" }

            var totalChars = numOfCharacters
            var mapping: IntArray? = null
            if (version == 2) {
                val numactual = i32()
                val mapOffset = i32()
                require(numactual in 1..1024) { "${file.name}: implausible numactual $numactual" }
                check(mapOffset + 2 * numactual <= raw.size) {
                    "${file.name}: mapping table at $mapOffset overruns file"
                }
                mapping = IntArray(numactual) { i ->
                    val at = mapOffset + i * 2
                    (raw[at].toInt() and 0xFF) or ((raw[at + 1].toInt() and 0xFF) shl 8)
                }
                totalChars = numactual
            }

            val glyphs = arrayOfNulls<Glyph>(numOfCharacters)
            // Read per-slot records sequentially; v2 mapping redirects each
            // sequential glyph to a (possibly later) table slot.
            val seen = BooleanArray(numOfCharacters)
            for (i in 0 until totalChars) {
                val slot = mapping?.get(i) ?: i
                if (slot >= numOfCharacters) continue
                val width = i32()
                if (width !in 0..maxWidth) continue
                val pix = Array(height) { IntArray(width) }
                for (y in 0 until height) {
                    var line = i32()
                    for (x in 0 until width) {
                        pix[y][x] = line and 0x03
                        line = line ushr 2
                    }
                }
                if (width > 0 && !seen[slot]) {
                    glyphs[slot] = Glyph(width, height, pix)
                    seen[slot] = true
                }
            }
            return BfFont(height, firstIndex, numOfCharacters, glyphs)
        }
    }

    /** Scans a directory for .bf files and loads them all. */
    fun loadAll(dir: File): Map<String, BfFont> {
        if (!dir.isDirectory) return emptyMap()
        return dir.listFiles { f -> f.extension == "bf" }
            ?.associate { it.name.removeSuffix(".bf") to load(it) }
            ?: emptyMap()
    }
}
