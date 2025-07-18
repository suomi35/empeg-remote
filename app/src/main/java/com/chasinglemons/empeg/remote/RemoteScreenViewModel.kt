package com.chasinglemons.empeg.remote

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chasinglemons.empeg.preferences.EmpegPreferences
import com.chasinglemons.empeg.util.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RemoteScreenViewModel: ViewModel(), KoinComponent {

    private val preferences: EmpegPreferences by inject()

    private val _lenColor = MutableStateFlow(Utils.convertStringToColor(preferences.lensColor))
    val lensColor = _lenColor.asStateFlow()

    fun updateLensColor(newLensColor: Color) {
        _lenColor.value = newLensColor
        preferences.lensColor = Utils.convertColorToString(newLensColor)
    }


    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RemoteScreenViewModel()
            }
        }
    }
}