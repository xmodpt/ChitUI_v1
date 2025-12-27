package com.chitui.client

import android.app.Application
import com.chitui.client.util.NetworkModule
import timber.log.Timber

class ChitUIApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Network Module
        NetworkModule.initialize(this)

        Timber.d("ChitUI Client Application started")
    }
}
