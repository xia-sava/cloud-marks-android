package to.sava.cloudmarksandroid.modules

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.ListBucketsRequest
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import aws.smithy.kotlin.runtime.content.toByteArray
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import to.sava.cloudmarksandroid.databases.models.MarkTreeNode
import java.nio.charset.Charset
import java.security.MessageDigest


class FileInfo(
    val filename: String,
) {
    val isEmpty: Boolean
        get() = filename == ""

    val timestamp: Long
        get() = Regex("""/bookmarks\.(\d+)\.json$""")
            .find(filename)?.groupValues?.get(1)?.toLong() ?: 0
}

@Serializable
class MarksJsonContainer(val version: Int, val hash: String, val contents: MarkTreeNode)

/**
 * cloud_marks形式のJSONは他のクライアントも書き込むため，知らないフィールドがあっても読み進める．
 */
private val json = Json {
    ignoreUnknownKeys = true
}

/**
 * contents をシリアライズしたJSONのSHA-256を求める．
 * cloud_marks形式の hash と突き合わせる値なので，シリアライズの出力が変わると既存データを読めなくなる．
 */
internal fun hashContents(contents: MarkTreeNode): String {
    val serialized = json.encodeToString(contents)
    return MessageDigest.getInstance("SHA-256").digest(serialized.toByteArray()).joinToString("") {
        String.format("%02x", it)
    }
}

/**
 * cloud_marks形式のJSONをMarksツリーとして読み込む．
 * version 1 では contents のハッシュが hash と一致することを確かめる．
 */
internal fun parseMarkFile(jsonStr: String): MarkTreeNode {
    val container: MarksJsonContainer
    try {
        container = json.decodeFromString<MarksJsonContainer>(jsonStr)
    } catch (jsonEx: SerializationException) {
        throw InvalidJsonException("読込みデータの形式が不正です")
    }
    when (container.version) {
        1 -> {
            if (container.hash != hashContents(container.contents)) {
                throw InvalidJsonException("読込みデータの整合性エラーです")
            }
        }
    }
    return container.contents
}

interface Storage {

    suspend fun checkAccessibility(): Boolean

    suspend fun listDir(): List<FileInfo>

    suspend fun read(fileInfo: FileInfo): String

    suspend fun readMarkFile(file: FileInfo): MarkTreeNode = parseMarkFile(read(file))
}

class AwsS3Storage(private val settings: Settings) : Storage {

    /**
     * S3の設定を読んでクライアントを組み立て，[block] へ渡す．
     * 設定は毎回読み直す．保持すると設定画面での変更が次の起動まで効かない．
     */
    private suspend fun <T> api(
        block: suspend (
            s3: S3Client,
            bucketName: String,
            folderName: String,
        ) -> T
    ): T {
        val awsS3AccessKeyId = settings.getAwsS3AccessKeyId()
        val awsS3SecretAccessKey = settings.getAwsS3SecretAccessKey()
        val awsS3Region = settings.getAwsS3Region()
        val awsS3BucketName = settings.getAwsS3BucketName()
        val awsS3FolderName = settings.getAwsS3FolderName()

        S3Client {
            region = awsS3Region
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = awsS3AccessKeyId
                secretAccessKey = awsS3SecretAccessKey
            }
        }.use { client ->
            return block(client, awsS3BucketName, awsS3FolderName)
        }
    }

    override suspend fun read(fileInfo: FileInfo): String {
        return api { s3, bucketName, folderName ->
            s3.getObject(GetObjectRequest {
                bucket = bucketName
                key = fileInfo.filename
            }) {
                it.body?.toByteArray()?.toString(Charset.defaultCharset())
                    ?: throw InvalidJsonException("ファイルの読込みに失敗しました")
            }
        }
    }

    private suspend fun S3Client.listFiles(bucketName: String, folderName: String): List<FileInfo> {
        val response = listObjectsV2(ListObjectsV2Request {
            bucket = bucketName
            prefix = "${folderName}/"
        })
        return (response.contents ?: listOf()).mapNotNull { obj ->
            obj.key?.let { FileInfo(it) }
        }
    }

    override suspend fun listDir(): List<FileInfo> {
        return api { s3, bucketName, folderName ->
            s3.listFiles(bucketName, folderName)
        }
    }

    /**
     * 読込みができる状態かを確かめる．
     * バケット一覧の取得はリージョンにもフォルダにも依らないため，指定フォルダに
     * ブックマークのJSONが見えるところまで確かめる．
     */
    override suspend fun checkAccessibility(): Boolean {
        return api { s3, bucketName, folderName ->
            val buckets = s3.listBuckets(ListBucketsRequest {})
            if (bucketName !in (buckets.buckets ?: listOf()).map { it.name }) {
                return@api false
            }
            s3.listFiles(bucketName, folderName).any { it.timestamp > 0 }
        }
    }
}
