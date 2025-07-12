package com.chasinglemons.empeg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.chasinglemons.empeg.app.AppScaffold
import com.chasinglemons.empeg.phone.ui.theme.EmpegRemoteTheme

class EmpegActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val from = intent.getStringExtra("from")
        println(">>> EmpegActivity: from = $from")

        setContent {
            val navController = rememberNavController()

            EmpegRemoteTheme {
                AppScaffold(
                    from = from,
                    navController = navController
                )
            }
        }
    }
}