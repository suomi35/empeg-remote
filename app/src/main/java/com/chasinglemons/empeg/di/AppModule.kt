package com.chasinglemons.empeg.di

import com.chasinglemons.empeg.preferences.EmpegPreferences
import com.chasinglemons.empeg.main.MainViewModel
import com.chasinglemons.empeg.remote.RemoteScreenViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { EmpegPreferences(get()) }

    viewModel { MainViewModel() }

    viewModel { RemoteScreenViewModel() }
}
