package to.sava.cloudmarksandroid.update

import kotlinx.serialization.Serializable

/**
 * 配布物の版と照合値．
 * 配布物の所在はアプリが固定の配布元から組み立てるため，マニフェストからは受け取らない．
 */
@Serializable
class ReleaseInfo(
    val versionCode: Int,
    val versionName: String,
    /** 配布物のSHA-256(16進)．ダウンロードした実体の照合に使う． */
    val sha256: String,
)

/**
 * latest.json の構造．
 * 配布元が後から項目を足しても読み進められるよう，知らないフィールドは読み飛ばす．
 */
@Serializable
class LatestManifest(
    val android: ReleaseInfo,
)
