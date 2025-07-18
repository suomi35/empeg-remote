package com.chasinglemons.empeg.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chasinglemons.empeg.EmpegApplication
import com.chasinglemons.empeg.preferences.EmpegPreferences
import com.chasinglemons.empeg.util.Utils
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.http.appendEncodedPathSegments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PrimaryScreenViewModel: ViewModel(), KoinComponent {

    private val preferences: EmpegPreferences by inject()

    private val _empegIp = MutableStateFlow(preferences.empegIp)
    val empegIp = _empegIp.asStateFlow()

    private val _lenColor = MutableStateFlow(Utils.convertStringToColor(preferences.lensColor))
    val lensColor = _lenColor.asStateFlow()

    suspend fun sendCommand(command: String) {
        println(">>> sendCommand: $command")
        withContext(Dispatchers.IO) {
            EmpegApplication.ktorClient.get {
                url {
                    protocol = URLProtocol.HTTP
                    host = _empegIp.value
                    appendEncodedPathSegments("/proc/empeg_notify?button=$command")
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PrimaryScreenViewModel()
            }
        }
    }
}