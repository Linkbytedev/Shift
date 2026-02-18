package com.Linkbyte.Shift

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
// Hilt Application class
class MessengerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
