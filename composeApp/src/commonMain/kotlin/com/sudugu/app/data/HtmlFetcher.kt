package com.sudugu.app.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.utils.io.errors.IOException

/**
 * Thin wrapper around Ktor HttpClient for HTML fetches.
 * Encapsulates: User-Agent, timeout, simple in-memory LRU cache, and a
 * shared error type that the scraper can branch on.
 *
 * Cache key = full URL. Caches the most recent 200 entries; older ones evict FIFO.
 */
class HtmlFetcher(
    private val client: HttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    @Volatile
    private val cache = LinkedHashMap<String, String>(MAX_CACHE * 2, 0.75f, true)
    private val cacheLock = Any()

    suspend fun fetch(path: String): String {
        val fullUrl = if (path.startsWith("http")) path else baseUrl + path
        synchronized(cacheLock) {
            cache[fullUrl]?.let { return it }
        }
        val response = client.get(fullUrl)
        if (!response.status.isSuccess()) {
            throw IOException("HTTP ${response.status.value} for $fullUrl")
        }
        val text = response.bodyAsText()
        synchronized(cacheLock) {
            if (cache.size >= MAX_CACHE) {
                val it = cache.entries.iterator()
                if (it.hasNext()) {
                    it.next()
                    it.remove()
                }
            }
            cache[fullUrl] = text
        }
        return text
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://sudugu.org"
        const val MAX_CACHE = 200
    }
}
