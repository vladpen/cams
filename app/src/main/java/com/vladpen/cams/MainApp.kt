package com.vladpen.cams

import android.app.Application
import android.content.Context

class MainApp : Application() {
    init {
        instance = this
    }

    companion object {
        lateinit var instance: MainApp
        val context: Context get() {
            return instance.applicationContext
        }
    }
}