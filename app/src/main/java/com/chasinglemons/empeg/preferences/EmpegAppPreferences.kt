package com.chasinglemons.empeg.preferences

interface EmpegAppPreferences {

    var empegIp: String
    
    var discoveryTimeout: Int

    var lensColor: String

    var screenRefreshRate: Int

    var persistentNotification: Boolean

    var keepScreenOn: Boolean

    var vibrate: Boolean

    var showDisplay: Boolean

    var usePixelFont: Boolean

    var useKeyboard: Boolean

    var showDisplayBoard: Boolean

    var swipeAction: String

    fun set(key: String, value: String)

    fun set(key: String, value: Boolean)

    fun set(key: String, value: Int)

    fun get(key: String, defaultVal: String?): String?

    fun get(key: String, defaultVal: Boolean): Boolean

    fun get(key: String, defaultVal: Int): Int

    companion object {
        const val EMPTY_STRING = ""
        const val EMPEG_IP = "empegIp"
        const val DISCOVERY_TIMEOUT = "discoveryTimeout"
        const val DEFAULT_LENS_COLOR = "#FF00BFFF"
        const val LENS_COLOR = "lensColor"
        const val SCREEN_REFRESH_RATE = "screenRefreshRate"
        const val PERSISTENT_NOTIFICATION = "persistentNotification"
        const val KEEP_SCREEN_ON = "keepScreenOn"
        const val VIBRATE = "vibrate"
        const val SHOW_DISPLAY = "showDisplay"
        const val PIXEL_FONT = "pixelFont"
        const val USE_KEYBOARD = "useKeyboard"
        const val DEFAULT_SWIPE_ACTION = "playlists"
        const val SWIPE_ACTION = "swipeAction"
        const val SHOW_DISPLAY_BOARD = "showDisplayBoard"
    }
}