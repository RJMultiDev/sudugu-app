package com.sudugu.app.data

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual val platformEngineFactory: HttpClientEngineFactory<*> = Darwin
