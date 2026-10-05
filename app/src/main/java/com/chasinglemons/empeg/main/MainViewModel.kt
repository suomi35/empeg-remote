package com.chasinglemons.empeg.main

import androidx.lifecycle.ViewModel
import com.chasinglemons.empeg.preferences.EmpegPreferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainViewModel: ViewModel(), KoinComponent {
    private val preferences: EmpegPreferences by inject()

    fun isEmpegConfigured(): Boolean {
        return preferences.empegIp.isNotEmpty()
    }

    fun getEmpegIp(): String {
        return preferences.empegIp
    }
}