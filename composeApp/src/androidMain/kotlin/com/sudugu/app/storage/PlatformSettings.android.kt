package com.sudugu.app.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import android.content.Context
import android.preference.PreferenceManager

private var appContext: Context? = null

fun setAndroidContext(ctx: Context) {
    appContext = ctx
}

actual fun createSettings(): Settings {
    val ctx = appContext ?: error("Android context not initialized. Call setAndroidContext() in Application.onCreate().")
    val sp = PreferenceManager.getDefaultSharedPreferences(ctx)
    return SharedPreferencesSettings(sp)
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
