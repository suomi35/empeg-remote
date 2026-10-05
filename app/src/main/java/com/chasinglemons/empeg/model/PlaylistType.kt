package com.chasinglemons.empeg.model

enum class PlaylistType(val type: String) {
    PLAYLIST("playlist"),
    TUNE("tune"),
    UNKNOWN("unknown");

    companion object {
        private val map = entries.associateBy(PlaylistType::type)

        fun fromString(type: String): PlaylistType {
            return map[type.lowercase()] ?: UNKNOWN
        }
    }
}