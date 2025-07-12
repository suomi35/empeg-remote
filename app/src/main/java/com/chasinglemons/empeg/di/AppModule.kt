package com.chasinglemons.empeg.di

import com.chasinglemons.empeg.preferences.EmpegPreferences
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { EmpegPreferences(get()) }
}
