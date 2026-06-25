package com.sudugu.app.data

import com.sudugu.app.model.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

/**
 * Jsoup-based scraper. Mirrors the original cheerio scraper (server/scraper.ts)
 * plus the ad-filter + multi-page chapter logic from the RN client.
 *
 * Pure data layer — no Compose, no platform dependencies. Safe to call from
 * any source set (common, android, ios, jvm, wasmJs).
 */
class Scraper(
    private val fetcher: HtmlFetcher,
    private val baseUrl: String = HtmlFetcher.DEFAULT_BASE_URL,
) {

    // ---- Homepage ----

    suspend fun scrapeHome(): HomeData {
        val html = fetcher.fetch("/")
        val doc = Jsoup.parse(html)

        val latestUpdates = parseListItems(doc, ".container:nth-of-type(1) .item", includeChapters = false)
        val rankings = parseTopList(doc, ".container:nth-of-type(2) .list.top li")
        val completed = parseTopListNovels(doc, ".container:nth-of-type(3) .list.top li")

        return HomeData(latestUpdates, rankings, completed)
    }

    // ---- Categories ----

    suspend fun scrapeCategories(): List<Category> {
        val html = fetcher.fetch("/fenlei/")
        val doc = Jsoup.parse(html)
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

    // ---- Category novels (paginated) ----

    suspend fun scrapeCategory(category: String, page: Int = 1): CategoryDetail {
        val path = if (page > 1) "/$category/$page.html" else "/$category/"
        val html = fetcher.fetch(path)
        val doc = Jsoup.parse(html)
        val novels = parseListItems(doc, ".item", includeChapters = true)
        return CategoryDetail(novels, page, parsePagination(doc))
    }

    // ---- Ranking ----

    suspend fun scrapeRanking(page: Int = 1): CategoryDetail {
        val path = if (page > 1) "/paihang/$page.html" else "/paihang/"
        val html = fetcher.fetch(path)
        val doc = Jsoup.parse(html)
        val items = doc.select(".item").mapIndexedNotNull { idx, el ->
            parseNovelItem(el, includeChapters = false)?.copy(
                rank = idx + 1 + (page - 1) * 10,
            )
        }
        return CategoryDetail(items, page, parsePagination(doc))
    }

    // ---- Completed novels ----

    suspend fun scrapeCompleted(page: Int = 1): CategoryDetail {
        val path = if (page > 1) "/wanjie/$page.html" else "/wanjie/"
        val html = fetcher.fetch(path)
        val doc = Jsoup.parse(html)
        val novels = parseListItems(doc, ".item", includeChapters = true)
        return CategoryDetail(novels, page, parsePagination(doc))
    }

    // ---- Latest updates ----

    suspend fun scrapeLatest(page: Int = 1): CategoryDetail {
        val path = if (page > 1) "/zuixin/$page.html" else "/zuixin/"
        val html = fetcher.fetch(path)
        val doc = Jsoup.parse(html)
        val novels = parseListItems(doc, ".item", includeChapters = false)
        return CategoryDetail(novels, page, parsePagination(doc))
    }

    // ---- Search ----

    suspend fun scrapeSearch(keyword: String): Pair<List<Novel>, String> {
        if (keyword.isBlank()) return emptyList<Novel>() to keyword
        val encoded = jsUrlEncode(keyword)
        val html = fetcher.fetch("/i/sor.aspx?key=$encoded")
        val doc = Jsoup.parse(html)
        val novels = parseListItems(doc, ".item", includeChapters = true)
        return novels to keyword
    }

    // ---- Novel detail (incl. chapter list) ----

    suspend fun scrapeNovelDetail(bookId: String): NovelDetail {
        val html = fetcher.fetch("/$bookId/")
        val doc = Jsoup.parse(html)

        val title = doc.selectFirst(".itemtxt h1 a, .itemtxt h3 a")?.text()?.trim().orEmpty()
        val coverRaw = doc.selectFirst(".item img")?.attr("src").orEmpty()
        val cover = normalizeCover(coverRaw)
        val status = doc.select(".itemtxt p span").getOrNull(0)?.text()?.trim().orEmpty()
        val category = doc.select(".itemtxt p span").getOrNull(1)?.text()?.trim().orEmpty()
        val author = Regex("""作者[：:]\s*(\S+)""").find(doc.text())?.groupValues?.get(1).orEmpty()
        val words = doc.selectFirst(".itemtxt h1 i, .itemtxt h3 i")?.text()?.trim().orEmpty()
        val description = doc.selectFirst(".des.bb")?.text()?.trim().orEmpty()

        val chapters = doc.select("#list a").mapNotNull { el ->
            val href = el.attr("href")
            val chapterId = Regex("""/(\d+)\.html""").find(href)?.groupValues?.get(1) ?: return@mapNotNull null
            val chapterTitle = el.text().trim()
            if (chapterId.isBlank() || chapterTitle.isBlank()) null
            else Chapter(id = chapterId, title = chapterTitle, bookId = bookId)
        }

        val txtLinks = runCatching {
            val txtHtml = fetcher.fetch("/$bookId/txt.html")
            val txtDoc = Jsoup.parse(txtHtml)
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
            id = bookId,
            title = title,
            author = author,
            cover = cover,
            status = status,
            category = category,
            words = words,
            description = description,
            chapters = chapters,
            txtLinks = txtLinks,
        )
    }

    // ---- Chapter content (with ad filter + multi-page accumulation) ----

    suspend fun scrapeChapter(bookId: String, chapterId: String): ChapterContent {
        val allParagraphs = mutableListOf<String>()
        var currentUrl: String? = "/$bookId/$chapterId.html"
        var bookTitle = ""
        var chapterTitle = ""
        var prevChapter: ChapterRef? = null
        var nextChapter: ChapterRef? = null
        val seenHashes = mutableSetOf<Int>()
        var pageCount = 0

        while (currentUrl != null && pageCount < MAX_PAGES) {
            pageCount++
            val html = runCatching { fetcher.fetch(currentUrl) }.getOrNull() ?: break
            val hash = html.hashCode()
            if (!seenHashes.add(hash)) break // loop guard

            val doc = Jsoup.parse(html)
            if (pageCount == 1) {
                // Parse book & chapter title from the submenu
                val h1 = doc.selectFirst(".submenu h1")
                if (h1 != null) {
                    val linkText = h1.selectFirst("a")?.text()?.trim().orEmpty()
                    bookTitle = linkText
                    val fullH1 = h1.text().replace(bookTitle, "").replace(">", "").trim()
                    chapterTitle = fullH1
                }
                // Parse prev/next from prenext
                val prenext = doc.selectFirst(".prenext")
                if (prenext != null) {
                    val spans = prenext.select("span")
                    prevChapter = parseChapterLink(spans.getOrNull(0)?.selectFirst("a"))
                    nextChapter = parseChapterLink(spans.lastOrNull()?.selectFirst("a"))
                }
            }

            // Collect paragraphs from this page
            val con = doc.selectFirst(".con")
            if (con != null) {
                con.select("p").forEach { p ->
                    val raw = AdFilter.trim(p.text())
                    if (!AdFilter.isAdParagraph(raw)) {
                        allParagraphs.add(raw)
                    }
                }
            }

            // Find the next page link (chapterId-N.html)
            val prenext = doc.selectFirst(".prenext")
            val nextPageHref = prenext?.select("a[href]")?.toList()
                ?.lastOrNull { a ->
                    val href = a.attr("href")
                    Regex("""^/$bookId/$chapterId-\d+\.html$""").matches(href)
                }
                ?.attr("href")

            currentUrl = nextPageHref
        }

        return ChapterContent(
            bookId = bookId,
            bookTitle = bookTitle,
            chapterId = chapterId,
            chapterTitle = chapterTitle,
            content = allParagraphs.joinToString("\n\n"),
            prevChapter = prevChapter,
            nextChapter = nextChapter,
        )
    }

    // ---- Author novels ----

    suspend fun scrapeAuthorNovels(author: String): Pair<List<Novel>, String> {
        if (author.isBlank()) return emptyList<Novel>() to author
        val encoded = jsUrlEncode(author)
        val html = fetcher.fetch("/zuozhe/?tag=$encoded")
        val doc = Jsoup.parse(html)
        val novels = parseListItems(doc, ".item", includeChapters = true)
        return novels to author
    }

    // ---- Helpers ----

    private fun parseListItems(doc: Document, selector: String, includeChapters: Boolean): List<Novel> =
        doc.select(selector).mapNotNull { el -> parseNovelItem(el, includeChapters) }

    private fun parseTopList(doc: Document, selector: String): List<Novel> {
        val items = doc.select(selector)
        return items.mapIndexedNotNull { idx, el ->
            val titleEl = el.selectFirst("p a") ?: return@mapIndexedNotNull null
            val title = titleEl.text().trim()
            val href = el.selectFirst(".imga")?.attr("href").orEmpty()
            val id = href.replace("/", "").substringBefore("?")
            val coverRaw = el.selectFirst("img")?.attr("src").orEmpty()
            if (id.isBlank() || title.isBlank()) null
            else Novel(
                id = id, title = title,
                author = "",
                cover = normalizeCover(coverRaw),
                rank = idx + 1,
            )
        }
    }

    private fun parseTopListNovels(doc: Document, selector: String): List<Novel> =
        doc.select(selector).mapNotNull { el ->
            val title = el.selectFirst("p a")?.text()?.trim().orEmpty()
            val href = el.selectFirst(".imga")?.attr("href").orEmpty()
            val id = href.replace("/", "")
            val cover = normalizeCover(el.selectFirst("img")?.attr("src").orEmpty())
            if (id.isBlank() || title.isBlank()) null
            else Novel(id = id, title = title, author = "", cover = cover)
        }

    private fun parseNovelItem(el: Element, includeChapters: Boolean): Novel? {
        val link = el.selectFirst(".itemtxt h1 a, .itemtxt h3 a") ?: return null
        val title = link.text().trim()
        val href = link.attr("href")
        val id = href.replace("/", "")
        if (id.isBlank() || title.isBlank()) return null
        val coverRaw = el.selectFirst("img")?.attr("src").orEmpty()
        val cover = normalizeCover(coverRaw)
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
            id = id,
            title = title,
            author = author,
            cover = cover,
            status = status,
            category = category,
            words = words,
            lastChapter = lastChapter,
        )
    }

    private fun parseChapterLink(a: Element?): ChapterRef? {
        if (a == null) return null
        val href = a.attr("href")
        val id = Regex("""/(\d+)\.html""").find(href)?.groupValues?.get(1).orEmpty()
        val title = a.text().trim()
        if (id.isBlank()) return null
        return ChapterRef(id = id, title = title)
    }

    private fun parsePagination(doc: Document): Int {
        val pageText = doc.selectFirst(".page")?.text().orEmpty()
        val nums = Regex("""\d+""").findAll(pageText).toList()
        return nums.lastOrNull()?.value?.toIntOrNull() ?: 1
    }

    private fun normalizeCover(src: String): String = when {
        src.isBlank() -> ""
        src.startsWith("http") -> src
        src.startsWith("//") -> "https:$src"
        else -> "$baseUrl$src"
    }

    private fun jsUrlEncode(s: String): String {
        // Lightweight percent-encoding for non-alphanumerics
        val sb = StringBuilder()
        for (b in s.encodeToByteArray()) {
            val c = b.toInt() and 0xff
            if ((c in 'a'.code..'z'.code) ||
                (c in 'A'.code..'Z'.code) ||
                (c in '0'.code..'9'.code) ||
                c == '-' .code || c == '_'.code || c == '.'.code || c == '~'.code
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

    companion object {
        const val MAX_PAGES = 5
    }
}
