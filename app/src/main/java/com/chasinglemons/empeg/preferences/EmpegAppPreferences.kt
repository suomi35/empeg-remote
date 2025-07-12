package com.chasinglemons.empeg.preferences

interface EmpegAppPreferences {

    var empegIp: String
    
    var discoveryTimeout: Int

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
    }
}