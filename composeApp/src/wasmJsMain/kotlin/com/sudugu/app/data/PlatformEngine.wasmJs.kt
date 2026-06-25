package com.sudugu.app.data

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js

actual val platformEngineFactory: HttpClientEngineFactory<*> = Js
