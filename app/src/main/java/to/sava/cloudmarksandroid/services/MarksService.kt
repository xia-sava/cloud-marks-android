package to.sava.cloudmarksandroid.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.os.Build
import android.os.Handler
import android.support.v4.app.JobIntentService
import android.support.v4.app.NotificationCompat
import com.crashlytics.android.Crashlytics
import com.google.android.gms.auth.GoogleAuthException
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException
import io.realm.Realm
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.toast
import to.sava.cloudmarksandroid.CloudMarksAndroidApplication
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.activities.SettingsActivity
import to.sava.cloudmarksandroid.libs.Marks
import to.sava.cloudmarksandroid.libs.ServiceAuthenticationException


internal enum class Action {
    LOAD,
    SAVE,
    MERGE,
}

class MarksService : JobIntentService() {
    companion object {
        var onMarksServiceCompleteListener: OnMarksServiceCompleteListener? = null

        @JvmStatic
        fun startActionLoad(context: Context) {
            startAction(context, Action.LOAD)
        }

//        @JvmStatic
//        fun startActionSave(context: Context) {
//            startAction(context, Action.SAVE)
//        }

//        @JvmStatic
//        fun startActionMerge(context: Context) {
//            startAction(context, Action.MERGE)
//        }

        private fun startAction(context: Context, action: Action) {
            if (context is OnMarksServiceCompleteListener) {
                this.onMarksServiceCompleteListener = context
            }
            val intent = Intent(context, MarksService::class.java).apply {
                this.action = action.toString()
            }
            enqueueWork(context, MarksService::class.java, JOB_ID, intent)
        }

        private const val JOB_ID = 1001
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "CMA_PROGRESS"
        const val NOTIFICATION_CHANNEL_NAME = "Cloud Marks Android 処理状況"
    }

    interface OnMarksServiceCompleteListener {
        fun onMarksServiceComplete()
    }

    private val handler = Handler()

    override fun onHandleWork(intent: Intent) {
        intent.action?.let {
            val action = Action.valueOf(it)
            val rc = handleAction(action)

            if (rc) {
                handler.post {
                    toast(R.string.marks_service_action_done)
                }
            }
        }
    }

    private fun handleAction(action: Action): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        var completeNotification: Notification? = null
        var rc = false
        try {
            when (action) {
                Action.LOAD -> {
                    CloudMarksAndroidApplication.instance.loading = true

                    val progressNotificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).apply {
                        setOngoing(true)
                        setSmallIcon(R.drawable.ic_cloud_download_black_24dp)
                        setContentTitle(getString(R.string.marks_service_progress_notification_title, getString(R.string.marks_service_action_load_title)))
                        setProgress(100, 0, true)
                    }
                    startForeground(NOTIFICATION_ID, progressNotificationBuilder.build())

                    Realm.getDefaultInstance().use { realm ->
                        val marks = Marks(realm)
                        marks.progressListener = {folder: String, percent: Int ->
                            NotificationCompat.BigTextStyle(progressNotificationBuilder).bigText(
                                    getString(R.string.marks_service_progress_folder, folder))
                            progressNotificationBuilder.setProgress(100, percent, false)
                            startForeground(NOTIFICATION_ID, progressNotificationBuilder.build())
                        }
                        marks.load()
                    }
                }
                else -> {}
            }
            rc = true
        }
        catch (ex: Exception) {
            val contentTitle: String
            val contentText: String
            when (ex) {
                is ServiceAuthenticationException, is GoogleAuthException, is GoogleAuthIOException -> {
                    contentTitle = getString(R.string.marks_service_auth_error_title)
                    contentText = getString(R.string.marks_service_auth_error_text)
                }
                else -> {
                    contentTitle = getString(R.string.marks_service_error_title)
                    contentText = "${ex::class.java.name}\n${ex.message}"
                    Crashlytics.logException(ex)
                }
            }
            completeNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).apply {
                setSmallIcon(R.drawable.ic_cloud_circle_black_24dp)
                setAutoCancel(true)
                setContentTitle(contentTitle)
                NotificationCompat.BigTextStyle(this).bigText(contentText)
                val intentNext = Intent(this@MarksService, SettingsActivity::class.java)
                intentNext.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                setContentIntent(PendingIntent.getActivity(this@MarksService, 1, intentNext, PendingIntent.FLAG_ONE_SHOT))
            }.build()
        }
        finally {
            CloudMarksAndroidApplication.instance.processing = false
        }

        stopForeground(true)

        completeNotification?.let {
            notificationManager.notify(NOTIFICATION_ID, it)
        }

        onMarksServiceCompleteListener?.onMarksServiceComplete()

        return rc
    }
}
