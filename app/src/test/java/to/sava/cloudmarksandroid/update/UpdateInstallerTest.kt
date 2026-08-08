package to.sava.cloudmarksandroid.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class UpdateInstallerTest {

    /** packageName が自アプリと一致するときだけ受け入れる */
    @ParameterizedTest(name = "{0} と {1} → {2}")
    @CsvSource(
        "to.sava.cloudmarksandroid, to.sava.cloudmarksandroid, true",
        "to.sava.cloudmarksandroid.other, to.sava.cloudmarksandroid, false",
        "to.sava.other, to.sava.cloudmarksandroid, false",
    )
    fun judgesPackageName(actual: String, expected: String, result: Boolean) {
        assertEquals(result, isExpectedApkPackage(actual, expected))
    }

    /** APKとして解析できなかったものは受け入れない */
    @Test
    fun rejectsUnparsableApk() {
        assertFalse(isExpectedApkPackage(null, "to.sava.cloudmarksandroid"))
    }
}
