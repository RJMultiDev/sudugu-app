package com.sudugu.app.model

import kotlinx.serialization.Serializable

// ---- Domain models (mirror the original RN app) ----

@Serializable
data class Novel(
    val id: String,
    val title: String,
    val author: String = "",
    val cover: String = "",
    val status: String = "",
    val category: String = "",
    val words: String = "",
    val description: String = "",
    val lastChapter: ChapterRef? = null,
    val rank: Int? = null,
)

@Serializable
data class ChapterRef(
    val id: String,
    val title: String,
    val time: String? = null,
)

@Serializable
data class Chapter(
    val id: String,
    val title: String,
    val bookId: String,
)

@Serializable
data class ChapterContent(
    val bookId: String,
    val bookTitle: String,
    val chapterId: String,
    val chapterTitle: String,
    val content: String,
    val prevChapter: ChapterRef? = null,
    val nextChapter: ChapterRef? = null,
)

@Serializable
data class Category(
    val slug: String,
    val name: String,
    val count: Int = 0,
)

@Serializable
data class CategoryDetail(
    val novels: List<Novel> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
)

@Serializable
data class HomeData(
    val latestUpdates: List<Novel> = emptyList(),
    val rankings: List<Novel> = emptyList(),
    val completedNovels: List<Novel> = emptyList(),
)

@Serializable
data class TxtLink(
    val url: String,
    val label: String,
)

@Serializable
data class NovelDetail(
    val id: String,
    val title: String,
    val author: String = "",
    val cover: String = "",
    val status: String = "",
    val category: String = "",
    val words: String = "",
    val description: String = "",
    val chapters: List<Chapter> = emptyList(),
    val txtLinks: List<TxtLink> = emptyList(),
)

// ---- Local storage models ----

@Serializable
data class BookshelfItem(
    val id: String,
    val title: String,
    val author: String = "",
    val cover: String = "",
    val status: String = "",
    val category: String = "",
    val addedAt: Long = 0L,
)

@Serializable
data class ReadProgress(
    val bookId: String,
    val chapterId: String,
    val chapterTitle: String,
    val bookTitle: String,
    val timestamp: Long,
    val scrollY: Float = 0f,
)

enum class ThemeMode { LIGHT, DARK }

enum class ReaderFontSize(val key: String, val size: Int) {
    SMALL("small", 16),
    MEDIUM("medium", 18),
    LARGE("large", 20),
    XLARGE("xlarge", 22);

    companion object {
        fun fromKey(key: String?): ReaderFontSize =
            entries.firstOrNull { it.key == key } ?: MEDIUM
    }
}
