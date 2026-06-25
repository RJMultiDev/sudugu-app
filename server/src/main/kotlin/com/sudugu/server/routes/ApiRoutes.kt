package com.sudugu.server.routes

import com.sudugu.server.scraper.Scraper
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.apiRoutes(scraper: Scraper) {
    route("/api") {
        get("/home") {
            val data = scraper.scrapeHome()
            call.respond(data)
        }
        get("/categories") {
            call.respond(scraper.scrapeCategories())
        }
        get("/category/{slug}") {
            val slug = call.parameters["slug"].orEmpty()
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            call.respond(scraper.scrapeCategory(slug, page))
        }
        get("/ranking") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            call.respond(scraper.scrapeRanking(page))
        }
        get("/completed") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            call.respond(scraper.scrapeCompleted(page))
        }
        get("/latest") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            call.respond(scraper.scrapeLatest(page))
        }
        get("/search") {
            val kw = call.request.queryParameters["keyword"].orEmpty()
            if (kw.isBlank()) {
                call.respond(SearchResponse(novels = emptyList(), keyword = ""))
                return@get
            }
            val (novels, keyword) = scraper.scrapeSearch(kw)
            call.respond(SearchResponse(novels, keyword))
        }
        get("/novel/{id}") {
            val id = call.parameters["id"].orEmpty()
            call.respond(scraper.scrapeNovelDetail(id))
        }
        get("/chapter/{bookId}/{chapterId}") {
            val bookId = call.parameters["bookId"].orEmpty()
            val chapterId = call.parameters["chapterId"].orEmpty()
            call.respond(scraper.scrapeChapter(bookId, chapterId))
        }
        get("/author") {
            val tag = call.request.queryParameters["tag"].orEmpty()
            if (tag.isBlank()) {
                call.respond(SearchResponse(novels = emptyList(), keyword = ""))
                return@get
            }
            val (novels, author) = scraper.scrapeAuthorNovels(tag)
            call.respond(SearchResponse(novels, author))
        }
    }
}

@kotlinx.serialization.Serializable
data class SearchResponse(
    val novels: List<com.sudugu.server.scraper.Novel> = emptyList(),
    val keyword: String = "",
)
