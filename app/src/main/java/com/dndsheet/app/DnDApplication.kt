package com.dndsheet.app

import android.app.Application

/**
 * Owns the [AppContainer] for the lifetime of the process. Registered in
 * AndroidManifest under `<application android:name=".DnDApplication">`.
 */
class DnDApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
