package to.sava.cloudmarksandroid.databases.repositories

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import to.sava.cloudmarksandroid.databases.dao.FaviconDao

class FaviconRepositoryTest {

    /** 与えたハンドラで応答する FaviconRepository を組み立てる */
    private fun repositoryWith(
        handler: MockRequestHandler,
    ): Pair<FaviconRepository, MockEngine> {
        val engine = MockEngine(handler)
        return FaviconRepository(mockk<FaviconDao>(), HttpClient(engine)) to engine
    }

    /**
     * fetchFaviconBytes は実時間で打ち切りを掛けるため、仮想時間を進める runTest ではなく
     * runBlocking で検証する。runTest では HTTP の完了を待つ間に仮想時間が進み、
     * リクエストが飛ぶ前に打ち切られてしまう。
     */
    @Nested
    inner class FetchFaviconBytes {

        /** 最初の配信元が返した画像をそのまま返す */
        @Test
        fun returnsBytesFromFirstSource() = runBlocking {
            val image = byteArrayOf(1, 2, 3)
            val (repository, engine) = repositoryWith { _ -> respond(image) }

            assertArrayEquals(image, repository.fetchFaviconBytes("example.com"))
            assertEquals(1, engine.requestHistory.size)
        }

        /** 最初の配信元が 404 を返したら次の配信元を当たる */
        @Test
        fun fallsBackToNextSource() = runBlocking {
            val image = byteArrayOf(4, 5, 6)
            val (repository, engine) = repositoryWith { request ->
                if (request.url.toString().contains("st-hatena")) {
                    respond(ByteArray(0), HttpStatusCode.NotFound)
                } else {
                    respond(image)
                }
            }

            assertArrayEquals(image, repository.fetchFaviconBytes("example.com"))
            assertEquals(3, engine.requestHistory.size)
        }

        /** 中身が空の応答は取得できなかったものとして次の配信元を当たる */
        @Test
        fun treatsEmptyBodyAsFailure() = runBlocking {
            val image = byteArrayOf(7)
            val (repository, engine) = repositoryWith { request ->
                if (request.url.toString().contains("st-hatena")) {
                    respond(ByteArray(0))
                } else {
                    respond(image)
                }
            }

            assertArrayEquals(image, repository.fetchFaviconBytes("example.com"))
            assertEquals(3, engine.requestHistory.size)
        }

        /** どの配信元からも取れなければ null を返す */
        @Test
        fun returnsNullWhenAllSourcesFail() = runBlocking {
            val (repository, engine) =
                repositoryWith { _ -> respond(ByteArray(0), HttpStatusCode.NotFound) }

            assertNull(repository.fetchFaviconBytes("example.com"))
            assertEquals(3, engine.requestHistory.size)
        }

        /** 問い合わせ先のURLにドメインを埋めて渡す */
        @Test
        fun passesDomainToEachSource() = runBlocking {
            val (repository, engine) =
                repositoryWith { _ -> respond(ByteArray(0), HttpStatusCode.NotFound) }

            repository.fetchFaviconBytes("example.com")

            assertEquals(
                listOf(
                    "https://cdn-ak.favicon.st-hatena.com/?url=https://example.com",
                    "https://cdn-ak.favicon.st-hatena.com/?url=http://example.com",
                    "https://www.google.com/s2/favicons?domain=example.com",
                ),
                engine.requestHistory.map { it.url.toString() },
            )
        }
    }
}
