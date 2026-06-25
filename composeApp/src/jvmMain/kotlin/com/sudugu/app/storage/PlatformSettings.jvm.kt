package com.sudugu.app.storage

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual fun createSettings(): Settings {
    val prefs = Preferences.userRoot().node("com.sudugu.app")
    return PreferencesSettings(prefs)
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
