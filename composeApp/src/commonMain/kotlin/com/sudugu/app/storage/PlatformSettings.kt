package com.sudugu.app.storage

import com.russhwolf.settings.Settings

/**
 * Per-platform settings factory. Native platforms use the platform-native
 * settings; web uses a thin wrapper over window.localStorage.
 */
expect fun createSettings(): Settings
