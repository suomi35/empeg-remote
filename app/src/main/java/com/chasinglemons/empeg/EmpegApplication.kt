package com.chasinglemons.empeg

import android.app.Application
import com.chasinglemons.empeg.di.appModule
import com.chasinglemons.empeg.notification.PlayerNotification
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber
import java.util.concurrent.TimeUnit

class EmpegApplication : Application() {

    init {
        appInstance = this
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        PlayerNotification.createChannel(this)

        startKoin {
            androidContext(this@EmpegApplication)
            modules(appModule)
        }
    }

    companion object {
        val ktorClient = HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(10, TimeUnit.SECONDS)
                    readTimeout(15, TimeUnit.SECONDS)
                    writeTimeout(15, TimeUnit.SECONDS)
                    callTimeout(20, TimeUnit.SECONDS)
                }
            }
        }

        lateinit var appInstance: EmpegApplication
    }
}
