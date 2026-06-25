package com.sudugu.app

import android.app.Application
import com.sudugu.app.storage.setAndroidContext

class SuduguApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setAndroidContext(this)
    }
}
