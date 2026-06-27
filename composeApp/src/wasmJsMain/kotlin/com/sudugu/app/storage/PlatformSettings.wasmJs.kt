package com.sudugu.app.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import kotlinx.browser.localStorage
import kotlinx.datetime.Clock

actual fun createSettings(): Settings = StorageSettings(localStorage)

actual fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
