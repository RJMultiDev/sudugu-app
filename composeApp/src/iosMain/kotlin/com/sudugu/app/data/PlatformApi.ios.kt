package com.sudugu.app.data

actual fun createApi(): SuduguApiContract =
    ServerApi(platformEngineFactory, defaultApiBaseUrl)
