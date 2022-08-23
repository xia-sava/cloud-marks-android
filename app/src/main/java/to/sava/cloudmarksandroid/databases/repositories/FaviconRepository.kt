package to.sava.cloudmarksandroid.databases.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import to.sava.cloudmarksandroid.databases.dao.FaviconDao
import to.sava.cloudmarksandroid.databases.models.Favicon
import java.nio.ByteBuffer

class FaviconRepository(
    private val context: Context,
    private val access: FaviconDao
) {

    suspend fun findFavicon(domain: String): Favicon? {
        return access.findFavicon(domain)
    }

    suspend fun findFavicons(domains: List<String>): List<Favicon> {
        return access.findFavicons(domains)
    }

    suspend fun saveFavicons(favicons: List<Favicon>): List<Long> {
        return access.save(favicons)
    }

    suspend fun findFaviconDrawable(domain: String): Drawable? {
        return findFavicon(domain)?.let { favicon ->
            val bitmap = Bitmap.createBitmap(favicon.size, favicon.size, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(favicon.favicon))
            BitmapDrawable(context.resources, bitmap)
        }
    }
}
