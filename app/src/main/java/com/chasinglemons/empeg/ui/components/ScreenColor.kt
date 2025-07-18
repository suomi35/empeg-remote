package com.chasinglemons.empeg.ui.components

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter

enum class ScreenColor(val colorFilter: ColorFilter) {
    BLUE(
        ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    0f, 0f, 0f, 0f, 0f,
                    0f, 0.4f, 0.35f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0.1f, 1f, 0f, 0f
                )
            )
        )
    ),
    GREEN(
        ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    0f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f
                )
            )
        )
    ),
    RED(
        ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 0f, 0f,
                    1f, 0f, 0f, 0f, 0f
                )
            )
        )
    ),
    YELLOW(
        ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,  //red
            0f, 1f, 0f, 0f, 0f,  //green
            0f, 0f, 0f, 0f, 0f,  //blue
            0.5f, 0.5f, 0f, 0f, 0f //alpha
                )
            )
        )
    )
}