package com.chasinglemons.empeg.model

enum class SwipeAction(val type: String) {
    PLAYLISTS("Playlists"),
    GESTURES("Gestures"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(type: String): SwipeAction {
            return entries.firstOrNull { it.type.equals(type, ignoreCase = true) }
                ?: UNKNOWN
        }
    }
}