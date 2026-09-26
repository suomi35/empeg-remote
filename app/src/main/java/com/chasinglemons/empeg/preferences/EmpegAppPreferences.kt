package com.chasinglemons.empeg.preferences

import androidx.compose.ui.graphics.Color
import com.chasinglemons.empeg.util.Constants
import com.chasinglemons.empeg.util.Utils

interface EmpegAppPreferences {

    var empegIp: String
    
    var discoveryTimeout: Int

    var lensColor: String

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
        /**
         * The colour stored until a player's lens colour has been set: the player's standard
         * blue, written in the same form [Utils.convertColorToString] stores.
         */
        val DEFAULT_LENS_COLOR = Utils.convertColorToString(Color(Constants.LENS_BLUE))
        const val LENS_COLOR = "lensColor"
    }
}