package to.sava.cloudmarksandroid.update

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

/** テスト用の ECDSA P-256 鍵ペアを作る */
fun generateTestKeyPair(): KeyPair =
    KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()

/** 公開鍵を X.509 SubjectPublicKeyInfo の DER を base64 にした形で返す */
fun KeyPair.publicKeyBase64(): String =
    Base64.getEncoder().encodeToString(public.encoded)

/** 与えたバイト列へ署名し、DER 署名を base64 にして返す */
fun KeyPair.signBase64(data: ByteArray): String =
    Signature.getInstance("SHA256withECDSA").run {
        initSign(private)
        update(data)
        Base64.getEncoder().encodeToString(sign())
    }
