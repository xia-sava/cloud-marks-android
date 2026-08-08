package to.sava.cloudmarksandroid.update

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private const val MANIFEST_URL = "https://example.test/latest.json"

class UpdateCheckerTest {

    private val keyPair = generateTestKeyPair()

    /** 版だけが違うマニフェストを組み立てる */
    private fun manifestOf(versionCode: Int, versionName: String, sha256: String = "ab12") =
        """{"android":{"versionCode":$versionCode,"versionName":"$versionName","sha256":"$sha256"}}"""

    /**
     * マニフェストとその署名を返す UpdateChecker を組み立てる。
     * [signature] を渡すと署名だけ差し替えられる。
     */
    private fun checkerFor(
        manifest: String,
        currentVersionCode: Int,
        signature: String? = null,
        manifestStatus: HttpStatusCode = HttpStatusCode.OK,
        signatureStatus: HttpStatusCode = HttpStatusCode.OK,
    ): Pair<UpdateChecker, MockEngine> {
        val engine = MockEngine { request ->
            if (request.url.toString().endsWith(".sig")) {
                respond(
                    (signature ?: keyPair.signBase64(manifest.toByteArray())).toByteArray(),
                    signatureStatus,
                )
            } else {
                respond(manifest.toByteArray(), manifestStatus)
            }
        }
        val checker = UpdateChecker(
            HttpClient(engine),
            currentVersionCode,
            MANIFEST_URL,
            keyPair.publicKeyBase64(),
        )
        return checker to engine
    }

    @Nested
    inner class VersionComparison {

        /** 配布中の版が自分より新しければ更新ありとする */
        @Test
        fun newerVersionIsAvailable() = runTest {
            val (checker, _) = checkerFor(manifestOf(2026080801, "1.4.0"), 2026021901)

            val status = assertInstanceOf(UpdateStatus.Available::class.java, checker.check())
            assertEquals("1.4.0", status.versionName)
            assertEquals("ab12", status.sha256)
        }

        /** 配布中の版が自分と同じなら最新とする */
        @Test
        fun sameVersionIsUpToDate() = runTest {
            val (checker, _) = checkerFor(manifestOf(2026021901, "1.3.0"), 2026021901)

            assertEquals(UpdateStatus.UpToDate, checker.check())
        }

        /** 配布中の版が自分より古ければ最新とする */
        @Test
        fun olderVersionIsUpToDate() = runTest {
            val (checker, _) = checkerFor(manifestOf(2026021801, "1.2.0"), 2026021901)

            assertEquals(UpdateStatus.UpToDate, checker.check())
        }
    }

    @Nested
    inner class AssetUrl {

        /** 配布物の取得先はマニフェストの記載ではなく固定の規則で組む */
        @Test
        fun buildsUrlFromFixedRule() = runTest {
            val manifest =
                """{"android":{"versionCode":2026080801,"versionName":"1.4.0","sha256":"ab12","url":"https://evil.test/malware.apk"}}"""
            val (checker, _) = checkerFor(manifest, 2026021901)

            val status = assertInstanceOf(UpdateStatus.Available::class.java, checker.check())
            assertEquals(
                "https://github.com/xia-sava/cloud-marks-android/releases/download/latest/cloud-marks.apk",
                status.url,
            )
        }
    }

    @Nested
    inner class SignatureVerification {

        /** 署名が中身と合わなければ更新ありとは扱わない */
        @Test
        fun rejectsSignatureForOtherContent() = runTest {
            val signature = keyPair.signBase64("別の中身".toByteArray())
            val (checker, _) =
                checkerFor(manifestOf(2026080801, "1.4.0"), 2026021901, signature = signature)

            val status = assertInstanceOf(UpdateStatus.Failed::class.java, checker.check())
            assertEquals("配布物の署名を確認できませんでした", status.reason)
        }

        /** 別の鍵で作られた署名は受け付けない */
        @Test
        fun rejectsSignatureFromAnotherKey() = runTest {
            val manifest = manifestOf(2026080801, "1.4.0")
            val signature = generateTestKeyPair().signBase64(manifest.toByteArray())
            val (checker, _) = checkerFor(manifest, 2026021901, signature = signature)

            assertInstanceOf(UpdateStatus.Failed::class.java, checker.check())
        }

        /** 署名を取得できなければ検証を省かず失敗にする */
        @Test
        fun rejectsWhenSignatureIsMissing() = runTest {
            val (checker, _) = checkerFor(
                manifestOf(2026080801, "1.4.0"),
                2026021901,
                signatureStatus = HttpStatusCode.NotFound,
            )

            val status = assertInstanceOf(UpdateStatus.Failed::class.java, checker.check())
            assertEquals("配布物の署名を確認できませんでした", status.reason)
        }

        /** 署名を確かめる前にマニフェストを解釈しない */
        @Test
        fun doesNotParseBeforeVerifying() = runTest {
            val body = "これはJSONではない"
            val signature = generateTestKeyPair().signBase64(body.toByteArray())
            val (checker, _) = checkerFor(body, 2026021901, signature = signature)

            val status = assertInstanceOf(UpdateStatus.Failed::class.java, checker.check())
            assertEquals("配布物の署名を確認できませんでした", status.reason)
        }
    }

    @Nested
    inner class Failures {

        /** マニフェストを取得できなければ失敗とする */
        @Test
        fun manifestNotFound() = runTest {
            val (checker, _) = checkerFor(
                manifestOf(2026080801, "1.4.0"),
                2026021901,
                manifestStatus = HttpStatusCode.NotFound,
            )

            val status = assertInstanceOf(UpdateStatus.Failed::class.java, checker.check())
            assertTrue(status.reason.contains("404"))
        }

        /** 署名は通るが中身が JSON として壊れていれば失敗とする */
        @Test
        fun malformedManifest() = runTest {
            val (checker, _) = checkerFor("{\"android\":}", 2026021901)

            val status = assertInstanceOf(UpdateStatus.Failed::class.java, checker.check())
            assertEquals("更新情報を解析できませんでした", status.reason)
        }

        /** android の項目が無いマニフェストは失敗とする */
        @Test
        fun manifestWithoutAndroidEntry() = runTest {
            val (checker, _) = checkerFor("""{"desktop":{"versionCode":1}}""", 2026021901)

            val status = assertInstanceOf(UpdateStatus.Failed::class.java, checker.check())
            assertEquals("更新情報を解析できませんでした", status.reason)
        }
    }
}
