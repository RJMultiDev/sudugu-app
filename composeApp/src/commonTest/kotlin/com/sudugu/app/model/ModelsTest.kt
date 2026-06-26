package com.sudugu.app.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ModelsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun `serializes and deserializes Novel`() {
        val original = Novel(
            id = "100",
            title = "仙工开物",
            author = "蛊真人",
            cover = "https://example.com/cover.jpg",
            status = "连载中",
            category = "仙侠小说",
            words = "945 万字",
            lastChapter = ChapterRef(id = "4029839", title = "第944章", time = "8秒前"),
            rank = 2,
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Novel>(encoded)
        assertEquals(original.id, decoded.id)
        assertEquals(original.title, decoded.title)
        assertEquals(original.lastChapter?.id, decoded.lastChapter?.id)
        assertEquals(original.rank, decoded.rank)
    }

    @Test fun `serializes ChapterContent with prev and next`() {
        val c = ChapterContent(
            bookId = "100",
            bookTitle = "仙工开物",
            chapterId = "4029839",
            chapterTitle = "第944章",
            content = "正文段落 1\n\n正文段落 2",
            prevChapter = ChapterRef(id = "4021578", title = "上一章"),
            nextChapter = ChapterRef(id = "4029839-2", title = "下一页"),
        )
        val encoded = json.encodeToString(c)
        val decoded = json.decodeFromString<ChapterContent>(encoded)
        assertEquals(c.content, decoded.content)
        assertEquals(c.nextChapter?.id, decoded.nextChapter?.id)
    }

    @Test fun `ReaderFontSize fromKey falls back to medium`() {
        assertEquals(ReaderFontSize.MEDIUM, ReaderFontSize.fromKey(null))
        assertEquals(ReaderFontSize.MEDIUM, ReaderFontSize.fromKey("unknown"))
        assertEquals(ReaderFontSize.LARGE, ReaderFontSize.fromKey("large"))
        assertEquals(ReaderFontSize.XLARGE, ReaderFontSize.fromKey("xlarge"))
    }
}
