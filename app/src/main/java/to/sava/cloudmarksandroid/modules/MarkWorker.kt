package to.sava.cloudmarksandroid.modules

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.LifecycleOwner
import androidx.work.*
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.ui.MainActivity

private const val NOTIFICATION_ID = 1001
private const val NOTIFICATION_FAILURE_ID = 1002
private const val NOTIFICATION_CHANNEL_ID = "CMA_PROGRESS"
private const val NOTIFICATION_CHANNEL_NAME = "Cloud Marks Android 処理状況"


fun enqueueMarkLoader(
    action: MarkWorker.Action,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onWorkerEnds: (workInfo: WorkInfo) -> Unit = {},
) {
    val request = OneTimeWorkRequestBuilder<MarkWorker>()
        .setInputData(workDataOf("action" to action.name))
        .build()
    val wm = WorkManager.getInstance(context)
    wm.getWorkInfoByIdLiveData(request.id)
        .observe(lifecycleOwner) { workInfo ->
            if (workInfo.state.isFinished) {
                onWorkerEnds(workInfo)
            }
        }
    wm.enqueue(request)
}

@HiltWorker
class MarkWorker @AssistedInject constructor(
    val marks: Marks,
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    enum class Action {
        LOAD,
        SAVE,
        MERGE;

        val actionName
            get() = when (this) {
                LOAD -> "ロード"
                SAVE -> "セーブ"
                MERGE -> "マージ"
            }
    }

    override suspend fun doWork(): Result {
        val action = Action.valueOf(inputData.keyValueMap["action"].toString())
        try {
            setForeground(
                createNotification(
                    "ブックマークを${action.actionName}しています",
                    "読込中……",
                    0,
                )
            )
            when (action) {
                Action.LOAD -> loadAction()
                else -> {}
            }
            withContext(Dispatchers.Main) {
                applicationContext.toast(
                    "Cloud Marks Android: ${action.actionName}処理が完了しました。",
                    Toast.LENGTH_LONG,
                )
            }
            return Result.success()

        } catch (ex: Exception) {
            when (ex) {
                is ServiceAuthenticationException, is GoogleAuthIOException -> Pair(
                    "認証エラーが発生しました",
                    "認証処理でエラーが発生しました。\n設定画面から認証をやり直してみてください。",
                )
                else -> Pair(
                    "何かエラーが発生しました",
                    "${ex::class.java.name}\n${ex.message}",
                )
            }.let { (title, text) ->
                showFailedStickyNotification(title, text)
            }
            return Result.failure()
        }
    }

    private suspend fun loadAction() {

        marks.progressListener = { folder: String, percent: Int ->
            setProgress(workDataOf("percent" to percent))
            setForeground(
                createNotification(
                    "ブックマークをロードしています",
                    "フォルダ $folder を処理しています……",
                    percent,
                )
            )
        }
        marks.load()
    }

    private fun createNotification(
        title: String,
        progressText: String,
        percent: Int,
    ): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_cloud_download_black_24dp)
            .setContentTitle(title)
            .setProgress(100, percent, false)
            .setStyle(NotificationCompat.BigTextStyle().bigText(progressText))
            .build()
            .let {
                return ForegroundInfo(NOTIFICATION_ID, it)
            }
    }

    private fun showFailedStickyNotification(title: String, text: String) {
        NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cloud_circle_black_24dp)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(
                PendingIntent.getActivity(
                    applicationContext,
                    1,
                    Intent(appContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_ONE_SHOT
                )
            )
            .build()
            .let {
                applicationContext.notificationManager.notify(
                    NOTIFICATION_FAILURE_ID,
                    it,
                )
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        applicationContext.notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

}
