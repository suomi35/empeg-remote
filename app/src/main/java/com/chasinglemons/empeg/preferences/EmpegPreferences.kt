package com.chasinglemons.empeg.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.DISCOVERY_TIMEOUT
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.EMPEG_IP
import com.chasinglemons.empeg.preferences.EmpegAppPreferences.Companion.EMPTY_STRING


class EmpegPreferences(context: Context) : EmpegAppPreferences {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun set(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun set(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override operator fun set(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
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
}