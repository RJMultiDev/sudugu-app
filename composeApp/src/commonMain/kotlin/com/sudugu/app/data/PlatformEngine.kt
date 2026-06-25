package com.sudugu.app.data

import io.ktor.client.engine.HttpClientEngineFactory

/**
 * Each platform declares its preferred Ktor engine factory.
 *   - Android  → OkHttp
 *   - iOS      → Darwin (NSURLSession)
 *   - JVM/Desktop → OkHttp
 *   - Wasm/JS → ktor-client-js (browser fetch)
 */
expect val platformEngineFactory: HttpClientEngineFactory<*>
