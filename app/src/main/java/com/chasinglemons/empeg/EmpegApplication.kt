package com.chasinglemons.empeg

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationManagerCompat
import com.chasinglemons.empeg.di.appModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
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

        createNotificationChannel()

        startKoin {
            androidContext(this@EmpegApplication)
            modules(appModule)
        }
    }

    private fun createNotificationChannel() {
//        val manager = NotificationManagerCompat.from(this)
//        val channel = NotificationChannel(
//            CHANNEL_ID,
//            CHANNEL_NAME,
//            NotificationManager.IMPORTANCE_HIGH
//        )
//        manager.createNotificationChannel(channel)
    }

    companion object {
        val ktorClient = HttpClient(OkHttp)

        lateinit var appInstance: EmpegApplication
    }
}