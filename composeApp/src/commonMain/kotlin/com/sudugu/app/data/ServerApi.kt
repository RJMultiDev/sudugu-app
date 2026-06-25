package com.sudugu.app.data

import com.sudugu.app.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.statement.bodyAsText

/**
 * Calls the optional Kotlin/Ktor server (see ../server). Used by the web
 * (wasmJs) build where browsers cannot fetch sudugu.org directly due to CORS.
 *
 * On native platforms the app uses [SuduguApi] (direct scrape) and ignores
 * this class unless [baseUrl] is set explicitly.
 */
class ServerApi(
    engineFactory: HttpClientEngineFactory<*>,
    private val baseUrl: String,
) {
    private val client: HttpClient = HttpClient(engineFactory) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun home(): HomeData = client.get("$baseUrl/api/home").body()
    suspend fun categories(): List<Category> = client.get("$baseUrl/api/categories").body()
    suspend fun category(slug: String, page: Int = 1): CategoryDetail =
        client.get("$baseUrl/api/category/$slug") { parameter("page", page) }.body()
    suspend fun ranking(page: Int = 1): CategoryDetail =
        client.get("$baseUrl/api/ranking") { parameter("page", page) }.body()
    suspend fun completed(page: Int = 1): CategoryDetail =
        client.get("$baseUrl/api/completed") { parameter("page", page) }.body()
    suspend fun latest(page: Int = 1): CategoryDetail =
        client.get("$baseUrl/api/latest") { parameter("page", page) }.body()
    suspend fun search(keyword: String): SearchResult =
        client.get("$baseUrl/api/search") { parameter("keyword", keyword) }.body()
    suspend fun novelDetail(id: String): NovelDetail = client.get("$baseUrl/api/novel/$id").body()
    suspend fun chapter(bookId: String, chapterId: String): ChapterContent =
        client.get("$baseUrl/api/chapter/$bookId/$chapterId").body()
    suspend fun author(author: String): SearchResult =
        client.get("$baseUrl/api/author") { parameter("tag", author) }.body()

    fun close() = client.close()

    @kotlinx.serialization.Serializable
    data class SearchResult(val novels: List<Novel> = emptyList(), val keyword: String = "")
}
