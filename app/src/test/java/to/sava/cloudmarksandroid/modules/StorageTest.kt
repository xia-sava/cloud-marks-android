package to.sava.cloudmarksandroid.modules

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import to.sava.cloudmarksandroid.databases.models.MarkTreeNode
import to.sava.cloudmarksandroid.databases.models.MarkType

/** テスト用の FileInfo 具象クラス */
private class StubFileInfo(filename: String) : FileInfo<Unit>(filename) {
    override val fileObject = Unit
}

/** テスト用の Storage 具象クラス。read() が返す JSON を外から指定できる */
private class TestStorage(
    private val jsonResponse: String,
) : Storage<StubFileInfo>(mockk()) {
    override suspend fun checkAccessibility() = true
    override suspend fun ls() = emptyList<StubFileInfo>()
    override suspend fun read(fileInfo: StubFileInfo) = jsonResponse
}

class StorageTest {

    private val storageGson = TestStorage("").gson

    /**
     * テスト用の有効な version 1 JSON を生成する。
     * hashContents は gson.toJson(MarkTreeNode) の結果をハッシュするため、
     * テスト JSON の contents は整数型 type（デシリアライズ用）で記述しつつ、
     * ハッシュは gson がリシリアライズした形式から計算する。
     */
    private fun validJson(
        title: String = "root",
        url: String = "",
        type: MarkType = MarkType.Folder,
    ): String {
        val node = MarkTreeNode(type, title, url, emptyList())
        val reSerializedContents = storageGson.toJson(node).trim()
        val hash = sha256(reSerializedContents)
        val typeValue = type.rawValue
        val contents = """{"type":$typeValue,"title":"$title","url":"$url","children":[]}"""
        return """{"version":1,"hash":"$hash","contents":$contents}"""
    }

    private fun sha256(input: String): String {
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { String.format("%02x", it) }
    }

    @Nested
    inner class ReadMarkFile {

        /** 有効な JSON を正しくパースする */
        @Test
        fun validJson() = runTest {
            val json = validJson(title = "bookmarks")
            val storage = TestStorage(json)
            val result = storage.readMarkFile(StubFileInfo("test.json"))
            assertEquals("bookmarks", result.title)
            assertEquals(MarkType.Folder, result.type)
        }

        /** 不正な JSON は InvalidJsonException を投げる */
        @Test
        fun invalidJson_throwsException() = runTest {
            val storage = TestStorage("not valid json")
            assertThrows(InvalidJsonException::class.java) {
                kotlinx.coroutines.test.runTest {
                    storage.readMarkFile(StubFileInfo("test.json"))
                }
            }
        }

        /** ハッシュ不一致は InvalidJsonException を投げる */
        @Test
        fun hashMismatch_throwsException() = runTest {
            val json = """{"version":1,"hash":"0000000000000000000000000000000000000000000000000000000000000000","contents":{"type":0,"title":"root","url":"","children":[]}}"""
            val storage = TestStorage(json)
            assertThrows(InvalidJsonException::class.java) {
                kotlinx.coroutines.test.runTest {
                    storage.readMarkFile(StubFileInfo("test.json"))
                }
            }
        }

        /** version が 1 以外の場合はハッシュ検証をスキップする */
        @Test
        fun unknownVersion_skipsHashCheck() = runTest {
            val json = """{"version":2,"hash":"wrong","contents":{"type":0,"title":"root","url":"","children":[]}}"""
            val storage = TestStorage(json)
            val result = storage.readMarkFile(StubFileInfo("test.json"))
            assertEquals("root", result.title)
        }

        /** ブックマークタイプのノードをパースできる */
        @Test
        fun bookmarkType() = runTest {
            val json = validJson(title = "Example", url = "https://example.com", type = MarkType.Bookmark)
            val storage = TestStorage(json)
            val result = storage.readMarkFile(StubFileInfo("test.json"))
            assertEquals(MarkType.Bookmark, result.type)
            assertEquals("https://example.com", result.url)
        }
    }

    @Nested
    inner class GsonRoundTrip {

        /** MarkTreeNode を toJson → fromJson しても MarkType が保持される */
        @Test
        fun markTypePreservedOnRoundTrip() {
            val original = MarkTreeNode(MarkType.Folder, "test", "", emptyList())
            val json = storageGson.toJson(original)
            val restored = storageGson.fromJson(json, MarkTreeNode::class.java)
            assertEquals(MarkType.Folder, restored.type)
        }

        /** toJson が整数型の type を出力する（hashContents の整合性に必要） */
        @Test
        fun toJsonUsesIntegerType() {
            val node = MarkTreeNode(MarkType.Bookmark, "test", "https://example.com", emptyList())
            val json = storageGson.toJson(node)
            assert(json.contains(""""type":1""")) {
                "type should be serialized as integer 1, but was: $json"
            }
        }
    }

    @Nested
    inner class ListDir {

        /** listDir は ls() の結果をそのまま返す */
        @Test
        fun delegatesToLs() = runTest {
            val storage = TestStorage("")
            assertEquals(emptyList<StubFileInfo>(), storage.listDir())
        }
    }
}
