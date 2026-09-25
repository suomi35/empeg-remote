package com.chasinglemons.empeg.empegapi

/**
 * A single item in an Empeg playlist, mirroring the <item> element of the
 * emplayer/hijack XML interface (web lite 0.95 and later).
 *
 * Fields are nullable because playlists mix `playlist` and `tune` items and
 * not every field is present on every item type.
 */
data class EmpegItem(
    val type: String,
    val fid: String,
    val tagFid: String,
    val title: String,
    val artist: String?,
    val source: String?,
    val year: String?,
    val genre: String?,
    val comment: String?,
    /** Byte count for tunes, item count for playlists. */
    val length: String?,
    val tracknr: String?,
    /** Human readable duration, e.g. "13:04". */
    val duration: String?,
    val bitrate: String?,
    val samplerate: String?,
    val codec: String?,
    val offset: String?
) {
    val isPlaylist: Boolean get() = type == TYPE_PLAYLIST
    val isTune: Boolean get() = type == TYPE_TUNE

    companion object {
        const val TYPE_PLAYLIST = "playlist"
        const val TYPE_TUNE = "tune"
    }
}

/**
 * A parsed playlist document, mirroring the root <playlist> element.
 */
data class EmpegPlaylist(
    val title: String,
    val fid: String,
    val tagFid: String,
    val type: String,
    val allowCommands: Boolean,
    val allowFiles: Boolean,
    val items: List<EmpegItem>
)
