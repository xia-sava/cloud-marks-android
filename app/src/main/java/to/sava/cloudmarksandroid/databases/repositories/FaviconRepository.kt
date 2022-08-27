package to.sava.cloudmarksandroid.databases.repositories

import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import to.sava.cloudmarksandroid.databases.dao.FaviconDao
import to.sava.cloudmarksandroid.databases.models.Favicon
import java.net.URL
import java.nio.ByteBuffer

class FaviconRepository(
    private val access: FaviconDao
) {

    suspend fun findFavicon(domain: String): Favicon? {
        return access.findFavicon(domain)
    }

    suspend fun findFavicons(domains: List<String>): List<Favicon> {
        return access.findFavicons(domains)
    }

    suspend fun saveFavicon(favicon: Favicon): Long {
        return access.save(favicon)
    }

    suspend fun saveFavicons(favicons: List<Favicon>): List<Long> {
        return access.save(favicons)
    }

    suspend fun fetchFavicon(domain: String): Favicon? {
        return withTimeoutOrNull(5000) {
            val faviconUrl = "https://cdn-ak.favicon.st-hatena.com/?url=https://$domain"
//            val faviconUrl = "https://www.google.com/s2/favicons?domain=$domain"
            withContext(Dispatchers.IO) {
                BitmapFactory.decodeStream(URL(faviconUrl).openStream())
            }.let { bitmap ->
                ByteBuffer.allocate(bitmap.byteCount).also {
                    bitmap.copyPixelsToBuffer(it)
                }.let { bytes ->
                    Favicon(domain, bytes.array(), Integer.max(bitmap.height, bitmap.width))
                }
            }
        }
    }
}
