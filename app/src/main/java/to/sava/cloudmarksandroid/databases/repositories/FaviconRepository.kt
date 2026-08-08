package to.sava.cloudmarksandroid.databases.repositories

import android.graphics.BitmapFactory
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import to.sava.cloudmarksandroid.databases.dao.FaviconDao
import to.sava.cloudmarksandroid.databases.models.Favicon
import java.io.IOException
import java.nio.ByteBuffer

/** favicon を取りに行くのを諦めるまでの時間． */
private const val FETCH_TIMEOUT_MILLIS = 5000L

class FaviconRepository(
    private val access: FaviconDao,
    private val httpClient: HttpClient,
) {

    suspend fun findFavicon(domain: String): Favicon? {
        return access.findFavicon(domain)
    }

    suspend fun findFavicons(domains: List<String>): List<Favicon> {
        return access.findFavicons(domains)
    }

    suspend fun findAllFavicons(): List<Favicon> {
        return access.findAllFavicons()
    }

    suspend fun saveFavicon(favicon: Favicon): Long {
        return access.save(favicon)
    }

    suspend fun saveFavicons(favicons: List<Favicon>): List<Long> {
        return access.save(favicons)
    }

    suspend fun fetchFavicon(domain: String): Favicon? {
        val image = fetchFaviconBytes(domain) ?: return null
        return BitmapFactory.decodeByteArray(image, 0, image.size)?.let { bitmap ->
            ByteBuffer.allocate(bitmap.byteCount)
                .also { bitmap.copyPixelsToBuffer(it) }
                .let { pixels ->
                    Favicon(domain, pixels.array(), Integer.max(bitmap.height, bitmap.width))
                }
        }
    }

    /**
     * favicon の配信元を順に当たり，最初に取れた画像のバイト列を返す．
     * どこからも取れなければ null を返す．
     */
    internal suspend fun fetchFaviconBytes(domain: String): ByteArray? =
        withTimeoutOrNull(FETCH_TIMEOUT_MILLIS) {
            faviconUrls(domain).firstNotNullOfOrNull { url -> fetchBytes(url) }
        }

    private fun faviconUrls(domain: String) = listOf(
        "https://cdn-ak.favicon.st-hatena.com/?url=https://$domain",
        "https://cdn-ak.favicon.st-hatena.com/?url=http://$domain",
        "https://www.google.com/s2/favicons?domain=$domain",
    )

    private suspend fun fetchBytes(url: String): ByteArray? =
        try {
            httpClient.get(url)
                .takeIf { it.status.isSuccess() }
                ?.readRawBytes()
                ?.takeIf { it.isNotEmpty() }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: IOException) {
            null
        }
}
