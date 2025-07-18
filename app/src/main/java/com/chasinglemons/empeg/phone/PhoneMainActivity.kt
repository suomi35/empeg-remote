package com.chasinglemons.empeg.phone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.chasinglemons.empeg.ui.theme.EmpegRemoteTheme
import com.chasinglemons.empeg.preferences.EmpegPreferences
import org.koin.android.ext.android.inject

class PhoneMainActivity : ComponentActivity() {

    private var isNotificationPermissionGranted = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            isNotificationPermissionGranted = permissions[Manifest.permission.POST_NOTIFICATIONS]
                ?: isNotificationPermissionGranted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences: EmpegPreferences by inject()

        setContent {
            EmpegRemoteTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        requestPermissions()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isNotificationPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            val permissionRequestList = ArrayList<String>()

            if (!isNotificationPermissionGranted) {
                permissionRequestList.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (permissionRequestList.isNotEmpty()) {
                permissionLauncher.launch(permissionRequestList.toTypedArray())
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    EmpegRemoteTheme {
        Greeting("Android")
    }
}