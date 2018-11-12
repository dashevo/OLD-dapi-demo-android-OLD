package org.dashevo.dapidemo

import android.app.Application

import org.dashevo.dapidemo.dapi.DapiDemoClient

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DapiDemoClient.enablePersistence(filesDir)
    }
}
