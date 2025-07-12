package com.chasinglemons.empeg.app

import androidx.lifecycle.ViewModel
import com.chasinglemons.empeg.preferences.EmpegPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppViewModel: ViewModel(), KoinComponent {
    private val preferences: EmpegPreferences by inject()
    private var innerShowTopBar = MutableStateFlow(true)

    val showTopBar = innerShowTopBar.asStateFlow()

    fun isEmpegConfigured(): Boolean {
        return preferences.empegIp.isNotEmpty()
    }
}