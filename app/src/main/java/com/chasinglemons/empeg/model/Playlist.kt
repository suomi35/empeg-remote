package com.chasinglemons.empeg.model


class Playlist(
    val name: String,
    /** FID used for commands, i.e. SERIAL "#<fid>" plays this item. */
    val fid: String,
    /** FID used to browse the item, i.e. "?FID=<tagfid>&EXT=.xml". */
    val tagFid: String,
    val streamURL: String,
    val playURL: String,
    val insertURL: String,
    val enqueueURL: String,
    val appendURL: String,
    val url: String,
    val length: String,
    val type: String,
    val artist: String,
    val source: String
)