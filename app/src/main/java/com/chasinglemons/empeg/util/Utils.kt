package com.chasinglemons.empeg.util

import android.graphics.Point
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.toColorInt
import timber.log.Timber

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
        val normalized = hexString.trim().let { value ->
            when {
                value.startsWith("#") -> value
                value.startsWith("0x", ignoreCase = true) -> "#${value.substring(2)}"
                else -> "#$value"
            }
        }
        return try {
            Color(normalized.toColorInt())
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Invalid hex string '%s'. Using default color.", hexString)
            Color(0xFF00BFFF)
        }
    }

    fun translateKeyToButton(key: String): String {
        return when (key.trim().lowercase()) {
            "1" -> Constants.ONE
            "2", "a", "b", "c" -> Constants.TWO
            "3", "d", "e", "f" -> Constants.THREE
            "4", "g", "h", "i" -> Constants.FOUR
            "5", "j", "k", "l" -> Constants.FIVE
            "6", "m", "n", "o" -> Constants.SIX
            "7", "p", "r", "s" -> Constants.SEVEN
            "8", "t", "u", "v" -> Constants.EIGHT
            "9", "w", "x", "y" -> Constants.NINE
            "0", "q", "z" -> Constants.ZERO
            else -> ""
        }
    }

    fun translateSpecialKeyToButton(key: Key): String {
        return when (key) {
            Key.Delete, Key.Back, Key.Backspace -> Constants.CANCEL
            Key.Enter -> Constants.MENU
            else -> ""
        }
    }

    /**
     * Maps a value from a source range [0, sourceMax] to its equivalent
     * in a target range [0, targetMax].
     *
     * @param value The value from the source range (0 to sourceMax).
     * @param sourceMax The maximum value of the source range.
     * @param targetMax The maximum value of the target range.
     * @return The equivalent value in the target range [0, targetMax].
     *         Returns NaN if sourceMax is 0 to prevent division by zero,
     *         unless the value is also 0 (in which case 0 is returned).
     *         The returned value is not clamped, meaning it can be outside
     *         [0, targetMax] if the input `value` is outside [0, sourceMax].
     */
    fun mapValueFromZeroBasedRange(
        value: Float,
        sourceMax: Float,
        targetMax: Float
    ): Float {
        if (sourceMax == 0f) {
            // If sourceMax is 0, the only valid input is 0, which maps to 0 in the target.
            // Any other value is effectively "out of an infinitely dense range"
            // or results in division by zero.
            return if (value == 0f) 0f else Float.NaN
        }

        // 1. Calculate the percentage of the value within the source range [0, sourceMax]
        val percentageInSource = value / sourceMax

        // 2. Apply this percentage to the target range [0, targetMax]
        val mappedValue = percentageInSource * targetMax

        return mappedValue
    }

    /**
     * Overload for Double inputs.
     */
    fun mapValueFromZeroBasedRange(
        value: Double,
        sourceMax: Double,
        targetMax: Double
    ): Double {
        if (sourceMax == 0.0) {
            return if (value == 0.0) 0.0 else Double.NaN
        }
        val percentageInSource = value / sourceMax
        return percentageInSource * targetMax
    }

    /**
     * Overload for Int inputs.
     * Returns a Float as the mapping can be non-integer.
     */
    fun mapValueFromZeroBasedRange(
        value: Int,
        sourceMax: Int,
        targetMax: Int
    ): Float {
        return mapValueFromZeroBasedRange(
            value.toFloat(),
            sourceMax.toFloat(),
            targetMax.toFloat()
        )
    }

    fun translateTouchPoint(clickX: Float, clickY: Float, fasciaDimensionsOnDevice: IntSize): Point {
        val drawableFasciaDimensions = IntSize(572, 172)
        val translatedX = mapValueFromZeroBasedRange(
            clickX.toInt(),
            fasciaDimensionsOnDevice.width,
            drawableFasciaDimensions.width
        )

        val translatedY = mapValueFromZeroBasedRange(
            clickY.toInt(),
            fasciaDimensionsOnDevice.height,
            drawableFasciaDimensions.height
        )

        Timber.d("calculateClickTarget: %s,%s translates to %s,%s", clickX, clickY, translatedX, translatedY)

        return Point(translatedX.toInt(), translatedY.toInt())
    }
}