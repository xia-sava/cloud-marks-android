package to.sava.cloudmarksandroid.databases

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ByteArrayConverterTest {

    private val converter = ByteArrayConverter()

    @Nested
    inner class RoundTrip {

        /** ByteArray → Base64 → ByteArray の往復変換が一致する */
        @Test
        fun roundTrip() {
            val original = byteArrayOf(0, 1, 2, 127, -128, -1)
            val encoded = converter.fromByteArray(original)
            val decoded = converter.toByteArray(encoded)
            assertArrayEquals(original, decoded)
        }

        /** 空の ByteArray も変換できる */
        @Test
        fun emptyArray() {
            val original = byteArrayOf()
            val encoded = converter.fromByteArray(original)
            assertEquals("", encoded)
            assertArrayEquals(original, converter.toByteArray(encoded))
        }
    }

    @Nested
    inner class FromByteArray {

        /** 既知の値が正しい Base64 文字列に変換される */
        @Test
        fun knownValue() {
            val input = "Hello".toByteArray()
            assertEquals("SGVsbG8=", converter.fromByteArray(input))
        }
    }

    @Nested
    inner class ToByteArray {

        /** 既知の Base64 文字列が正しい ByteArray にデコードされる */
        @Test
        fun knownValue() {
            val result = converter.toByteArray("SGVsbG8=")
            assertEquals("Hello", String(result))
        }
    }
}
