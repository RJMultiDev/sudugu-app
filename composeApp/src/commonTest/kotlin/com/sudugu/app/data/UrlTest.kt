package com.sudugu.app.data

import com.sudugu.app.util.Url
import kotlin.test.Test
import kotlin.test.assertEquals

class UrlTest {
    @Test fun `encodes alphanumerics as is`() {
        assertEquals("hello", Url.encode("hello"))
        assertEquals("abc-123_x.~", Url.encode("abc-123_x.~"))
    }

    @Test fun `encodes chinese`() {
        val out = Url.encode("速读谷")
        // Each Chinese char becomes 3 %-encoded bytes (3 bytes × 3 chars × 3 chars per %XX)
        assertEquals(27, out.length)
        assert(out.startsWith("%E9"))
    }

    @Test fun `decodes simple case`() {
        assertEquals("hello world", Url.decode("hello%20world"))
    }

    @Test fun `decode is inverse of encode`() {
        val s = "速读谷 novel 123"
        assertEquals(s, Url.decode(Url.encode(s)))
    }
}
