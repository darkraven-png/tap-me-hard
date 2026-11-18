package com.tapmehard.app

import android.app.Application
import com.tapmehard.app.data.SettingsDataStore

class TapMeHardApp : Application() {

    lateinit var settingsStore: SettingsDataStore
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsDataStore(this)
    }
}
