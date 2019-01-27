package to.sava.cloudmarksandroid.libs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.coroutines.*
import org.jetbrains.anko.windowManager
import to.sava.cloudmarksandroid.models.Favicon
import java.net.URL
import to.sava.cloudmarksandroid.models.MarkNode
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import javax.inject.Inject


class FaviconLibrary @Inject constructor(private val context: Context, private val realm: Realm) {

    fun find(mark: MarkNode): Drawable? {
        return realm
                .where<Favicon>()
                .equalTo("domain", mark.domain)
                .findFirst()?.let { favicon ->
                    val bitmap = Bitmap.createBitmap(favicon.size, favicon.size, Bitmap.Config.ARGB_8888)
                    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(favicon.favicon))
                    BitmapDrawable(context.resources, bitmap)
                }
    }

    /**
     * bitmapをByteArrayに変換してFaviconオブジェクトとして保存する．
     * realm トランザクションを生成するため，
     * このメソッドは UI Thread 以外で動かすと例外発生となるので注意．
     */
    fun register(urls: List<String>) = runBlocking {
        val faviconAsyncs = urls.map { url ->
            async(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                fetchFavicon(url)
            }
        }
        val standAloneFavicons = awaitAll(deferreds = *(faviconAsyncs.toTypedArray()))
        standAloneFavicons.forEach { standAloneFavicon ->
            realm.executeTransaction { realm ->
                realm.copyToRealmOrUpdate(standAloneFavicon)
            }
        }
    }

    /**
     * FaviconのURLに直接アクセスして取得する．
     * Uri fetch を行なうため，
     * このメソッドは UI Thread で動かすと例外発生となるので注意．
     */
    private fun fetchFavicon(siteUrl: String): Favicon {
        val faviconUrl = faviconUrl(siteUrl)
        val binary = URL(faviconUrl).openStream()
        val bitmap = BitmapFactory.decodeStream(binary)
        val metrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(metrics)
        val size = (24 * metrics.density).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, false)
        val bytes = ByteBuffer.allocate(scaled.byteCount)
        scaled.copyPixelsToBuffer(bytes)
        return Favicon(MarkNode.parseDomain(siteUrl), bytes.array(), size)
    }

    /**
     * Googleのfavicon取得サービスのURLを生成する

     * <s>URL を分解して path を /favicon.ico にする．</s>
     */
    private fun faviconUrl(pageUrl: String): String {
        val url = URL(pageUrl)
        return "https://www.google.com/s2/favicons?domain=${url.host}"
//        val uri = Uri.parse(pageUrl)
//        val faviconUri = Uri.Builder()
//                .scheme(uri.scheme)
//                .authority(uri.authority)
//                .path("/favicon.ico")
//                .build()
//        return faviconUri.toString()
    }
}
