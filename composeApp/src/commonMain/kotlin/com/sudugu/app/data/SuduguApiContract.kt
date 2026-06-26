package com.sudugu.app.data

import com.sudugu.app.model.*

/**
 * Common interface so the UI/VM can be platform-agnostic. The actual
 * implementation depends on the platform:
 *   - Android / iOS / Desktop → [SuduguApi] (direct fetch + Jsoup)
 *   - Web (Wasm/JS) → [ServerApi] (talks to the Ktor server because browsers
 *     can't bypass CORS for sudugu.org)
 */
interface SuduguApiContract : AutoCloseable {
    suspend fun home(): HomeData
    suspend fun categories(): List<Category>
    suspend fun category(slug: String, page: Int = 1): CategoryDetail
    suspend fun ranking(page: Int = 1): CategoryDetail
    suspend fun completed(page: Int = 1): CategoryDetail
    suspend fun latest(page: Int = 1): CategoryDetail
    suspend fun search(keyword: String): Pair<List<Novel>, String>
    suspend fun novelDetail(id: String): NovelDetail
    suspend fun chapter(bookId: String, chapterId: String): ChapterContent
    suspend fun authorNovels(author: String): Pair<List<Novel>, String>

    override fun close() {}
}
