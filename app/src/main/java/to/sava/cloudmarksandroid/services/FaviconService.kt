package to.sava.cloudmarksandroid.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.util.DisplayMetrics
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import dagger.android.AndroidInjection
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.toast
import org.jetbrains.anko.windowManager
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.models.Favicon
import to.sava.cloudmarksandroid.repositories.FaviconRepository
import to.sava.cloudmarksandroid.repositories.MarkNodeRepository
import java.net.URL
import java.nio.ByteBuffer
import javax.inject.Inject

class FaviconService : JobIntentService() {
    companion object {
        @JvmStatic
        fun startAction(context: Context, markId: String) {
            val intent = Intent(context, FaviconService::class.java).apply {
                this.putExtra("id", markId)
            }
            enqueueWork(context, FaviconService::class.java, JOB_ID, intent)
        }

        private const val JOB_ID = 2001
        const val NOTIFICATION_ID = 2001
        const val NOTIFICATION_CHANNEL_ID = "CMA_PROGRESS"
        const val NOTIFICATION_CHANNEL_NAME = "Cloud Marks Android 処理状況"
    }

    data class FaviconServiceCompleteEvent(val urls: List<String>)

    @Inject
    internal lateinit var faviconRepos: FaviconRepository

    @Inject
    internal lateinit var marksRepos: MarkNodeRepository

    private val handler = Handler()

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    override fun onHandleWork(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).apply {
            setOngoing(true)
            setSmallIcon(R.drawable.ic_cloud_download_black_24dp)
            setContentTitle(getString(R.string.favicon_service_progress_notification_title))
        }
        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        val markId = intent.getStringExtra("id")
        val domains = marksRepos.getUniqueListOfFaviconDomains(markId)

        val favicons = runBlocking {
            val deferredFavicons = domains.map { domain ->
                async {
                    withTimeoutOrNull(5000) {
                        val faviconUrl = "https://www.google.com/s2/favicons?domain=$domain"
                        val bitmap = BitmapFactory.decodeStream(URL(faviconUrl).openStream())
                        val metrics = DisplayMetrics().apply {
                            windowManager.defaultDisplay.getMetrics(this)
                        }
                        val size = (24 * metrics.density).toInt()
                        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, false)
                        val bytes = ByteBuffer.allocate(scaled.byteCount)
                        scaled.copyPixelsToBuffer(bytes)
                        Favicon(domain, bytes.array(), size)
                    }
                }
            }
            awaitAll(*(deferredFavicons.toTypedArray())).filterNotNull()
        }

        faviconRepos.saveFavicons(favicons)

        // 終了通知
        EventBus.getDefault().postSticky(FaviconServiceCompleteEvent(domains))
        handler.post {
            toast("${getString(R.string.app_name)}: ${getString(R.string.favicon_toast_fetch)}${getString(R.string.marks_service_action_done)}")
        }
    }
}
