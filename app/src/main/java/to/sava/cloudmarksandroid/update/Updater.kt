package to.sava.cloudmarksandroid.update

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException

private const val LOG_TAG = "Updater"

/**
 * 自己更新の入り口．更新確認と適用の手続きを提供する．
 * 進み具合は呼び出し側へ渡すだけで，状態そのものは持たない．
 */
class Updater(
    context: Context,
    private val httpClient: HttpClient,
) {
    private val applicationContext = context.applicationContext

    private val packageInfo =
        applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)

    private val installer = UpdateInstaller(applicationContext, httpClient)

    /** 実行中の版の表示名． */
    val currentVersionName: String = packageInfo.versionName ?: ""

    /**
     * 配布物として動いているか．
     * 開発ビルドは配布版より版数が古く署名も食い違うため，確認できても適用は必ず失敗する．
     */
    val updatable: Boolean =
        (applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0

    /** 配布元のマニフェストを引いて，実行中の版と比べる． */
    suspend fun check(): UpdateStatus =
        UpdateChecker(httpClient, packageInfo.longVersionCode.toInt()).check()

    /**
     * APKをダウンロードして照合し，インストール確認のIntentを発行する．
     * インストールの可否は利用者が確認画面で決めるため，ここでは発行までを担う．
     */
    suspend fun install(
        available: UpdateStatus.Available,
        onState: (UpdateInstallState) -> Unit,
    ) {
        try {
            onState(UpdateInstallState.Downloading(0, 0))
            val apk = installer.download(available.url) { received, total ->
                onState(UpdateInstallState.Downloading(received, total))
            }
            onState(UpdateInstallState.Verifying)
            installer.verify(apk, available.sha256)
            installer.launch(apk)
            onState(UpdateInstallState.Launching)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.e(LOG_TAG, "更新の適用に失敗しました", error)
            onState(UpdateInstallState.Failed("更新の適用に失敗しました"))
        }
    }
}
