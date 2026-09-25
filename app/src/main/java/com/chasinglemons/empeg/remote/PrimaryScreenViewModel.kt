package com.chasinglemons.empeg.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chasinglemons.empeg.empegapi.EmpegApi
import com.chasinglemons.empeg.preferences.EmpegPreferences
import com.chasinglemons.empeg.util.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PrimaryScreenViewModel : ViewModel(), KoinComponent {

    private val api: EmpegApi by inject()
    private val preferences: EmpegPreferences by inject()

    private val _empegIp = MutableStateFlow(preferences.empegIp)
    val empegIp = _empegIp.asStateFlow()

    private val _lenColor = MutableStateFlow(Utils.convertStringToColor(preferences.lensColor))
    val lensColor = _lenColor.asStateFlow()

    suspend fun sendCommand(command: String) {
        api.sendNotifyButton(command)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PrimaryScreenViewModel()
            }
        }
    }
}
