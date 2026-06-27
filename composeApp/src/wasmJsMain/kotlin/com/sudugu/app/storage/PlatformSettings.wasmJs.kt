package com.sudugu.app.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import kotlinx.browser.localStorage
import kotlin.js.Date

private val nowMillis: Long get() = Date.now().toLong()

actual fun createSettings(): Settings = StorageSettings(localStorage)

actual fun currentTimeMillis(): Long = nowMillis
