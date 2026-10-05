package com.chasinglemons.empeg.remote

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent

class RemoteScreenViewModel: ViewModel(), KoinComponent {

    private val _showKeyboard = MutableStateFlow(false)
    val showKeyboard: StateFlow<Boolean?> = _showKeyboard

    fun showKeyboard(enable: Boolean) {
        _showKeyboard.value = enable
    }
}