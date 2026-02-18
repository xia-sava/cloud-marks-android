package to.sava.cloudmarksandroid.databases

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import to.sava.cloudmarksandroid.databases.models.MarkType

class MarkTypeConverterTest {

    private val converter = MarkTypeConverter()

    @Nested
    inner class ToMarkType {

        /** rawValue から対応する MarkType に変換する */
        @ParameterizedTest(name = "rawValue={0} → {1}")
        @CsvSource("0, Folder", "1, Bookmark")
        fun validValues(rawValue: Int, expected: MarkType) {
            assertEquals(expected, converter.toMarkType(rawValue))
        }

        /** 未定義の rawValue は null を返す */
        @Test
        fun invalidValue_returnsNull() {
            assertNull(converter.toMarkType(999))
        }
    }

    @Nested
    inner class FromMarkType {

        /** MarkType から rawValue に変換する */
        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource("Folder, 0", "Bookmark, 1")
        fun allTypes(type: MarkType, expected: Int) {
            assertEquals(expected, converter.fromMarkType(type))
        }
    }
}
