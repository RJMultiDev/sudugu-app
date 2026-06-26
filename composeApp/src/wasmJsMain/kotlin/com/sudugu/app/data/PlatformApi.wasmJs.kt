package com.sudugu.app.data

import kotlinx.browser.window

actual fun createApi(): SuduguApiContract {
    // Browser uses same origin by default (developer deploys server and web together).
    return ServerApi(platformEngineFactory, window.location.origin)
}
