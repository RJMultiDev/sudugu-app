package com.sudugu.server.scraper

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

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
data class ChapterRef(val id: String, val title: String, val time: String? = null)

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
data class Category(val slug: String, val name: String, val count: Int = 0)

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
data class TxtLink(val url: String, val label: String)

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

private object AdFilter {
    private val symbolChar = Regex("""[?？,。.、…!！_+\-=*·•・]""")
    private val symbolOnly = Regex("""^[\s?？,。.、…!！_+\-=*·•・]+$""")
    private val repeated = Regex("""[?？,。.]{3,}""")
    private val adKeyword = Regex(
        """速读谷|更多小说|点击阅读|最新热门|打开APP|下载APP|下载app|关注我们|二维码|扫码加群|书友群|官方群|企鹅|微信""",
        RegexOption.IGNORE_CASE,
    )
    private val tag = Regex("""<[^>]+>""")
    private val ent = Regex("""&[a-z]+;|&#\d+;""")

    fun isAd(text: String): Boolean {
        if (text.isBlank() || text.length < 4) return true
        if (symbolOnly.matches(text)) return true
        val symbolCount = symbolChar.findAll(text).count()
        if (symbolCount * 2 > text.length) return true
        if (repeated.containsMatchIn(text)) return true
        if (adKeyword.containsMatchIn(text)) return true
        if (tag.containsMatchIn(text)) return true
        if (ent.containsMatchIn(text)) return true
        return false
    }

    fun trim(text: String): String = text.replace(tag, "").trim()
}

class Scraper {
    private val baseUrl = "https://sudugu.org"
    private val userAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    private val cache = LinkedHashMap<String, String>(500, 0.75f, true)
    private val cacheLock = Any()

