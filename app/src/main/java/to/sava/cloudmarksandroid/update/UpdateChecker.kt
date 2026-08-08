package to.sava.cloudmarksandroid.update

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/** 配布物を置くリポジトリと，公開のたびに中身が入れ替わるリリースのタグ． */
private const val RELEASE_REPOSITORY = "xia-sava/cloud-marks-android"
private const val RELEASE_TAG = "latest"

/** 配布物のファイル名．固定名なので公開を跨いで変わらない． */
const val APK_ASSET_NAME = "cloud-marks.apk"

/** マニフェストの署名の所在．マニフェストと同じ場所に同じ名前で並べる． */
private const val MANIFEST_SIGNATURE_SUFFIX = ".sig"

/** 署名を確かめられなかったときの表示．確かめられなかったこと以上の内部事情は伝えない． */
private const val SIGNATURE_UNVERIFIED = "配布物の署名を確認できませんでした"

private const val LOG_TAG = "UpdateChecker"

/** GitHub Releases 上の配布物のURLを組む． */
fun releaseAssetUrl(assetName: String): String =
    "https://github.com/$RELEASE_REPOSITORY/releases/download/$RELEASE_TAG/$assetName"

/** latest.json の所在．設定とは無関係に固定の配布元を指す． */
val LATEST_MANIFEST_URL: String = releaseAssetUrl("latest.json")

private val json = Json { ignoreUnknownKeys = true }

/**
 * 配布元の latest.json を取得し，自分の [currentVersionCode] と比べる．
 * ネットワーク失敗・JSON不正・署名不一致はいずれも [UpdateStatus.Failed] とし，理由を握り潰さない．
 *
 * 配布物の取得先はマニフェストの指定を受け付けず，[releaseAssetUrl] で固定の配布元から組み立てる．
 * マニフェストが動かせるのは版と照合値だけになる．
 *
 * マニフェストは署名を検証してからでないと解釈しない．署名が無い・読めない・鍵が合わない場合は
 * いずれも拒否し，検証を省く経路を作らない．検証の対象は取得したバイト列そのものとする．
 *
 * [manifestUrl] と [publicKey] は取得先と信頼の起点を差し替えるための口で，
 * 配布物としての動作では固定の [LATEST_MANIFEST_URL] と [MANIFEST_PUBLIC_KEY] を使う．
 */
class UpdateChecker(
    private val httpClient: HttpClient,
    private val currentVersionCode: Int,
    private val manifestUrl: String = LATEST_MANIFEST_URL,
    private val publicKey: String = MANIFEST_PUBLIC_KEY,
) {
    suspend fun check(): UpdateStatus {
        val response = fetch(manifestUrl)
            ?: return UpdateStatus.Failed("更新情報を取得できませんでした")
        if (!response.status.isSuccess()) {
            return UpdateStatus.Failed("更新情報を取得できませんでした (HTTP ${response.status.value})")
        }
        val body = bytesOf(response)
            ?: return UpdateStatus.Failed("更新情報を取得できませんでした")
        if (!hasValidSignature(body)) {
            return UpdateStatus.Failed(SIGNATURE_UNVERIFIED)
        }
        val release = decode(body)?.android
            ?: return UpdateStatus.Failed("更新情報を解析できませんでした")
        if (release.versionCode <= currentVersionCode) {
            return UpdateStatus.UpToDate
        }
        return UpdateStatus.Available(
            release.versionName,
            releaseAssetUrl(APK_ASSET_NAME),
            release.sha256,
        )
    }

    /** マニフェストに添えられた署名を取得して照合する．取得できない署名は不正な署名と同じく拒否する． */
    private suspend fun hasValidSignature(manifest: ByteArray): Boolean =
        fetch("$manifestUrl$MANIFEST_SIGNATURE_SUFFIX")
            ?.takeIf { response -> response.status.isSuccess() }
            ?.let { response -> bytesOf(response) }
            ?.let { signature ->
                verifyManifestSignature(manifest, signature.decodeToString(), publicKey)
            }
            ?: false

    private suspend fun fetch(url: String): HttpResponse? =
        try {
            httpClient.get(url)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.w(LOG_TAG, "更新情報の取得に失敗しました", error)
            null
        }

    private suspend fun bytesOf(response: HttpResponse): ByteArray? =
        try {
            response.readRawBytes()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.w(LOG_TAG, "応答の読み取りに失敗しました", error)
            null
        }

    private fun decode(manifest: ByteArray): LatestManifest? =
        try {
            json.decodeFromString<LatestManifest>(manifest.decodeToString())
        } catch (error: Exception) {
            Log.w(LOG_TAG, "更新情報の解析に失敗しました", error)
            null
        }
}
