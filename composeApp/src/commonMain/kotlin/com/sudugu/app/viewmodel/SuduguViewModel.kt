package com.sudugu.app.viewmodel

import com.sudugu.app.data.SuduguApiContract
import com.sudugu.app.model.*
import com.sudugu.app.storage.LocalStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

data class HomeUiState(
    val loading: Boolean = false,
    val data: HomeData? = null,
    val error: String? = null,
)

data class ListUiState<T>(
    val loading: Boolean = false,
    val items: List<T> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val error: String? = null,
)

data class CategoryListUiState(
    val loading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val error: String? = null,
)

data class DetailUiState(
    val loading: Boolean = false,
    val data: NovelDetail? = null,
    val error: String? = null,
)

data class ChapterUiState(
    val loading: Boolean = false,
    val data: ChapterContent? = null,
    val error: String? = null,
)

data class SearchUiState(
    val keyword: String = "",
    val loading: Boolean = false,
    val results: List<Novel> = emptyList(),
    val error: String? = null,
)

/**
 * Single ViewModel for the whole app. Holds UI state for each screen and
 * delegates network calls to [SuduguApi] and local storage to [LocalStore].
 */
class SuduguViewModel(
    private val api: SuduguApiContract,
    val store: LocalStore,
) : CoroutineScope {

    override val coroutineContext: CoroutineContext =
        Dispatchers.Main + SupervisorJob()

    fun onCleared() {
        coroutineContext.cancel()
        api.close()
    }


    private val _home = MutableStateFlow(HomeUiState())
    val home: StateFlow<HomeUiState> = _home.asStateFlow()

    private val _categories = MutableStateFlow(CategoryListUiState())
    val categories: StateFlow<CategoryListUiState> = _categories.asStateFlow()

    private val _categoryDetail = MutableStateFlow(ListUiState<Novel>())
    val categoryDetail: StateFlow<ListUiState<Novel>> = _categoryDetail.asStateFlow()

    private val _ranking = MutableStateFlow(ListUiState<Novel>())
    val ranking: StateFlow<ListUiState<Novel>> = _ranking.asStateFlow()

    private val _completed = MutableStateFlow(ListUiState<Novel>())
    val completed: StateFlow<ListUiState<Novel>> = _completed.asStateFlow()

    private val _latest = MutableStateFlow(ListUiState<Novel>())
    val latest: StateFlow<ListUiState<Novel>> = _latest.asStateFlow()

    private val _detail = MutableStateFlow(DetailUiState())
    val detail: StateFlow<DetailUiState> = _detail.asStateFlow()

    private val _chapter = MutableStateFlow(ChapterUiState())
    val chapter: StateFlow<ChapterUiState> = _chapter.asStateFlow()

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    // ---- Home ----

    fun loadHome() {
        if (_home.value.loading) return
        _home.value = _home.value.copy(loading = true, error = null)
        launch {
            _home.value = try {
                HomeUiState(loading = false, data = api.home())
            } catch (e: Throwable) {
                HomeUiState(loading = false, error = e.message ?: "加载失败")
            }
        }
    }

    // ---- Categories ----

    fun loadCategories() {
        if (_categories.value.loading) return
        _categories.value = _categories.value.copy(loading = true, error = null)
        launch {
            _categories.value = try {
                CategoryListUiState(loading = false, categories = api.categories())
            } catch (e: Throwable) {
                CategoryListUiState(loading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun loadCategoryDetail(slug: String, page: Int = 1) {
        _categoryDetail.value = _categoryDetail.value.copy(loading = true, page = page, error = null)
        launch {
            _categoryDetail.value = try {
                val d = api.category(slug, page)
                ListUiState(loading = false, items = d.novels, page = d.page, totalPages = d.totalPages)
            } catch (e: Throwable) {
                ListUiState(loading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun loadRanking(page: Int = 1) {
        _ranking.value = _ranking.value.copy(loading = true, page = page)
        launch {
            _ranking.value = try {
                val d = api.ranking(page)
                ListUiState(loading = false, items = d.novels, page = d.page, totalPages = d.totalPages)
            } catch (e: Throwable) {
                ListUiState(loading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun loadCompleted(page: Int = 1) {
        _completed.value = _completed.value.copy(loading = true, page = page)
        launch {
            _completed.value = try {
                val d = api.completed(page)
                ListUiState(loading = false, items = d.novels, page = d.page, totalPages = d.totalPages)
            } catch (e: Throwable) {
                ListUiState(loading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun loadLatest(page: Int = 1) {
        _latest.value = _latest.value.copy(loading = true, page = page)
        launch {
            _latest.value = try {
                val d = api.latest(page)
                ListUiState(loading = false, items = d.novels, page = d.page, totalPages = d.totalPages)
            } catch (e: Throwable) {
                ListUiState(loading = false, error = e.message ?: "加载失败")
            }
        }
    }

    // ---- Novel detail ----

    fun loadDetail(id: String) {
        _detail.value = _detail.value.copy(loading = true, error = null)
        launch {
            _detail.value = try {
                DetailUiState(loading = false, data = api.novelDetail(id))
            } catch (e: Throwable) {
                DetailUiState(loading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun toggleBookshelf(novel: Novel) {
        launch {
            if (store.isInBookshelf(novel.id)) {
                store.removeFromBookshelf(novel.id)
            } else {
                store.addToBookshelf(
                    BookshelfItem(
                        id = novel.id,
                        title = novel.title,
                        author = novel.author,
                        cover = novel.cover,
                        status = novel.status,
                        category = novel.category,
                    )
                )
            }
        }
    }

    fun isInBookshelf(id: String): Boolean = store.isInBookshelf(id)

    fun readProgress(id: String): ReadProgress? = store.getReadProgress(id)

    // ---- Chapter ----

    fun loadChapter(bookId: String, chapterId: String) {
        _chapter.value = _chapter.value.copy(loading = true, error = null)
        launch {
            _chapter.value = try {
                val c = api.chapter(bookId, chapterId)
                // Save progress
                store.saveReadProgress(bookId, chapterId, c.chapterTitle, c.bookTitle)
                ChapterUiState(loading = false, data = c)
            } catch (e: Throwable) {
                ChapterUiState(loading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun saveScrollPosition(bookId: String, chapterId: String, chapterTitle: String, bookTitle: String, scrollY: Float) {
        launch {
            store.saveReadProgress(bookId, chapterId, chapterTitle, bookTitle, scrollY)
        }
    }

    // ---- Search ----

    fun setSearchKeyword(k: String) {
        _search.value = _search.value.copy(keyword = k)
    }

    fun doSearch() {
        val kw = _search.value.keyword
        if (kw.isBlank()) return
        _search.value = _search.value.copy(loading = true, error = null)
        launch {
            _search.value = try {
                val (results, _) = api.search(kw)
                SearchUiState(keyword = kw, loading = false, results = results)
            } catch (e: Throwable) {
                SearchUiState(keyword = kw, loading = false, error = e.message ?: "搜索失败")
            }
        }
    }
}
