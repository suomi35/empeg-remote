package com.chasinglemons.empeg

import android.app.Application
import com.chasinglemons.empeg.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber.DebugTree
import timber.log.Timber.Forest.plant

class EmpegApplication: Application() {

    init {
        appInstance = this
    }

    override fun onCreate() {
        super.onCreate()

        plant(DebugTree())

        startKoin {
            androidContext(this@EmpegApplication)
            modules(appModule)
        }
    }

    companion object {
        lateinit var appInstance: EmpegApplication
    }
}