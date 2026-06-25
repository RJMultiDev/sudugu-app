package com.sudugu.app.util

/** Percent-encoding for URL path / query components. KMP-friendly. */
object Url {
    fun encode(s: String): String {
        val sb = StringBuilder()
        for (b in s.encodeToByteArray()) {
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

    fun decode(s: String): String {
        val bytes = ByteArray(s.length)
        var i = 0
        var j = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%' && i + 2 < s.length) {
                val hi = s[i + 1].digitToIntOrNull(16) ?: -1
                val lo = s[i + 2].digitToIntOrNull(16) ?: -1
                if (hi >= 0 && lo >= 0) {
                    bytes[j++] = ((hi shl 4) or lo).toByte()
                    i += 3
                    continue
                }
            }
            bytes[j++] = c.code.toByte()
            i++
        }
        return bytes.copyOf(j).decodeToString()
    }
}
