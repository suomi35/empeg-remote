package com.chasinglemons.empeg.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun KeepScreenOn(keepScreenOn: Boolean = true) {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit, keepScreenOn) {
        val window = activity?.window
        if (keepScreenOn && window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            if (window != null && keepScreenOn) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}