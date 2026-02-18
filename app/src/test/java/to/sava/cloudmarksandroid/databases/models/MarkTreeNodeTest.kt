package to.sava.cloudmarksandroid.databases.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MarkTreeNodeTest {

    /** テスト用ブックマークノードを作成する */
    private fun bookmark(title: String = "", url: String = "") =
        MarkTreeNode(MarkType.Bookmark, title, url, emptyList())

    /** テスト用フォルダノードを作成する */
    private fun folder(title: String = "", children: List<MarkTreeNode> = emptyList()) =
        MarkTreeNode(MarkType.Folder, title, "", children)

    @Nested
    inner class CountChildren {

        /** 単一ブックマークの countChildren は1 */
        @Test
        fun singleBookmark() {
            assertEquals(1L, bookmark("test").countChildren())
        }

        /** 単一フォルダの countChildren は1 */
        @Test
        fun singleFolder() {
            assertEquals(1L, folder("folder").countChildren())
        }

        /** フィルタなしではブックマークの子は数えない */
        @Test
        fun folderWithBookmarks_countsOnlyFolder() {
            val node = folder(
                "parent", listOf(
                    bookmark("a"),
                    bookmark("b"),
                    bookmark("c"),
                )
            )
            assertEquals(1L, node.countChildren())
        }

        /** ネストしたフォルダを再帰的にカウントする */
        @Test
        fun nestedFolders() {
            val node = folder(
                "root", listOf(
                    folder("sub1", listOf(bookmark("a"))),
                    folder("sub2", listOf(bookmark("b"))),
                )
            )
            assertEquals(3L, node.countChildren())
        }

        /** フィルタ指定で該当タイプだけカウントする */
        @ParameterizedTest(name = "filter={0} → {1}")
        @CsvSource("Folder, 2", "Bookmark, 1")
        fun withFilter(filter: MarkType, expected: Long) {
            val node = folder(
                "root", listOf(
                    folder("sub", listOf(bookmark("a"))),
                    bookmark("b"),
                )
            )
            assertEquals(expected, node.countChildren(filter))
        }
    }

    /** isBookmark と isFolder が type に応じて正しい */
    @ParameterizedTest(name = "{0} → isBookmark={1}, isFolder={2}")
    @CsvSource("Bookmark, true, false", "Folder, false, true")
    fun typeProperties(type: MarkType, isBookmark: Boolean, isFolder: Boolean) {
        val node = MarkTreeNode(type, "test", "", emptyList())
        assertEquals(isBookmark, node.isBookmark)
        assertEquals(isFolder, node.isFolder)
    }

    /** toString が type/title/url/子数 の形式 */
    @Test
    fun toString_format() {
        val node = folder("myFolder", listOf(bookmark("a"), bookmark("b")))
        assertEquals("Folder/myFolder/<>/2", node.toString())
    }
}