    private fun fetch(path: String): String {
        val url = if (path.startsWith("http")) path else baseUrl + path
        synchronized(cacheLock) {
            cache[url]?.let { return it }
        }
        val req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(15))
            .header("User-Agent", userAgent)
            .GET()
            .build()
        val resp = client.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() !in 200..299) {
            throw IllegalStateException("HTTP ${resp.statusCode()} for $url")
        }
        val text = resp.body()
        synchronized(cacheLock) {
            if (cache.size >= 500) {
                val it = cache.entries.iterator()
                if (it.hasNext()) {
                    it.next()
                    it.remove()
                }
            }
            cache[url] = text
        }
        return text
    }

    // ---- Homepage ----
    fun scrapeHome(): HomeData {
        val doc = Jsoup.parse(fetch("/"))
        val latest = doc.select(".container").getOrNull(0)?.select(".item")
            ?.mapNotNull { parseNovelItem(it, includeChapters = false) }
            .orEmpty()
        val rankings = doc.select(".container").getOrNull(1)?.select(".list.top li")
            ?.mapIndexedNotNull { idx, el ->
                val title = el.selectFirst("p a")?.text()?.trim().orEmpty()
                val href = el.selectFirst(".imga")?.attr("href").orEmpty()
                val id = href.replace("/", "").substringBefore("?")
                val cover = normalizeCover(el.selectFirst("img")?.attr("src").orEmpty())
                if (id.isBlank() || title.isBlank()) null
                else Novel(id = id, title = title, cover = cover, rank = idx + 1)
            }
            .orEmpty()
        val completed = doc.select(".container").getOrNull(2)?.select(".list.top li")
            ?.mapNotNull { el ->
                val title = el.selectFirst("p a")?.text()?.trim().orEmpty()
                val href = el.selectFirst(".imga")?.attr("href").orEmpty()
                val id = href.replace("/", "")
                val cover = normalizeCover(el.selectFirst("img")?.attr("src").orEmpty())
                if (id.isBlank() || title.isBlank()) null
                else Novel(id = id, title = title, cover = cover)
            }
            .orEmpty()
        return HomeData(latestUpdates = latest, rankings = rankings, completedNovels = completed)
    }

    // ---- Categories ----
    fun scrapeCategories(): List<Category> {
        val doc = Jsoup.parse(fetch("/fenlei/"))
        return doc.select(".fenlei li a").mapNotNull { el ->
            val text = el.text().trim()
            val href = el.attr("href")
            val match = Regex("""^(.+)\((\d+)部\)$""").matchEntire(text) ?: return@mapNotNull null
            Category(
                slug = href.replace("/", ""),
                name = match.groupValues[1],
                count = match.groupValues[2].toIntOrNull() ?: 0,
            )
        }
    }

    // ---- Category (paginated) ----
    fun scrapeCategory(slug: String, page: Int = 1): CategoryDetail {
        val path = if (page > 1) "/$slug/$page.html" else "/$slug/"
        val doc = Jsoup.parse(fetch(path))
        val items = doc.select(".item").mapNotNull { parseNovelItem(it, includeChapters = true) }
        return CategoryDetail(items, page, parsePagination(doc))
    }

    fun scrapeRanking(page: Int = 1): CategoryDetail {
        val path = if (page > 1) "/paihang/$page.html" else "/paihang/"
        val doc = Jsoup.parse(fetch(path))
        val items = doc.select(".item").mapIndexedNotNull { idx, el ->
            parseNovelItem(el, includeChapters = false)?.copy(rank = idx + 1 + (page - 1) * 10)
        }
        return CategoryDetail(items, page, parsePagination(doc))
    }

    fun scrapeCompleted(page: Int = 1): CategoryDetail {
        val path = if (page > 1) "/wanjie/$page.html" else "/wanjie/"
        val doc = Jsoup.parse(fetch(path))
        val items = doc.select(".item").mapNotNull { parseNovelItem(it, includeChapters = true) }
        return CategoryDetail(items, page, parsePagination(doc))
    }

    fun scrapeLatest(page: Int = 1): CategoryDetail {
        val path = if (page > 1) "/zuixin/$page.html" else "/zuixin/"
        val doc = Jsoup.parse(fetch(path))
        val items = doc.select(".item").mapNotNull { parseNovelItem(it, includeChapters = false) }
        return CategoryDetail(items, page, parsePagination(doc))
    }

    fun scrapeSearch(keyword: String): Pair<List<Novel>, String> {
        if (keyword.isBlank()) return emptyList<Novel>() to keyword
        val doc = Jsoup.parse(fetch("/i/sor.aspx?key=${urlEncode(keyword)}"))
        val items = doc.select(".item").mapNotNull { parseNovelItem(it, includeChapters = true) }
        return items to keyword
    }

    fun scrapeAuthorNovels(author: String): Pair<List<Novel>, String> {
        if (author.isBlank()) return emptyList<Novel>() to author
        val doc = Jsoup.parse(fetch("/zuozhe/?tag=${urlEncode(author)}"))
        val items = doc.select(".item").mapNotNull { parseNovelItem(it, includeChapters = true) }
        return items to author
    }

    // ---- Novel detail ----
    fun scrapeNovelDetail(bookId: String): NovelDetail {
        val doc = Jsoup.parse(fetch("/$bookId/"))
        val title = doc.selectFirst(".itemtxt h1 a, .itemtxt h3 a")?.text()?.trim().orEmpty()
        val cover = normalizeCover(doc.selectFirst(".item img")?.attr("src").orEmpty())
        val status = doc.select(".itemtxt p span").getOrNull(0)?.text()?.trim().orEmpty()
        val category = doc.select(".itemtxt p span").getOrNull(1)?.text()?.trim().orEmpty()
        val author = Regex("""作者[：:]\s*(\S+)""").find(doc.text())?.groupValues?.get(1).orEmpty()
        val words = doc.selectFirst(".itemtxt h1 i, .itemtxt h3 i")?.text()?.trim().orEmpty()
        val description = doc.selectFirst(".des.bb")?.text()?.trim().orEmpty()
        val chapters = doc.select("#list a").mapNotNull { el ->
            val href = el.attr("href")
            val id = Regex("""/(\d+)\.html""").find(href)?.groupValues?.get(1) ?: return@mapNotNull null
            val t = el.text().trim()
            if (t.isBlank()) null else Chapter(id = id, title = t, bookId = bookId)
        }
        val txtLinks = runCatching {
            val txtDoc = Jsoup.parse(fetch("/$bookId/txt.html"))
            txtDoc.select("#list a").mapNotNull { el ->
                val href = el.attr("href")
                val label = el.text().trim()
                if (href.isBlank() || label.isBlank()) null
                else TxtLink(
                    url = if (href.startsWith("http")) href else "$baseUrl$href",
                    label = label,
                )
            }
        }.getOrDefault(emptyList())
        return NovelDetail(
            id = bookId, title = title, author = author, cover = cover,
            status = status, category = category, words = words,
            description = description, chapters = chapters, txtLinks = txtLinks,
        )
    }

    // ---- Chapter (with ad filter + multi-page) ----
    fun scrapeChapter(bookId: String, chapterId: String): ChapterContent {
        val allParagraphs = mutableListOf<String>()
        var currentUrl: String? = "/$bookId/$chapterId.html"
        var bookTitle = ""
        var chapterTitle = ""
        var prev: ChapterRef? = null
        var next: ChapterRef? = null
        val seenHashes = mutableSetOf<Int>()
        var page = 0

        while (currentUrl != null && page < 5) {
            page++
            val html = runCatching { fetch(currentUrl) }.getOrNull() ?: break
            if (!seenHashes.add(html.hashCode())) break
            val doc = Jsoup.parse(html)
            if (page == 1) {
                val h1 = doc.selectFirst(".submenu h1")
                if (h1 != null) {
                    bookTitle = h1.selectFirst("a")?.text()?.trim().orEmpty()
                    chapterTitle = h1.text().replace(bookTitle, "").replace(">", "").trim()
                }
                doc.selectFirst(".prenext")?.let { pn ->
                    val spans = pn.select("span")
                    prev = parseChapterLink(spans.getOrNull(0)?.selectFirst("a"))
                    next = parseChapterLink(spans.lastOrNull()?.selectFirst("a"))
                }
            }
            val con = doc.selectFirst(".con")
            if (con != null) {
                con.select("p").forEach { p ->
                    val raw = AdFilter.trim(p.text())
                    if (!AdFilter.isAd(raw)) {
                        allParagraphs.add(raw)
                    }
                }
            }
            val nextPageHref = doc.selectFirst(".prenext")
                ?.select("a[href]")?.toList()
                ?.lastOrNull { a ->
                    Regex("""^/$bookId/$chapterId-\d+\.html$""").matches(a.attr("href"))
                }
                ?.attr("href")
            currentUrl = nextPageHref
        }
        return ChapterContent(
            bookId = bookId, bookTitle = bookTitle, chapterId = chapterId,
            chapterTitle = chapterTitle, content = allParagraphs.joinToString("\n\n"),
            prevChapter = prev, nextChapter = next,
        )
    }

    // ---- Helpers ----
    private fun parseNovelItem(el: Element, includeChapters: Boolean): Novel? {
        val link = el.selectFirst(".itemtxt h1 a, .itemtxt h3 a") ?: return null
        val title = link.text().trim()
        val href = link.attr("href")
        val id = href.replace("/", "")
        if (id.isBlank() || title.isBlank()) return null
        val cover = normalizeCover(el.selectFirst("img")?.attr("src").orEmpty())
        val fullText = el.selectFirst(".itemtxt")?.text().orEmpty()
        val author = Regex("""作者[：:]\s*(\S+)""").find(fullText)?.groupValues?.get(1).orEmpty()
        val status = el.select(".itemtxt p span").getOrNull(0)?.text()?.trim().orEmpty()
        val category = el.select(".itemtxt p span").getOrNull(1)?.text()?.trim().orEmpty()
        val words = el.selectFirst(".itemtxt h1 i, .itemtxt h3 i")?.text()?.trim().orEmpty()
        var lastChapter: ChapterRef? = null
        if (includeChapters) {
            val firstLi = el.selectFirst(".itemtxt ul li")
            if (firstLi != null) {
                val time = firstLi.selectFirst("i")?.text()?.trim()
                val a = firstLi.selectFirst("a")
                val chHref = a?.attr("href").orEmpty()
                val chId = Regex("""/(\d+)\.html""").find(chHref)?.groupValues?.get(1).orEmpty()
                val chTitle = a?.text()?.trim().orEmpty()
                if (chId.isNotBlank() && chTitle.isNotBlank()) {
                    lastChapter = ChapterRef(id = chId, title = chTitle, time = time)
                }
            }
        }
        return Novel(
            id = id, title = title, author = author, cover = cover,
            status = status, category = category, words = words, lastChapter = lastChapter,
        )
    }

    private fun parseChapterLink(a: Element?): ChapterRef? {
        if (a == null) return null
        val href = a.attr("href")
        val id = Regex("""/(\d+)\.html""").find(href)?.groupValues?.get(1).orEmpty()
        if (id.isBlank()) return null
        return ChapterRef(id = id, title = a.text().trim())
    }

    private fun parsePagination(doc: Document): Int {
        val text = doc.selectFirst(".page")?.text().orEmpty()
        val nums = Regex("""\d+""").findAll(text).toList()
        return nums.lastOrNull()?.value?.toIntOrNull() ?: 1
    }

    private fun normalizeCover(src: String): String = when {
        src.isBlank() -> ""
        src.startsWith("http") -> src
        src.startsWith("//") -> "https:$src"
        else -> "$baseUrl$src"
    }

    private fun urlEncode(s: String): String {
        val sb = StringBuilder()
        for (b in s.toByteArray(Charsets.UTF_8)) {
            val c = b.toInt() and 0xff
            if ((c in 'a'.code..'z'.code) ||
                (c in 'A'.code..'Z'.code) ||
                (c in '0'.code..'9'.code) ||
                c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code
            ) {
                sb.append(c.toChar())
            } else {
                sb.append('%')
                sb.append(((c ushr 4) and 0xf).toString(16).uppercase())
                sb.append((c and 0xf).toString(16).uppercase())
            }
        }
        return sb.toString()
    }
}
