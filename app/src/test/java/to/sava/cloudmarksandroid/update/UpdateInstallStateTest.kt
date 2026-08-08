package to.sava.cloudmarksandroid.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class UpdateInstallStateTest {

    @Nested
    inner class DownloadingFraction {

        /** 全体長が判っていれば受信量との比を返す */
        @ParameterizedTest(name = "{0}/{1} → {2}")
        @CsvSource("0, 100, 0.0", "25, 100, 0.25", "100, 100, 1.0")
        fun computesFraction(received: Long, total: Long, expected: Float) {
            val state = UpdateInstallState.Downloading(received, total)
            assertEquals(expected, state.fraction!!, 0.0001f)
        }

        /** 全体長が判らなければ進み具合を出さない */
        @Test
        fun unknownTotal() {
            assertNull(UpdateInstallState.Downloading(50, 0).fraction)
        }

        /** 受信量が全体長を超えても 1.0 を超えない */
        @Test
        fun clampsToOne() {
            val state = UpdateInstallState.Downloading(200, 100)
            assertEquals(1.0f, state.fraction!!, 0.0001f)
        }
    }
}
