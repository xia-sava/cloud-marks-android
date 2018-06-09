package to.sava.cloudmarksandroid.services

import android.app.Activity
import android.app.IntentService
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.os.Handler
import android.support.v4.app.NotificationCompat
import io.realm.Realm
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.toast
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.activities.MainActivity
import to.sava.cloudmarksandroid.activities.SettingsActivity
import to.sava.cloudmarksandroid.libs.Marks
import to.sava.cloudmarksandroid.libs.ServiceAuthenticationException
import to.sava.cloudmarksandroid.libs.Settings


internal enum class Action {
    LOAD,
    SAVE,
    MERGE,
}

internal class ActionParams(
        val startNotificationTitle: String,
        val startNotificationIcon: Int,
        val completeNotificationTitle: String
)


class MarksIntentService : IntentService("CloudMarksIntentService") {

    companion object {
        @JvmStatic
        fun startActionLoad(context: Context) {
            startAction(context, Action.LOAD)
        }

        @JvmStatic
        fun startActionSave(context: Context) {
            startAction(context, Action.SAVE)
        }

        @JvmStatic
        fun startActionMerge(context: Context) {
            startAction(context, Action.MERGE)
        }

        private fun startAction(context: Context, action: Action) {
            val intent = Intent(context, MarksIntentService::class.java).apply {
                this.action = action.toString()
            }
            context.startService(intent)
        }

        const val NOTIFICATION_ID = 1001
    }

    private val handler = Handler()

    private fun params(action: Action): ActionParams {
        return when (action) {
            Action.LOAD -> ActionParams(
                    "ブックマークをロードしています",
                    R.drawable.ic_cloud_download_black_24dp,
                    "ブックマークをロードしました"
            )
            Action.SAVE -> ActionParams(
                    "ブックマークをセーブしています",
                    R.drawable.ic_cloud_download_black_24dp,
                    "ブックマークをセーブしました"
            )
            Action.MERGE ->ActionParams(
                    "ブックマークをマージしています",
                    R.drawable.ic_cloud_download_black_24dp,
                    "ブックマークをマージしました"
            )
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        intent?.action?.let {
            val action = Action.valueOf(it)
            handleAction(action)

            handler.post({
                toast("終わったよ！")
            })
        }
    }

    private fun handleAction(action: Action) {
        val params = params(action)

        // TODO: 開始時に，既に何か notification が表示されていたらそれを消したい

        val startNotification = NotificationCompat.Builder(this).apply {
            setSmallIcon(params.startNotificationIcon)
            setContentTitle(params.startNotificationTitle)
            setProgress(0, 0, true)
        }.build()

        startForeground(NOTIFICATION_ID, startNotification)

        val completeNotificationBuilder = NotificationCompat.Builder(this).apply {
            setSmallIcon(R.drawable.ic_cloud_circle_black_24dp)
            setContentTitle(params.completeNotificationTitle)
            setAutoCancel(true)
        }
        var nextActivity: Class<out Activity> = MainActivity::class.java
        try {
            when (action) {
                Action.LOAD -> {
                    Settings().context.loading = true
                    try {
                        Realm.getDefaultInstance().use { realm ->
                            Marks(realm).load()
                        }
                    } finally {
                        Settings().context.loading = false
                    }
                }
                else -> {}
            }
        }
        catch (authEx: ServiceAuthenticationException) {
            completeNotificationBuilder.apply {
                setContentTitle(getString(R.string.service_auth_error_title))
                NotificationCompat.BigTextStyle(completeNotificationBuilder).bigText(getString(R.string.service_auth_error_text))
            }
            nextActivity = SettingsActivity::class.java
        }
        catch (ex: Exception) {
            completeNotificationBuilder.apply {
                setContentTitle(getString(R.string.service_error_title))
                NotificationCompat.BigTextStyle(completeNotificationBuilder).bigText(ex.message)
            }
        }

        stopForeground(true)

        completeNotificationBuilder.apply {
            val intentNext = Intent(this@MarksIntentService, nextActivity)
            intentNext.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            setContentIntent(PendingIntent.getActivity(this@MarksIntentService, 1, intentNext, PendingIntent.FLAG_ONE_SHOT))
        }
        notificationManager.notify(NOTIFICATION_ID, completeNotificationBuilder.build())
    }
}
