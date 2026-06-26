package com.sudugu.app.data

actual fun createApi(): SuduguApiContract =
    ServerApi(platformEngineFactory, "http://localhost:3001")
