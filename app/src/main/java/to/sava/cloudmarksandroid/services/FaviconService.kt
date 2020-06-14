package to.sava.cloudmarksandroid.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import dagger.android.AndroidInjection
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.databases.models.Favicon
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.databases.repositories.FaviconRepository
import to.sava.cloudmarksandroid.databases.repositories.MarkNodeRepository
import to.sava.cloudmarksandroid.libs.notificationManager
import to.sava.cloudmarksandroid.libs.toast
import java.net.URL
import java.nio.ByteBuffer
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

private const val JOB_ID = 2001
private const val NOTIFICATION_ID = 2001
private const val NOTIFICATION_CHANNEL_ID = "CMA_PROGRESS"
private const val NOTIFICATION_CHANNEL_NAME = "Cloud Marks Android 処理状況"

fun startFaviconService(context: Context, markId: Long) {
    val intent = Intent(context, FaviconService::class.java).apply {
        this.putExtra("id", markId)
    }
    JobIntentService.enqueueWork(context, FaviconService::class.java, JOB_ID, intent)
}

class FaviconService : JobIntentService(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    @Inject
    internal lateinit var faviconRepos: FaviconRepository

    @Inject
    internal lateinit var marksRepos: MarkNodeRepository

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onHandleWork(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).apply {
            setOngoing(true)
            setSmallIcon(R.drawable.ic_cloud_download_black_24dp)
            setContentTitle(getString(R.string.favicon_service_progress_notification_title))
        }
        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        runBlocking {
            val markId = intent.getLongExtra("id", MarkNode.ROOT_ID)
            val domains = marksRepos.getUniqueListOfFaviconDomains(markId)

            val favicons = domains.map { domain ->
                async {
                    withTimeoutOrNull(5000) {
                        val faviconUrl = "https://www.google.com/s2/favicons?domain=$domain"
                        val bitmap = withContext(Dispatchers.IO) {
                            BitmapFactory.decodeStream(URL(faviconUrl).openStream())
                        }
                        val size = (24 * resources.displayMetrics.density).toInt()
                        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, false)
                        val bytes = ByteBuffer.allocate(scaled.byteCount)
                        scaled.copyPixelsToBuffer(bytes)
                        Favicon(domain, bytes.array(), size)
                    }
                }
            }.awaitAll().filterNotNull()

            faviconRepos.saveFavicons(favicons)

            // 終了通知
            EventBus.getDefault().postSticky(FaviconServiceCompleteEvent(domains))
            withContext(Dispatchers.Main) {
                toast(
                    "${getString(R.string.app_name)}: ${getString(R.string.favicon_toast_fetch)}${getString(
                        R.string.marks_service_action_done
                    )}"
                )
            }
        }
    }

    data class FaviconServiceCompleteEvent(val urls: List<String>)
}
