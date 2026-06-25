package com.sudugu.app.data

/**
 * Heuristics to identify advertisement / junk paragraphs inserted by the source site
 * inside `<div class="con">`. Returns true if the paragraph looks like noise.
 *
 * Mirrors the ad-detection rules from the original RN client (src/services/scraper.ts).
 */
object AdFilter {
    private val symbolCharRegex = Regex("""[?？,。.、…!！_+\-=*·•・]""")
    private val symbolOnlyRegex = Regex("""^[\s?？,。.、…!！_+\-=*·•・]+$""")
    private val repeatedSymbolRegex = Regex("""[?？,。.]{3,}""")
    private val adKeywordRegex = Regex(
        """速读谷|更多小说|点击阅读|最新热门|打开APP|下载APP|下载app|关注我们|二维码|扫码加群|书友群|官方群|企鹅|微信""",
        RegexOption.IGNORE_CASE,
    )
    private val residueTagRegex = Regex("""<[^>]+>""")
    private val residueEntityRegex = Regex("""&[a-z]+;|&#\d+;""")

    fun isAdParagraph(text: String): Boolean {
        if (text.isBlank()) return true
        if (text.length < 4) return true
        if (symbolOnlyRegex.matches(text)) return true
        // High symbol ratio (>50%) — catches the typical `，??，??，??` patterns
        // where punct chars alternate and don't trigger the 3-in-a-row check.
        val symbolCount = symbolCharRegex.findAll(text).count()
        if (symbolCount * 2 > text.length) return true
        if (repeatedSymbolRegex.containsMatchIn(text)) return true
        if (adKeywordRegex.containsMatchIn(text)) return true
        // Unstripped HTML or HTML entities inside the "text" — scraper missed them.
        if (residueTagRegex.containsMatchIn(text)) return true
        if (residueEntityRegex.containsMatchIn(text)) return true
        return false
    }

    /** Clean obvious leading/trailing noise while leaving legitimate text intact. */
    fun trim(text: String): String =
        text.replace(residueTagRegex, "").trim()
}
