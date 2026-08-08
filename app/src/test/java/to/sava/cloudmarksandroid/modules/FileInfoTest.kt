package to.sava.cloudmarksandroid.modules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FileInfoTest {

    @Nested
    inner class Timestamp {

        /** bookmarks.{timestamp}.json のパターンからタイムスタンプを抽出する */
        @Test
        fun validFilename() {
            val info = FileInfo("folder/bookmarks.1234567890.json")
            assertEquals(1234567890L, info.timestamp)
        }

        /** パターンに一致しないファイル名は 0 を返す */
        @ParameterizedTest(name = "\"{0}\" → 0")
        @CsvSource("other.json", "bookmarks.json", "bookmarks.abc.json", "''")
        fun invalidFilenames(filename: String) {
            assertEquals(0L, FileInfo(filename).timestamp)
        }
    }

    @Nested
    inner class IsEmpty {

        /** 空文字列のファイル名は空と判定する */
        @Test
        fun emptyFilename() {
            assertEquals(true, FileInfo("").isEmpty)
        }

        /** 空でないファイル名は空でないと判定する */
        @Test
        fun nonEmptyFilename() {
            assertEquals(false, FileInfo("bookmarks.json").isEmpty)
        }
    }
}
