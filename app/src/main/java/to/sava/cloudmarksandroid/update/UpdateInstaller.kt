package to.sava.cloudmarksandroid.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.ktor.client.HttpClient
import java.io.File
import java.io.IOException

/** APKのMIMEタイプ． */
private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

/** FileProvider の authority 接尾辞．AndroidManifest の provider と一致させる． */
private const val FILE_PROVIDER_SUFFIX = ".updates"

/** ダウンロード先ディレクトリ名．res/xml/file_paths.xml の cache-path と一致させる． */
private const val DOWNLOAD_DIR = "updates"

/** ダウンロードしたAPKのファイル名． */
private const val APK_FILE_NAME = "cloud-marks-update.apk"

/**
 * ダウンロードしたAPKの packageName が自アプリと一致するかを判定する．
 * [actualPackage] は getPackageArchiveInfo が解析できないと null になる．
 */
fun isExpectedApkPackage(actualPackage: String?, expectedPackage: String): Boolean =
    actualPackage != null && actualPackage == expectedPackage

/**
 * APKをアプリ専用領域へ落とし，照合してから FileProvider 経由でインストール確認のIntentを発行する．
 * 外から落としたAPKは，HTTPステータス・sha256・packageName を検証してからインストールへ回す．
 */
class UpdateInstaller(
    private val context: Context,
    private val httpClient: HttpClient,
) {

    /**
     * 配布物をダウンロードしてアプリ専用領域へ置く．
     * 受信量を [onProgress] へ逐次知らせる(全体長が判らなければ 0 を渡す)．
     */
    suspend fun download(
        url: String,
        onProgress: (received: Long, total: Long) -> Unit = { _, _ -> },
    ): File {
        val dir = File(context.cacheDir, DOWNLOAD_DIR).apply { mkdirs() }
        val file = File(dir, APK_FILE_NAME)
        httpClient.downloadToFile(url, file, onProgress = onProgress)
        return file
    }

    /**
     * ダウンロード物が配布元の意図した実体で，かつ自アプリのAPKであることを確かめる．
     * どちらかが食い違えば破棄して中断する．
     */
    fun verify(apk: File, expectedSha256: String) {
        if (!matchesSha256(apk, expectedSha256)) {
            apk.delete()
            throw IOException("配布物の照合値が合いません")
        }
        val packageName = context.packageManager
            .getPackageArchiveInfo(apk.absolutePath, 0)
            ?.packageName
        if (!isExpectedApkPackage(packageName, context.packageName)) {
            apk.delete()
            throw IOException("配布物のパッケージ名が違います")
        }
    }

    /** インストール確認のIntentを発行する．実際のインストールは利用者が承認して進む． */
    fun launch(apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}$FILE_PROVIDER_SUFFIX",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
