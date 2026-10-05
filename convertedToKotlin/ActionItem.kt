package com.chasinglemons.empeg

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

class ActionItem {
    var icon: Drawable? = null
    var thumb: Bitmap? = null
    var title: String? = null
    var isSelected: Boolean = false

    constructor()

    constructor(icon: Drawable?) {
        this.icon = icon
    }
}