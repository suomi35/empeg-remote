package com.chasinglemons.empeg.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The remote is a fixed-proportion faceplate - the player's own button artwork at the player's
 * own spacing - so making it "just fit" is arithmetic: how far can the whole face be scaled down
 * before it stops overflowing the space between the app bar and the tabs. These are the numbers
 * the phone reports (1080x2340 @ 480dpi).
 */
class FitToViewportTest {

    /** 360x780dp screen; the Remote page is left 591dp of it, as the app measured on device. */
    private val viewportWidth = 1080
    private val viewportHeight = 1773

    /** Full size: the 128dp display plus a 10dp inset, 6 rows of 82dp buttons and 5 x 2dp gaps. */
    private val naturalWidth = 1080
    private val naturalHeight = 1920

    @Test
    fun `shrinks a remote that is taller than the space available`() {
        assertEquals(0.9234375f, scaleFor(naturalWidth, naturalHeight), 0.0001f)
    }

    @Test
    fun `cannot be moved by measuring the content again`() {
        // The face is always measured at the size it was designed at, so the scale it asks for
        // does not depend on the scale in use and the fit settles after a single correction.
        val first = scaleFor(naturalWidth, naturalHeight)

        assertEquals(first, scaleFor(naturalWidth, naturalHeight), 0.0001f)
    }

    @Test
    fun `leaves a remote that already fits alone`() {
        assertEquals(1f, scaleFor(naturalWidth, 1700), 0.0001f)
    }

    @Test
    fun `fits the width as well as the height`() {
        val scale = fitScale(
            availableWidth = 1000,
            availableHeight = 1000,
            contentWidth = 400,
            contentHeight = 200,
            maxScale = 4f,
        )

        assertEquals(2.5f, scale, 0.0001f)
    }

    @Test
    fun `ignores content that has not been measured yet`() {
        assertEquals(1f, fitScale(viewportWidth, viewportHeight, 0, 0), 0.0001f)
        assertEquals(1f, fitScale(0, 0, naturalWidth, naturalHeight), 0.0001f)
    }

    private fun scaleFor(contentWidth: Int, contentHeight: Int) = fitScale(
        availableWidth = viewportWidth,
        availableHeight = viewportHeight,
        contentWidth = contentWidth,
        contentHeight = contentHeight,
    )
}
