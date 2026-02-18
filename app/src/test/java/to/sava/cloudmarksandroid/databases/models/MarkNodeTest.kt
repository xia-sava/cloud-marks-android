package to.sava.cloudmarksandroid.databases.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MarkNodeTest {

    @Nested
    inner class TypeProperties {

        /** isBookmark と isFolder が type に応じて正しい */
        @ParameterizedTest(name = "{0} → isBookmark={1}, isFolder={2}")
        @CsvSource("Bookmark, true, false", "Folder, false, true")
        fun typeFlags(type: MarkType, isBookmark: Boolean, isFolder: Boolean) {
            val node = MarkNode(type = type)
            assertEquals(isBookmark, node.isBookmark)
            assertEquals(isFolder, node.isFolder)
        }
    }

    @Nested
    inner class ToString {

        /** toString が type/parentId/order/title/<url> の形式 */
        @Test
        fun format() {
            val node = MarkNode(
                type = MarkType.Bookmark,
                title = "Example",
                url = "https://example.com",
                order = 3,
                parentId = 1L,
            )
            assertEquals("Bookmark/1/3/Example/<https://example.com>", node.toString())
        }

        /** parentId が null の場合 */
        @Test
        fun nullParentId() {
            val node = MarkNode(type = MarkType.Folder, title = "root")
            assertEquals("Folder/null/0/root/<>", node.toString())
        }
    }

    @Nested
    inner class Id {

        /** id のデフォルト値は 0 */
        @Test
        fun defaultId() {
            assertEquals(0L, MarkNode().id)
        }

        /** id を設定・取得できる */
        @Test
        fun setAndGet() {
            val node = MarkNode()
            node.id = 42L
            assertEquals(42L, node.id)
        }
    }

    @Nested
    inner class Domain {

        /** URL からドメイン名を抽出する */
        @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
        @CsvSource(
            "https://example.com/path, example.com",
            "http://sub.example.com, sub.example.com",
            "https://example.com:8080/path, example.com",
        )
        fun parseDomain(url: String, expected: String) {
            assertEquals(expected, MarkNode.parseDomain(url))
        }

        /** 不正な URL や空文字列は空文字を返す */
        @ParameterizedTest(name = "\"{0}\" → empty")
        @CsvSource("''", "not-a-url")
        fun invalidUrl_returnsEmpty(url: String) {
            assertEquals("", MarkNode.parseDomain(url))
        }

        /** domain プロパティが parseDomain と同じ結果を返す */
        @Test
        fun domainProperty() {
            val node = MarkNode(url = "https://example.com/page")
            assertEquals("example.com", node.domain)
        }
    }

    @Nested
    inner class RootId {

        /** ROOT_ID 定数は 1 */
        @Test
        fun value() {
            assertEquals(1L, MarkNode.ROOT_ID)
        }
    }
}
