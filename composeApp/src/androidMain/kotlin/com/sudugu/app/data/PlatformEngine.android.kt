package com.sudugu.app.data

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

actual val platformEngineFactory: HttpClientEngineFactory<*> = OkHttp
