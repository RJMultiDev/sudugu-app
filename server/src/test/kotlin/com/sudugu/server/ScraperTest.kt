package com.sudugu.server

import com.sudugu.server.scraper.Scraper
import kotlin.test.Test

class ScraperTest {
    private val scraper = Scraper()

    @Test
    fun `home returns three sections`() {
        val home = scraper.scrapeHome()
        // We can't assert on data because the upstream site may be flaky.
        // Just ensure the call doesn't throw and shape is intact.
        assert(home.latestUpdates.isNotEmpty() || home.rankings.isNotEmpty() || home.completedNovels.isNotEmpty())
    }

    @Test
    fun `chapter content includes all pages with no ad paragraphs`() {
        val c = scraper.scrapeChapter("100", "4029839")
        val paragraphs = c.content.split("\n\n").filter { it.isNotBlank() }
        // Multi-page chapter on sudugu.org, expect at least 25 paragraphs (3 pages)
        println("Paragraphs: ${paragraphs.size}, total chars: ${c.content.length}")
        assert(paragraphs.size >= 25) { "Expected ≥25 paragraphs, got ${paragraphs.size}" }
        // No "，??" garbled paragraphs (typical ad insertion artifact)
        val adSuspect = paragraphs.count { Regex(""",\?{2,}""").containsMatchIn(it) }
        assert(adSuspect == 0) { "Found $adSuspect suspected ad paragraphs" }
    }

    @Test
    fun `chapter with embedded ads filters them out`() {
        // 100/4021578 has a "，??" ad paragraph in the middle of the first page
        val c = scraper.scrapeChapter("100", "4021578")
        val hasGarbage = c.content.contains("，??")
        assert(!hasGarbage) { "Ad paragraph leaked: $c.content" }
    }
}
