package com.chasinglemons.empeg.discovery

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chasinglemons.empeg.model.Empeg
import com.chasinglemons.empeg.preferences.EmpegPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class DiscoveryViewModel: ViewModel(), KoinComponent, Discoverer.DiscoveryReceiver {

    private val preferences: EmpegPreferences by inject()

    private val _showProgressIndicator = MutableStateFlow(false)
    val showProgressIndicator = _showProgressIndicator.asStateFlow()

    private var empeg = mutableStateListOf<Empeg>()
    private val _discoveryFlow = MutableStateFlow(empeg)
    val discoveryFlow: StateFlow<List<Empeg>> get() = _discoveryFlow

//    init {
//        Timber.d(">>> init")
//        searchForEmpegs()
//    }

    fun searchForEmpegs() {
        Timber.d(">>> searchForEmpegs()")
        _showProgressIndicator.value = true
        Discoverer(this, preferences).start()
    }

    fun setPlayer(playerIp: String) {
        preferences.empegIp = playerIp
    }

    fun getCurrentlyHomedPlayerIp(): String {
        return preferences.empegIp
    }

    override fun addAnnouncedServers(servers: ArrayList<Empeg>?) {
        _discoveryFlow.value = mutableStateListOf(*servers!!.toTypedArray())
        _showProgressIndicator.value = false
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DiscoveryViewModel()
            }
        }
    }
}