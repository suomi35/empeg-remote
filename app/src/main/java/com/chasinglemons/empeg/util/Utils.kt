package com.chasinglemons.empeg.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt

object Utils {

    fun getColorMatrix(color: Color): ColorMatrix {
        return ColorMatrix(
            floatArrayOf(
                color.red, 0f, 0f, 0f, 0f,
                0f, color.green, 0f, 0f, 0f,
                0f, 0f, color.blue, 0f, 0f,
                0f, 0f, 0f, color.alpha, 0f
            )
        )
    }

    fun convertColorToString(color: Color): String {
        val argb = color.toArgb()
        return String.format("#%08X", argb)
    }

    fun convertStringToColor(hexString: String): Color {
        return try {
            Color(hexString.toColorInt())
        } catch (e: IllegalArgumentException) {
            println("Warning: Invalid hex string '$hexString'. Using default color. ${e.message}")
            Color(0xFF00BFFF)
        }
    }
}