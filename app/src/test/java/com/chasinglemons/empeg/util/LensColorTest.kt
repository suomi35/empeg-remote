package com.chasinglemons.empeg.util

import androidx.compose.ui.graphics.Color
import com.chasinglemons.empeg.preferences.EmpegAppPreferences
import com.chasinglemons.empeg.ui.theme.EmpegBlue
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The player's blue lens and the app's accent are one and the same colour: the display tint, the
 * colour picker and the stored default all read from [Constants.LENS_BLUE], which has to stay in
 * step with the theme's [EmpegBlue].
 */
class LensColorTest {

    @Test
    fun `the player's blue lens is the app accent`() {
        assertEquals(EmpegBlue, Color(Constants.LENS_BLUE))
    }

    @Test
    fun `the accent is the blue the app was asked for`() {
        assertEquals("#FF56BCF9", Utils.convertColorToString(EmpegBlue))
    }

    @Test
    fun `a player whose lens colour has never been set is stored as the player's blue`() {
        assertEquals("#FF56BCF9", EmpegAppPreferences.DEFAULT_LENS_COLOR)
    }
}
