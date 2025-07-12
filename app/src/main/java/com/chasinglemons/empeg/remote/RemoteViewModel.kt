package com.chasinglemons.empeg.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.koin.core.component.KoinComponent

class RemoteViewModel: ViewModel(), KoinComponent {


    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RemoteViewModel()
            }
        }
    }
}