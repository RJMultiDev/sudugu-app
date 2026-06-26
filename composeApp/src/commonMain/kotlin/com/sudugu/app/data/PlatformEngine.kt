package com.sudugu.app.data

import io.ktor.client.engine.HttpClientEngineFactory

/** Each platform declares its preferred Ktor engine factory. */
expect val platformEngineFactory: HttpClientEngineFactory<*>
