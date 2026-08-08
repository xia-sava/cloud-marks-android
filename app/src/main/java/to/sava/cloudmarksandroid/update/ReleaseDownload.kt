package to.sava.cloudmarksandroid.update

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest

/** 取得を許すURLスキーム．平文 http は経路上の差し替えを検出できないため受け付けない． */
private const val ALLOWED_SCHEME = "https"

/** 書き写しの読み取り単位． */
private const val COPY_BUFFER_BYTES = 64 * 1024

/** 受信量を知らせる間隔．1バイトごとに知らせると表示が過剰に更新されるため間引く． */
private const val PROGRESS_NOTIFY_BYTES = 512L * 1024

/**
 * 受け取る配布物の上限．APKの実寸に対して余裕を採った値で，
 * 際限なく書き続ける応答からディスクを守る．
 */
const val MAX_RELEASE_BYTES: Long = 200L * 1024 * 1024

/**
 * [url] が https でホストを持つ，取得してよい形式かを判定する．
 */
fun isDownloadableHttpsUrl(url: String): Boolean {
    val uri = try {
        URI(url)
    } catch (error: URISyntaxException) {
        return false
    }
    return uri.scheme?.lowercase() == ALLOWED_SCHEME && !uri.host.isNullOrEmpty()
}

/**
 * [file] の内容のSHA-256が [expected] と一致するかを判定する．
 * 数十MBになりうるため一度に読み込まず，読みながら計算する．
 */
fun matchesSha256(file: File, expected: String): Boolean {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(COPY_BUFFER_BYTES)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest()
        .joinToString("") { String.format("%02x", it) }
        .equals(expected.trim(), ignoreCase = true)
}

/**
 * [url] の中身を [file] へ書き出しながら，受信量を [onProgress] へ知らせる．
 * 受け取る量は [maxBytes] までとし，超える応答は書き込みを止めて失敗させる．
 * 途中で失敗したら書きかけを残さない．
 *
 * 応答を受け取りながら書き出す．ボディを一度メモリへ載せる読み方をすると，数十MBの配布物が
 * まるごとヒープに載るうえ，読み終わるまで受信量を知らせられず進捗が出ない．
 */
suspend fun HttpClient.downloadToFile(
    url: String,
    file: File,
    maxBytes: Long = MAX_RELEASE_BYTES,
    onProgress: (received: Long, total: Long) -> Unit = { _, _ -> },
) {
    if (!isDownloadableHttpsUrl(url)) {
        throw IOException("https のURLではありません")
    }
    try {
        prepareGet(url).execute { response ->
            if (!response.status.isSuccess()) {
                throw IOException("配布物を取得できませんでした (HTTP ${response.status.value})")
            }
            val total = response.contentLength() ?: 0L
            if (total > maxBytes) {
                throw IOException("配布物が大きすぎます ($total > $maxBytes)")
            }
            response.bodyAsChannel().toInputStream().use { input ->
                file.outputStream().buffered().use { output ->
                    copyReportingProgress(input, output, total, maxBytes, onProgress)
                }
            }
        }
    } catch (error: Exception) {
        file.delete()
        throw error
    }
}

/**
 * [input] を [output] へ書き写しながら，受信量を [onProgress] へ知らせる．
 * 通知は一定量ごとに間引き，書き写しの完了時には必ず1回知らせる．
 * [total] は全体長で，判らないときは 0 をそのまま渡す．
 * 受信量が [maxBytes] を超えたらそこで書き写しをやめる．
 */
fun copyReportingProgress(
    input: InputStream,
    output: OutputStream,
    total: Long,
    maxBytes: Long = MAX_RELEASE_BYTES,
    onProgress: (received: Long, total: Long) -> Unit,
) {
    val buffer = ByteArray(COPY_BUFFER_BYTES)
    var received = 0L
    var notified = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        if (received + read > maxBytes) {
            throw IOException("配布物が大きすぎます ($maxBytes を超えました)")
        }
        output.write(buffer, 0, read)
        received += read
        if (received - notified >= PROGRESS_NOTIFY_BYTES) {
            notified = received
            onProgress(received, total)
        }
    }
    onProgress(received, total)
}
