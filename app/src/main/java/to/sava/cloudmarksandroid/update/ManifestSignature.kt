package to.sava.cloudmarksandroid.update

import android.util.Log
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** マニフェストの署名方式．minSdk 30 の Android が標準で持つ． */
private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
private const val KEY_ALGORITHM = "EC"

private const val LOG_TAG = "ManifestSignature"

/**
 * 配布元がマニフェストへ付ける署名の検証鍵．
 * X.509 SubjectPublicKeyInfo の DER を base64 にした ECDSA P-256 の公開鍵．
 * 配布経路(TLS)ともマニフェストの中身とも独立した信頼の起点になる．
 *
 * TODO: 配布用の鍵ペアを作り，その公開鍵に差し替える．空のあいだ更新確認は必ず失敗する．
 */
const val MANIFEST_PUBLIC_KEY: String = ""

/**
 * マニフェストの生バイト [manifest] に対する [signature](DER署名をbase64にしたもの)を
 * [publicKey] で検証する．署名が読めない・鍵が合わない・バイト列が食い違うときは false を返す．
 */
fun verifyManifestSignature(
    manifest: ByteArray,
    signature: String,
    publicKey: String,
): Boolean =
    try {
        Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initVerify(publicKeyOf(publicKey))
            update(manifest)
            verify(decodeBase64(signature))
        }
    } catch (error: GeneralSecurityException) {
        Log.w(LOG_TAG, "マニフェストの署名を検証できませんでした", error)
        false
    } catch (error: IllegalArgumentException) {
        Log.w(LOG_TAG, "マニフェストの署名がbase64ではありません", error)
        false
    }

private fun publicKeyOf(base64: String): PublicKey =
    KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(X509EncodedKeySpec(decodeBase64(base64)))

/** 前後の空白を落としてから復号する．配信の都合で末尾に改行が付くことがある． */
private fun decodeBase64(text: String): ByteArray = Base64.getDecoder().decode(text.trim())
