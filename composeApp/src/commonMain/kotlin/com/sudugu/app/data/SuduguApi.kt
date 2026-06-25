package com.sudugu.app.data

import com.sudugu.app.model.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * High-level API. On every platform it talks to sudugu.org directly via the
 * client-side scraper. The optional Ktor server is used for web (wasmJs) where
 * the browser cannot bypass CORS — see [WebApi].
 */
class SuduguApi(
    engineFactory: HttpClientEngineFactory<*>,
    baseUrl: String = HtmlFetcher.DEFAULT_BASE_URL,
) {
    private val client: HttpClient = HttpClient(engineFactory) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(UserAgent) {
            agent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        install(Logging) {
            level = LogLevel.NONE
        }
    }

    private val fetcher = HtmlFetcher(client, baseUrl)
    private val scraper = Scraper(fetcher, baseUrl)

    suspend fun home(): HomeData = scraper.scrapeHome()
    suspend fun categories(): List<Category> = scraper.scrapeCategories()
    suspend fun category(slug: String, page: Int = 1): CategoryDetail = scraper.scrapeCategory(slug, page)
    suspend fun ranking(page: Int = 1): CategoryDetail = scraper.scrapeRanking(page)
    suspend fun completed(page: Int = 1): CategoryDetail = scraper.scrapeCompleted(page)
    suspend fun latest(page: Int = 1): CategoryDetail = scraper.scrapeLatest(page)
    suspend fun search(keyword: String): Pair<List<Novel>, String> = scraper.scrapeSearch(keyword)
    suspend fun novelDetail(id: String): NovelDetail = scraper.scrapeNovelDetail(id)
    suspend fun chapter(bookId: String, chapterId: String): ChapterContent = scraper.scrapeChapter(bookId, chapterId)
    suspend fun authorNovels(author: String): Pair<List<Novel>, String> = scraper.scrapeAuthorNovels(author)

    fun close() = client.close()
}
