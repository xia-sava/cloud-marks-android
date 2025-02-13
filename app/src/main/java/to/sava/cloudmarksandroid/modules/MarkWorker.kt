package to.sava.cloudmarksandroid.modules

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleOwner
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinWorker
import to.sava.cloudmarksandroid.CloudMarksAndroidApplication
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.ui.MainActivity

private const val NOTIFICATION_ID = 1001
private const val NOTIFICATION_FAILURE_ID = 1002
private const val NOTIFICATION_CHANNEL_ID = "CMA_PROGRESS"
private const val NOTIFICATION_CHANNEL_NAME = "Cloud Marks Android 処理状況"


fun enqueueMarkLoader(
    action: MarkWorker.Action,
    lifecycleOwner: LifecycleOwner,
    onWorkerEnds: (workInfo: WorkInfo) -> Unit = {},
) {
    MarkWorker.createChannel()
    val request = OneTimeWorkRequestBuilder<MarkWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .setInputData(workDataOf("action" to action.name))
        .build()
    CloudMarksAndroidApplication.instance.workerManager
        .apply {
            getWorkInfoByIdLiveData(request.id)
                .observe(lifecycleOwner) { workInfo ->
                    if (workInfo?.state?.isFinished == true) {
                        onWorkerEnds(workInfo)
                    }
                }
        }
        .enqueue(request)
}

@KoinWorker
class MarkWorker(
    private val marks: Marks,
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {
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
            showProgressNotification(
                "ブックマークを${action.actionName}しています",
                "読込中……",
                0,
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
                applicationContext.notificationManager.cancel(NOTIFICATION_ID)
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
            showProgressNotification(
                "ブックマークをロードしています",
                "フォルダ $folder を処理しています……",
                percent,
            )
        }
        marks.load()
        marks.fetchAllFavicons()
    }

    private fun showProgressNotification(
        title: String,
        progressText: String,
        percent: Int,
    ) {
        NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_cloud_download_black_24dp)
            .setContentTitle(title)
            .setProgress(100, percent, false)
            .setStyle(NotificationCompat.BigTextStyle().bigText(progressText))
            .build()
            .let {
                applicationContext.notificationManager.notify(NOTIFICATION_ID, it)
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
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
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

    companion object {
        fun createChannel() {
            CloudMarksAndroidApplication.instance
                .notificationManager
                .createNotificationChannel(
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        NOTIFICATION_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
        }
    }
}
