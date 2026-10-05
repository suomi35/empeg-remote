package com.chasinglemons.empeg

import android.app.Application

class GlobalData : Application() {
    var playlistHistory: MutableList<String?> = ArrayList()
}