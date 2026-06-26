package com.sudugu.app.data

/**
 * Returns a [SuduguApiContract] for the current platform.
 *
 * All clients use the server-backed [ServerApi] -- the JVM-only jsoup
 * scraper lives in the `server/` module and is exposed at `/api/`. Native
 * apps (Android/iOS/Desktop) ship with a default base URL of
 * `http://10.0.2.2:3001` (Android emulator host loopback) -- production
 * builds can override via env or platform-specific override.
 */
expect fun createApi(): SuduguApiContract

/** Default base URL for the Ktor server (Android emulator host loopback). */
const val defaultApiBaseUrl: String = "http://10.0.2.2:3001"
