package to.sava.cloudmarksandroid.services

import android.app.IntentService
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.os.Handler
import android.support.v4.app.NotificationCompat
import io.realm.Realm
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.toast
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.activities.MainActivity
import to.sava.cloudmarksandroid.libs.Marks


internal enum class Action {
    LOAD,
    SAVE,
    MERGE,
}

internal class ActionParams(
        val startNotificationId: Int,
        val startNotificationTitle: String,
        val startNotificationIcon: Int,
        val completeNotificationId: Int,
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
    }

    private val handler = Handler()

    private fun params(action: Action): ActionParams {
        return when (action) {
            Action.LOAD -> ActionParams(
                    1,
                    "ブックマークをロードしています",
                    R.drawable.ic_cloud_download_black_24dp,
                    2,
                    "ブックマークをロードしました"
            )
            Action.SAVE -> ActionParams(
                    101,
                    "ブックマークをセーブしています",
                    R.drawable.ic_cloud_download_black_24dp,
                    102,
                    "ブックマークをセーブしました"
            )
            Action.MERGE ->ActionParams(
                    201,
                    "ブックマークをマージしています",
                    R.drawable.ic_cloud_download_black_24dp,
                    202,
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
            setContentTitle(params.startNotificationTitle)
            setSmallIcon(params.startNotificationIcon)
            setProgress(0, 0, true)
        }.build()

        startForeground(params.startNotificationId, startNotification)

        when (action) {
            Action.LOAD -> {
                Realm.getDefaultInstance().use {realm ->
                    Marks(realm).load()
                }
//                (0..5).map {
//                    Thread.sleep(1000)
//                    Log.i("cma", "$it")
//                }
            }
            else -> {}
        }

        stopForeground(true)

        val completeNotification = NotificationCompat.Builder(this).apply {
            setContentTitle(params.completeNotificationTitle)
            setSmallIcon(R.drawable.ic_cloud_circle_black_24dp)
            val intentMain = intentFor<MainActivity>()
            intentMain.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            setContentIntent(PendingIntent.getActivity(this@MarksIntentService, 1, intentMain, PendingIntent.FLAG_ONE_SHOT))
            setAutoCancel(true)
        }.build()
        notificationManager.notify(params.completeNotificationId, completeNotification)
    }
}
