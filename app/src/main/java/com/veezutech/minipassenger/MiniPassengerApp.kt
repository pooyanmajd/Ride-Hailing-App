package com.veezutech.minipassenger

import android.app.Application
import com.google.android.gms.maps.MapsInitializer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MiniPassengerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST) {}
        }
    }
}