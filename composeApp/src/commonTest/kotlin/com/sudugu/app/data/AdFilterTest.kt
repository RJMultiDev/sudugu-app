package com.sudugu.app.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdFilterTest {
    @Test fun `empty and short strings are ads`() {
        assertTrue(AdFilter.isAdParagraph(""))
        assertTrue(AdFilter.isAdParagraph("，?"))
        assertTrue(AdFilter.isAdParagraph("。"))
    }

    @Test fun `pure symbol paragraphs are ads`() {
        assertTrue(AdFilter.isAdParagraph("，??，??，??"))
        assertTrue(AdFilter.isAdParagraph("。。。。。。"))
        assertTrue(AdFilter.isAdParagraph("，，？？。。，??"))
    }

    @Test fun `repeated punctuation is ad`() {
        assertTrue(AdFilter.isAdParagraph("正文中间插入……？、；"))
    }

    @Test fun `keywords trigger ad classification`() {
        assertTrue(AdFilter.isAdParagraph("速读谷为您提供最新热门小说"))
        assertTrue(AdFilter.isAdParagraph("扫码加群"))
        assertTrue(AdFilter.isAdParagraph("点击阅读更多"))
    }

    @Test fun `residual HTML and entities are ads`() {
        assertTrue(AdFilter.isAdParagraph("正文<script>alert(1)</script>"))
        assertTrue(AdFilter.isAdParagraph("介绍 &nbsp; 更多"))
    }

    @Test fun `legitimate paragraphs pass`() {
        assertFalse(AdFilter.isAdParagraph("宁拙接着分析：流金客第二战的宝物，来路太明显了。"))
        assertFalse(AdFilter.isAdParagraph("他越听越对敌人的反应，报以期待。"))
        assertFalse(AdFilter.isAdParagraph("第660章 龙王"));
    }
}
