package to.sava.cloudmarksandroid.services

import android.app.IntentService
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.os.Handler
import android.support.v4.app.NotificationCompat
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

class MarksIntentService : IntentService("CloudMarksIntentService") {

    companion object {
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
            val intent = Intent(context, MarksIntentService::class.java).apply {
                this.action = action.toString()
            }
            context.startService(intent)
        }

        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL = ""
    }

    private val handler = Handler()

    override fun onHandleIntent(intent: Intent?) {
        intent?.action?.let {
            val action = Action.valueOf(it)
            val rc = handleAction(action)

            if (rc) {
                handler.post {
                    toast(R.string.service_action_done)
                }
            }
        }
    }

    private fun handleAction(action: Action): Boolean {
        var completeNotification: Notification? = null
        var rc = false
        try {
            when (action) {
                Action.LOAD -> {
                    CloudMarksAndroidApplication.instance.loading = true

                    val progressNotificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL).apply {
                        setOngoing(true)
                        setSmallIcon(R.drawable.ic_cloud_download_black_24dp)
                        setContentTitle(getString(R.string.service_progress_notification_title, getString(R.string.service_action_load_title)))
                        setProgress(100, 0, true)
                    }
                    startForeground(NOTIFICATION_ID, progressNotificationBuilder.build())

                    Realm.getDefaultInstance().use { realm ->
                        val marks = Marks(realm)
                        marks.progressListener = {folder: String, percent: Int ->
                            NotificationCompat.BigTextStyle(progressNotificationBuilder).bigText(
                                    getString(R.string.service_progress_folder, folder))
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
        catch (authEx: ServiceAuthenticationException) {
            completeNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL).apply {
                setSmallIcon(R.drawable.ic_cloud_circle_black_24dp)
                setContentTitle(getString(R.string.service_auth_error_title))
                NotificationCompat.BigTextStyle(this).bigText(getString(R.string.service_auth_error_text))
                val intentNext = Intent(this@MarksIntentService, SettingsActivity::class.java)
                intentNext.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                setContentIntent(PendingIntent.getActivity(this@MarksIntentService, 1, intentNext, PendingIntent.FLAG_ONE_SHOT))
            }.build()
        }
        catch (ex: Exception) {
            completeNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL).apply {
                setSmallIcon(R.drawable.ic_cloud_circle_black_24dp)
                setContentTitle(getString(R.string.service_error_title))
                NotificationCompat.BigTextStyle(this).bigText(ex.message)
                val intentNext = Intent(this@MarksIntentService, SettingsActivity::class.java)
                intentNext.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                setContentIntent(PendingIntent.getActivity(this@MarksIntentService, 1, intentNext, PendingIntent.FLAG_ONE_SHOT))
            }.build()
        }
        finally {
            CloudMarksAndroidApplication.instance.processing = false
        }

        stopForeground(true)

        completeNotification?.let {
            notificationManager.notify(NOTIFICATION_ID, it)
        }

        return rc
    }
}
