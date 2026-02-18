package to.sava.cloudmarksandroid.databases.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FaviconTest {

    /** toString はドメイン名を返す */
    @Test
    fun toString_returnsDomain() {
        val favicon = Favicon(domain = "example.com", favicon = ByteArray(0), size = 0)
        assertEquals("example.com", favicon.toString())
    }

    /** デフォルト値で生成できる */
    @Test
    fun defaultValues() {
        val favicon = Favicon()
        assertEquals("", favicon.domain)
        assertEquals(0, favicon.favicon.size)
        assertEquals(0, favicon.size)
    }
}
