package com.yousefelsayed.travelbuddy.di

import android.app.Application
import com.yousefelsayed.travelbuddy.di.module.networkModule
import com.yousefelsayed.travelbuddy.di.module.repositoryModule
import com.yousefelsayed.travelbuddy.di.module.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(listOf(networkModule,repositoryModule,viewModelModule))
        }
    }
}