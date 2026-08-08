package to.sava.cloudmarksandroid.update

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManifestSignatureTest {

    private val keyPair = generateTestKeyPair()
    private val manifest = """{"android":{"versionCode":1,"versionName":"1.0.0","sha256":"ab"}}"""
        .toByteArray()

    /** 対応する鍵で作った署名は検証を通る */
    @Test
    fun validSignature() {
        val signature = keyPair.signBase64(manifest)
        assertTrue(verifyManifestSignature(manifest, signature, keyPair.publicKeyBase64()))
    }

    /** 署名した後に中身が変わっていれば検証を通さない */
    @Test
    fun tamperedManifest() {
        val signature = keyPair.signBase64(manifest)
        val tampered = manifest.decodeToString().replace("1.0.0", "9.9.9").toByteArray()
        assertFalse(verifyManifestSignature(tampered, signature, keyPair.publicKeyBase64()))
    }

    /** 別の鍵で作った署名は検証を通さない */
    @Test
    fun signatureFromAnotherKey() {
        val signature = generateTestKeyPair().signBase64(manifest)
        assertFalse(verifyManifestSignature(manifest, signature, keyPair.publicKeyBase64()))
    }

    /** 配信の都合で前後に空白が付いた署名も受け付ける */
    @Test
    fun signatureWithSurroundingWhitespace() {
        val signature = keyPair.signBase64(manifest)
        assertTrue(verifyManifestSignature(manifest, "\n$signature\n", keyPair.publicKeyBase64()))
    }

    /** base64 として読めない署名は検証を通さない */
    @Test
    fun signatureIsNotBase64() {
        assertFalse(verifyManifestSignature(manifest, "!!not base64!!", keyPair.publicKeyBase64()))
    }

    /** 鍵が空のあいだは検証を通さない */
    @Test
    fun emptyPublicKey() {
        val signature = keyPair.signBase64(manifest)
        assertFalse(verifyManifestSignature(manifest, signature, ""))
    }

    /** 公開鍵として読めない値では検証を通さない */
    @Test
    fun malformedPublicKey() {
        val signature = keyPair.signBase64(manifest)
        assertFalse(verifyManifestSignature(manifest, signature, "bm90IGEga2V5"))
    }
}
