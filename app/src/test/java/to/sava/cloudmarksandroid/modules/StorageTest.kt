package to.sava.cloudmarksandroid.modules

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import to.sava.cloudmarksandroid.databases.models.MarkTreeNode
import to.sava.cloudmarksandroid.databases.models.MarkType

/** テスト用の Storage 実装。read() が返す JSON を外から指定できる */
private class TestStorage(
    private val jsonResponse: String,
) : Storage {
    override suspend fun checkAccessibility() = true
    override suspend fun listDir() = emptyList<FileInfo>()
    override suspend fun read(fileInfo: FileInfo) = jsonResponse
}

class StorageTest {

    /** 与えたノードと整合の取れた version 1 の JSON を組み立てる */
    private fun validJson(
        title: String = "root",
        url: String = "",
        type: MarkType = MarkType.Folder,
    ): String {
        val hash = hashContents(MarkTreeNode(type, title, url, emptyList()))
        val contents = """{"type":${type.rawValue},"title":"$title","url":"$url","children":[]}"""
        return """{"version":1,"hash":"$hash","contents":$contents}"""
    }

    /** 文字列の SHA-256 を 16 進で求める */
    private fun sha256(input: String): String {
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { String.format("%02x", it) }
    }

    @Nested
    inner class ParseMarkFile {

        /** 有効な JSON を正しく解釈する */
        @Test
        fun validJson() {
            val result = parseMarkFile(validJson(title = "bookmarks"))
            assertEquals("bookmarks", result.title)
            assertEquals(MarkType.Folder, result.type)
        }

        /** 不正な JSON は InvalidJsonException を投げる */
        @Test
        fun invalidJson_throwsException() {
            assertThrows(InvalidJsonException::class.java) {
                parseMarkFile("not valid json")
            }
        }

        /** ハッシュ不一致は InvalidJsonException を投げる */
        @Test
        fun hashMismatch_throwsException() {
            val json =
                """{"version":1,"hash":"0000000000000000000000000000000000000000000000000000000000000000","contents":{"type":0,"title":"root","url":"","children":[]}}"""
            assertThrows(InvalidJsonException::class.java) {
                parseMarkFile(json)
            }
        }

        /** version が 1 以外の場合はハッシュ検証をスキップする */
        @Test
        fun unknownVersion_skipsHashCheck() {
            val json =
                """{"version":2,"hash":"wrong","contents":{"type":0,"title":"root","url":"","children":[]}}"""
            assertEquals("root", parseMarkFile(json).title)
        }

        /** 未知の type 値は InvalidJsonException を投げる */
        @Test
        fun unknownMarkType_throwsException() {
            val json =
                """{"version":2,"hash":"wrong","contents":{"type":9,"title":"root","url":"","children":[]}}"""
            assertThrows(InvalidJsonException::class.java) {
                parseMarkFile(json)
            }
        }

        /** 知らないフィールドがあっても読み進める */
        @Test
        fun unknownFieldsAreIgnored() {
            val json =
                """{"version":2,"hash":"wrong","extra":"x","contents":{"type":0,"title":"root","url":"","children":[],"note":"y"}}"""
            assertEquals("root", parseMarkFile(json).title)
        }

        /** ブックマークタイプのノードを解釈できる */
        @Test
        fun bookmarkType() {
            val result = parseMarkFile(
                validJson(title = "Example", url = "https://example.com", type = MarkType.Bookmark)
            )
            assertEquals(MarkType.Bookmark, result.type)
            assertEquals("https://example.com", result.url)
        }
    }

    @Nested
    inner class ReadMarkFile {

        /** read() が返した JSON をそのまま解釈して返す */
        @Test
        fun parsesWhatReadReturns() = runTest {
            val storage = TestStorage(validJson(title = "bookmarks"))
            assertEquals("bookmarks", storage.readMarkFile(FileInfo("test.json")).title)
        }
    }

    @Nested
    inner class HashContents {

        /** MarkType は列挙子の名前ではなく rawValue で出力する */
        @Test
        fun serializesMarkTypeAsRawValue() {
            val node = MarkTreeNode(MarkType.Bookmark, "test", "https://example.com", emptyList())
            val expected =
                """{"type":1,"title":"test","url":"https://example.com","children":[]}"""
            assertEquals(sha256(expected), hashContents(node))
        }

        /** HTML 特殊文字はエスケープせずそのまま出力する */
        @Test
        fun doesNotEscapeHtmlCharacters() {
            val node =
                MarkTreeNode(MarkType.Bookmark, "a&b", "https://example.com/?x=1&y=2", emptyList())
            val expected =
                """{"type":1,"title":"a&b","url":"https://example.com/?x=1&y=2","children":[]}"""
            assertEquals(sha256(expected), hashContents(node))
        }

        /** 子ノードは children へ入れ子で出力する */
        @Test
        fun serializesNestedChildren() {
            val child = MarkTreeNode(MarkType.Bookmark, "child", "https://example.com", emptyList())
            val node = MarkTreeNode(MarkType.Folder, "parent", "", listOf(child))
            val expected =
                """{"type":0,"title":"parent","url":"","children":[{"type":1,"title":"child","url":"https://example.com","children":[]}]}"""
            assertEquals(sha256(expected), hashContents(node))
        }
    }
}
