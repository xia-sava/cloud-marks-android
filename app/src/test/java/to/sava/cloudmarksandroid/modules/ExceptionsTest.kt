package to.sava.cloudmarksandroid.modules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/** 各カスタム例外のテストデータ */
fun exceptionCases() = listOf(
    Arguments.of(ServiceAuthenticationException("auth error"), "auth error"),
    Arguments.of(DirectoryNotFoundException("dir not found"), "dir not found"),
    Arguments.of(FileNotFoundException("file not found"), "file not found"),
    Arguments.of(InvalidJsonException("invalid json"), "invalid json"),
)

class ExceptionsTest {

    /** 各例外が RuntimeException を継承し、メッセージを保持する */
    @ParameterizedTest(name = "{0}")
    @MethodSource("to.sava.cloudmarksandroid.modules.ExceptionsTestKt#exceptionCases")
    fun messageAndInheritance(exception: RuntimeException, expectedMessage: String) {
        assertInstanceOf(RuntimeException::class.java, exception)
        assertEquals(expectedMessage, exception.message)
    }
}
