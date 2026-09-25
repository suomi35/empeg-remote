package com.chasinglemons.empeg.di

import com.chasinglemons.empeg.empegapi.EmpegApi
import com.chasinglemons.empeg.empegapi.KtorEmpegApi
import com.chasinglemons.empeg.empegapi.PlaylistXmlParser
import com.chasinglemons.empeg.main.MainViewModel
import com.chasinglemons.empeg.playlist.PlaylistRepository
import com.chasinglemons.empeg.preferences.EmpegPreferences
import com.chasinglemons.empeg.remote.RemoteScreenViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

val appModule = module {

    single {
        HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                // The player is on the LAN; fail fast rather than hanging the UI.
                connectTimeoutMillis = 5.seconds.inWholeMilliseconds
                requestTimeoutMillis = 10.seconds.inWholeMilliseconds
                socketTimeoutMillis = 10.seconds.inWholeMilliseconds
            }
        }
    }

    single<EmpegApi> {
        KtorEmpegApi(
            client = get(),
            hostProvider = { get<EmpegPreferences>().empegIp },
            parser = get()
        )
    }

    single { PlaylistXmlParser() }

    single { PlaylistRepository(get()) }

    single { EmpegPreferences(get()) }

    viewModel { MainViewModel() }

    viewModel { RemoteScreenViewModel() }
}

