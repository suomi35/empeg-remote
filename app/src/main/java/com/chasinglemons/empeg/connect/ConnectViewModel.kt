package com.chasinglemons.empeg.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chasinglemons.empeg.model.Empeg
import com.chasinglemons.empeg.preferences.EmpegPreferences
import com.chasinglemons.empeg.util.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ConnectViewModel : ViewModel(), KoinComponent {

    private val preferences: EmpegPreferences by inject()

    private val _showProgressIndicator = MutableStateFlow(false)
    val showProgressIndicator = _showProgressIndicator.asStateFlow()

    private val _lenColor = MutableStateFlow(Utils.convertStringToColor(preferences.lensColor))
    val lensColor = _lenColor.asStateFlow()

    private val _discoveryFlow = MutableStateFlow<List<Empeg>>(emptyList())
    val discoveryFlow: StateFlow<List<Empeg>> = _discoveryFlow.asStateFlow()

    private var discoveryJob: Job? = null

    fun searchForEmpegs() {
        Timber.d("searchForEmpegs()")
        discoveryJob?.cancel()
        _discoveryFlow.value = emptyList()
        _showProgressIndicator.value = true
        discoveryJob = viewModelScope.launch {
            val servers = try {
                withContext(Dispatchers.IO) {
                    Discoverer(preferences).discoverCancellable()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Discovery failed")
                emptyList()
            }
            _discoveryFlow.value = servers
            if (isActive) {
                _showProgressIndicator.value = false
            }
        }
    }

    fun setPlayer(playerIp: String) {
        preferences.empegIp = playerIp
    }

    fun getCurrentlyHomedPlayerIp(): String {
        return preferences.empegIp
    }

    override fun onCleared() {
        discoveryJob?.cancel()
        super.onCleared()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ConnectViewModel()
            }
        }
    }
}
