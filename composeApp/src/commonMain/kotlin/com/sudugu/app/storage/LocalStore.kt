package com.sudugu.app.storage

import com.russhwolf.settings.Settings
import com.sudugu.app.model.BookshelfItem
import com.sudugu.app.model.ReadProgress
import com.sudugu.app.model.ReaderFontSize
import com.sudugu.app.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Key-value storage backed by multiplatform-settings. The concrete `Settings`
 * instance is provided per platform (expectAndroidContext, expectDarwin,
 * expectDataStore, expectLocalStorage, …).
 */
class LocalStore(private val settings: Settings) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _bookshelf = MutableStateFlow<List<BookshelfItem>>(emptyList())
    val bookshelfFlow: StateFlow<List<BookshelfItem>> = _bookshelf.asStateFlow()

    private val _history = MutableStateFlow<List<ReadProgress>>(emptyList())
    val historyFlow: StateFlow<List<ReadProgress>> = _history.asStateFlow()

    private val _theme = MutableStateFlow(ThemeMode.LIGHT)
    val themeFlow: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _readerFontSize = MutableStateFlow(ReaderFontSize.MEDIUM)
    val readerFontSizeFlow: StateFlow<ReaderFontSize> = _readerFontSize.asStateFlow()

    fun loadAll() {
        _bookshelf.value = readList(KEY_BOOKSHELF)
        _history.value = readList(KEY_READ_HISTORY)
        _theme.value = ThemeMode.entries.firstOrNull { it.name == settings.getString(KEY_THEME, "LIGHT") }
            ?: ThemeMode.LIGHT
        _readerFontSize.value = ReaderFontSize.fromKey(settings.getString(KEY_FONT_SIZE, "medium"))
    }

    // ---- Bookshelf ----

    fun getBookshelf(): List<BookshelfItem> = _bookshelf.value

    suspend fun addToBookshelf(book: BookshelfItem) {
        val current = _bookshelf.value
        if (current.any { it.id == book.id }) return
        val updated = listOf(book.copy(addedAt = currentTimeMillis())) + current
        writeList(KEY_BOOKSHELF, updated)
        _bookshelf.value = updated
    }

    suspend fun removeFromBookshelf(bookId: String) {
        val updated = _bookshelf.value.filter { it.id != bookId }
        writeList(KEY_BOOKSHELF, updated)
        _bookshelf.value = updated
    }

    fun isInBookshelf(bookId: String): Boolean = _bookshelf.value.any { it.id == bookId }

    // ---- Read progress ----

    fun getReadProgress(bookId: String): ReadProgress? =
        readOrNull<ReadProgress>(keyProgress(bookId))

    fun getReadHistory(): List<ReadProgress> = _history.value

    suspend fun saveReadProgress(
        bookId: String,
        chapterId: String,
        chapterTitle: String,
        bookTitle: String,
        scrollY: Float = 0f,
    ) {
        val existing = getReadProgress(bookId)
        val progress = ReadProgress(
            bookId = bookId,
            chapterId = chapterId,
            chapterTitle = chapterTitle,
            bookTitle = bookTitle,
            timestamp = currentTimeMillis(),
            scrollY = if (scrollY > 0f) scrollY else existing?.scrollY ?: 0f,
        )
        writeObject(keyProgress(bookId), progress)
        val newHistory = (listOf(progress) + _history.value.filter { it.bookId != bookId }).take(50)
        writeList(KEY_READ_HISTORY, newHistory)
        _history.value = newHistory
    }

    // ---- Theme ----

    fun setThemeMode(mode: ThemeMode) {
        settings.putString(KEY_THEME, mode.name)
        _theme.value = mode
    }

    // ---- Font size ----

    fun setReaderFontSize(size: ReaderFontSize) {
        settings.putString(KEY_FONT_SIZE, size.key)
        _readerFontSize.value = size
    }

    // ---- Internals ----

    private inline fun <reified T> readOrNull(key: String): T? =
        settings.getStringOrNull(key)?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() }

    private inline fun <reified T> readList(key: String): List<T> =
        settings.getStringOrNull(key)
            ?.let { runCatching { json.decodeFromString<List<T>>(it) }.getOrNull() }
            ?: emptyList()

    private inline fun <reified T> writeList(key: String, value: List<T>) {
        settings.putString(key, json.encodeToString(value))
    }

    private inline fun <reified T> writeObject(key: String, value: T) {
        settings.putString(key, json.encodeToString(value))
    }

    private fun keyProgress(bookId: String) = "${KEY_READ_PROGRESS_PREFIX}_$bookId"

    companion object {
        private const val KEY_BOOKSHELF = "@sudugu/bookshelf"
        private const val KEY_READ_HISTORY = "@sudugu/read_history"
        private const val KEY_THEME = "@sudugu/theme"
        private const val KEY_FONT_SIZE = "@sudugu/font_size"
        private const val KEY_READ_PROGRESS_PREFIX = "@sudugu/reading_progress"
    }
}

/** Platform-specific current time. */
expect fun currentTimeMillis(): Long
