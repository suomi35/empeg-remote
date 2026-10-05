package com.chasinglemons.empeg.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.DEFAULT_LENS_COLOR
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.DEFAULT_SWIPE_ACTION
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.DISCOVERY_TIMEOUT
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.VIBRATE
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.EMPEG_IP
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.EMPTY_STRING
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.KEEP_SCREEN_ON
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.LENS_COLOR
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.PERSISTENT_NOTIFICATION
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.PIXEL_FONT
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.SCREEN_REFRESH_RATE
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.SHOW_DISPLAY
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.SHOW_DISPLAY_BOARD
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.USE_KEYBOARD
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.SWIPE_ACTION


class EmpegPreferences(context: Context) : EmpegAppPreferences {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun set(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }

    override fun set(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    override operator fun set(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }

    override operator fun get(key: String, defaultVal: String?): String? {
        val aux: String? = if (defaultVal == null) {
            prefs.getString(key, EMPTY_STRING)
        } else {
            prefs.getString(key, defaultVal)
        }
        return if (defaultVal == null && aux == EMPTY_STRING) {
            null
        } else {
            aux
        }
    }

    override operator fun get(key: String, defaultVal: Boolean): Boolean {
        return prefs.getBoolean(key, defaultVal)
    }

    override operator fun get(key: String, defaultVal: Int): Int {
        return prefs.getInt(key, defaultVal)
    }

    override var empegIp: String
        get() = get(EMPEG_IP, EMPTY_STRING).toString()
        set(value) {
            set(EMPEG_IP, value)
        }

    override var discoveryTimeout: Int
        get() = get(DISCOVERY_TIMEOUT, 2)
        set(value) {
            set(DISCOVERY_TIMEOUT, value)
        }

    override var lensColor: String
        get() = get(LENS_COLOR, DEFAULT_LENS_COLOR).toString()
        set(value) {
            set(LENS_COLOR, value)
        }

    override var screenRefreshRate: Int
        get() = get(SCREEN_REFRESH_RATE, 1000)
        set(value) {
            set(SCREEN_REFRESH_RATE, value)
        }

    override var persistentNotification: Boolean
        get() = get(PERSISTENT_NOTIFICATION, true)
        set(value) {
            set(PERSISTENT_NOTIFICATION, value)
        }

    override var keepScreenOn: Boolean
        get() = get(KEEP_SCREEN_ON, false)
        set(value) {
            set(KEEP_SCREEN_ON, value)
        }

    override var vibrate: Boolean
        get() = get(VIBRATE, true)
        set(value) {
            set(VIBRATE, value)
        }

    override var showDisplay: Boolean
        get() = get(SHOW_DISPLAY, true)
        set(value) {
            set(SHOW_DISPLAY, value)
        }

    override var usePixelFont: Boolean
        get() = get(PIXEL_FONT, true)
        set(value) {
            set(PIXEL_FONT, value)
        }

    override var useKeyboard: Boolean
        get() = get(USE_KEYBOARD, true)
        set(value) {
            set(USE_KEYBOARD, value)
        }

    override var showDisplayBoard: Boolean
        get() = get(SHOW_DISPLAY_BOARD, false)
        set(value) {
            set(SHOW_DISPLAY_BOARD, value)
        }

    override var swipeAction: String
        get() = get(SWIPE_ACTION, DEFAULT_SWIPE_ACTION).toString()
        set(value) {
            set(SWIPE_ACTION, value)
        }
}