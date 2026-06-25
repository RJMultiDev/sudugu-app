package com.sudugu.app.ui.nav

import com.sudugu.app.util.Url

/**
 * Static route names for the navigation graph. Each route may take optional
 * arguments — encoded as query-string in the route string and read back
 * via [RouteArgs].
 */
object Routes {
    const val HOME = "home"
    const val CATEGORY = "category"
    const val CATEGORY_DETAIL = "category_detail/{slug}/{name}"
    const val BOOKSHELF = "bookshelf"
    const val PROFILE = "profile"
    const val SEARCH = "search"
    const val NOVEL_DETAIL = "novel_detail/{id}"
    const val READER = "reader/{bookId}/{bookTitle}/{chapterIndex}"
    const val CHAPTER_LIST = "chapter_list/{bookId}/{bookTitle}"
    const val RANKING = "ranking"
    const val READ_HISTORY = "read_history"

    fun categoryDetail(slug: String, name: String): String =
        "category_detail/$slug/${Url.encode(name)}"

    fun novelDetail(id: String): String = "novel_detail/$id"

    fun reader(bookId: String, bookTitle: String, chapterIndex: Int): String =
        "reader/$bookId/${Url.encode(bookTitle)}/$chapterIndex"

    fun chapterList(bookId: String, bookTitle: String): String =
        "chapter_list/$bookId/${Url.encode(bookTitle)}"
}
