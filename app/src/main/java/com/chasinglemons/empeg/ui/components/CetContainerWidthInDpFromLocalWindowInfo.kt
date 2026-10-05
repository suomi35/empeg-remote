package com.chasinglemons.empeg.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

@Composable
fun getContainerWidthInDpFromLocalWindowInfo(): Dp {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val containerWidthInPixels: Int = windowInfo.containerSize.width

    val containerWidthInDp: Dp = with(density) {
        containerWidthInPixels.toDp()
    }

    return containerWidthInDp
}