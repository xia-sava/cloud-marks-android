package to.sava.cloudmarksandroid.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import to.sava.cloudmarksandroid.models.Favicon
import java.nio.ByteBuffer
import javax.inject.Inject

class FaviconRepository @Inject constructor(private val context: Context) {
    private val access by lazy { RealmAccess(Favicon::class) }

    private fun findFavicon(domain: String): Favicon? {
        return access.find("domain", domain)
    }

    fun saveFavicons(favicons: List<Favicon>) {
        access.save(favicons)
    }

    fun findFaviconDrawable(domain: String): Drawable? {
        return findFavicon(domain)?.let { favicon ->
            val bitmap = Bitmap.createBitmap(favicon.size, favicon.size, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(favicon.favicon))
            BitmapDrawable(context.resources, bitmap)
        }
    }
}
