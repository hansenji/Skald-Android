package com.example.absclientapp

import android.app.Application
import com.example.absclientapp.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ABSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@ABSApplication)
            modules(appModule)
        }
    }
}
