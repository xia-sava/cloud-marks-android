package to.sava.cloudmarksandroid.update

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class ReleaseDownloadTest {

    @Nested
    inner class IsDownloadableHttpsUrl {

        /** https でホストを持つ URL だけを受け付ける */
        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource(
            "https://github.com/a/b.apk, true",
            "HTTPS://github.com/a/b.apk, true",
            "http://github.com/a/b.apk, false",
            "ftp://github.com/a/b.apk, false",
            "https:///nohost.apk, false",
            "file:///tmp/b.apk, false",
            "'', false",
            "not a url, false",
        )
        fun judgesScheme(url: String, expected: Boolean) {
            assertEquals(expected, isDownloadableHttpsUrl(url))
        }
    }

    @Nested
    inner class MatchesSha256 {

        /** 中身から計算した値と一致すれば真、違う値なら偽とする */
        @Test
        fun matchingDigest(@TempDir dir: File) {
            val file = File(dir, "asset").apply { writeText("cloud marks") }

            assertTrue(matchesSha256(file, sha256Of(file)))
            assertFalse(matchesSha256(file, "0".repeat(64)))
        }

        /** 大文字小文字と前後の空白は無視して比べる */
        @Test
        fun ignoresCaseAndWhitespace(@TempDir dir: File) {
            val file = File(dir, "asset").apply { writeText("cloud marks") }
            val digest = sha256Of(file)

            assertTrue(matchesSha256(file, digest.uppercase()))
            assertTrue(matchesSha256(file, "  $digest\n"))
        }

        /** 中身が違えば偽とする */
        @Test
        fun differentContent(@TempDir dir: File) {
            val file = File(dir, "asset").apply { writeText("cloud marks") }
            val other = File(dir, "other").apply { writeText("cloud marks!") }

            assertFalse(matchesSha256(file, sha256Of(other)))
        }

        /** 期待値を得るために実際に計算する */
        private fun sha256Of(file: File): String =
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(file.readBytes())
                .joinToString("") { String.format("%02x", it) }
    }

    @Nested
    inner class CopyReportingProgress {

        /** 書き写した内容をそのまま渡す */
        @Test
        fun copiesContent() {
            val source = ByteArray(1000) { it.toByte() }
            val output = ByteArrayOutputStream()

            copyReportingProgress(ByteArrayInputStream(source), output, 1000) { _, _ -> }

            assertArrayEquals(source, output.toByteArray())
        }

        /** 書き写しの完了時には必ず 1 回知らせる */
        @Test
        fun notifiesOnCompletion() {
            val progress = mutableListOf<Pair<Long, Long>>()

            copyReportingProgress(
                ByteArrayInputStream(ByteArray(10)),
                ByteArrayOutputStream(),
                10,
            ) { received, total -> progress += received to total }

            assertEquals(listOf(10L to 10L), progress)
        }

        /** 上限を超える入力は書き写しをやめて失敗させる */
        @Test
        fun stopsAtMaxBytes() {
            assertThrows(IOException::class.java) {
                copyReportingProgress(
                    ByteArrayInputStream(ByteArray(100)),
                    ByteArrayOutputStream(),
                    100,
                    maxBytes = 50,
                ) { _, _ -> }
            }
        }
    }

    @Nested
    inner class DownloadToFile {

        /** 応答の中身をファイルへ書き出す */
        @Test
        fun writesResponseToFile(@TempDir dir: File) = runBlocking {
            val payload = ByteArray(2048) { it.toByte() }
            val client = HttpClient(MockEngine { respond(payload) })
            val file = File(dir, "cloud-marks.apk")

            client.downloadToFile("https://example.test/cloud-marks.apk", file)

            assertArrayEquals(payload, file.readBytes())
        }

        /** https でない取得先は問い合わせずに失敗させる */
        @Test
        fun rejectsNonHttpsUrl(@TempDir dir: File) = runBlocking {
            val engine = MockEngine { respond(ByteArray(1)) }
            val file = File(dir, "cloud-marks.apk")

            assertThrows(IOException::class.java) {
                runBlocking {
                    HttpClient(engine).downloadToFile("http://example.test/a.apk", file)
                }
            }
            assertEquals(0, engine.requestHistory.size)
        }

        /** 取得に失敗したら書きかけを残さない */
        @Test
        fun leavesNoFileOnFailure(@TempDir dir: File) = runBlocking {
            val client = HttpClient(MockEngine { respond(ByteArray(0), HttpStatusCode.NotFound) })
            val file = File(dir, "cloud-marks.apk")

            assertThrows(IOException::class.java) {
                runBlocking {
                    client.downloadToFile("https://example.test/cloud-marks.apk", file)
                }
            }
            assertFalse(file.exists())
        }

        /** 宣言された長さが上限を超える応答は受け取らない */
        @Test
        fun rejectsTooLargeAnnouncedLength(@TempDir dir: File) = runBlocking {
            val client = HttpClient(MockEngine { respond(ByteArray(1024)) })
            val file = File(dir, "cloud-marks.apk")

            assertThrows(IOException::class.java) {
                runBlocking {
                    client.downloadToFile(
                        "https://example.test/cloud-marks.apk",
                        file,
                        maxBytes = 512,
                    )
                }
            }
        }
    }
}
