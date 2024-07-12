package to.sava.cloudmarksandroid.modules

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.ListBucketsRequest
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import aws.smithy.kotlin.runtime.content.toByteArray
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.common.io.ByteSource
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import to.sava.cloudmarksandroid.databases.models.MarkTreeNode
import to.sava.cloudmarksandroid.databases.models.MarkType
import java.nio.charset.Charset
import java.security.MessageDigest
import aws.sdk.kotlin.services.s3.model.Object as S3Object


enum class Services {
    GoogleDrive,
    AwsS3,
}

abstract class FileInfo<T>(
    val filename: String,
) {
    abstract val fileObject: T

    val isEmpty: Boolean
        get() = filename == ""

    val timestamp: Long
        get() = Regex("""^bookmarks\.(\d+)\.json$""")
            .find(filename)?.groupValues?.get(1)?.toLong() ?: 0
}

class MarksJsonContainer(val version: Int, val hash: String, val contents: MarkTreeNode)

abstract class Storage<T : FileInfo<*>>(
    val settings: Settings
) {
    open val gson: Gson by lazy {
        GsonBuilder()
            .disableHtmlEscaping()
            .registerTypeAdapter(MarkType::class.java, JsonDeserializer { json, _, _ ->
                MarkType.entries.first { it.rawValue == json.asInt }
            })
            .create()
    }

    abstract suspend fun checkAccessibility(): Boolean
    abstract suspend fun ls(): List<T>
    abstract suspend fun read(fileInfo: T): String

    suspend fun listDir(): List<T> {
        return ls()
    }

    /**
     * JSONをMarksツリーとして読み込む．
     *
     * readContents()みたく汎用の読み込みルーチンにしたかったけど，
     * gsonの入出力を共に外から与えるのが困難すぎて断念．
     */
    suspend fun readMarkFile(file: T): MarkTreeNode {
        val jsonStr = read(file)
        val container: MarksJsonContainer
        try {
            container = gson.fromJson(jsonStr, MarksJsonContainer::class.java)
        } catch (jsonEx: JsonParseException) {
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

    private fun hashContents(contents: Any): String {
        val json = gson.toJson(contents).trim()
        return MessageDigest.getInstance("SHA-256").digest(json.toByteArray()).joinToString("") {
            String.format("%02x", it)
        }
    }
}

class AwsS3FileInfo(
    filename: String,
    override val fileObject: S3Object,
) : FileInfo<S3Object>(filename)

class AwsS3Storage(settings: Settings) : Storage<AwsS3FileInfo>(settings) {
    private var _awsS3AccessKeyId: String? = null
    private suspend fun getAwsS3AccessKeyId(): String {
        return _awsS3AccessKeyId
            ?: settings.getAwsS3AccessKeyId().also { _awsS3AccessKeyId = it }
    }

    private var _awsS3SecretAccessKey: String? = null
    private suspend fun getAwsS3SecretAccessKey(): String {
        return _awsS3SecretAccessKey
            ?: settings.getAwsS3SecretAccessKey().also { _awsS3SecretAccessKey = it }
    }

    private var _awsS3Region: String? = null
    private suspend fun getAwsS3Region(): String {
        return _awsS3Region
            ?: settings.getAwsS3Region().also { _awsS3Region = it }
    }

    private var _awsS3BucketName: String? = null
    private suspend fun getAwsS3BucketName(): String {
        return _awsS3BucketName
            ?: settings.getAwsS3BucketName().also { _awsS3BucketName = it }
    }

    private var _awsS3FolderName: String? = null
    private suspend fun getAwsS3FolderName(): String {
        return _awsS3FolderName
            ?: settings.getAwsS3FolderName().also { _awsS3FolderName = it }
    }

    private suspend fun <T> api(
        block: suspend (
            s3: S3Client,
            bucketName: String,
            folderName: String,
        ) -> T
    ): T {
        val awsS3AccessKeyId = getAwsS3AccessKeyId()
        val awsS3SecretAccessKey = getAwsS3SecretAccessKey()
        val awsS3Region = getAwsS3Region()
        val awsS3BucketName = getAwsS3BucketName()
        val awsS3FolderName = getAwsS3FolderName()

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

    override suspend fun read(fileInfo: AwsS3FileInfo): String {
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

    override suspend fun ls(): List<AwsS3FileInfo> {
        return api { s3, bucketName, folderName ->
            val response = s3.listObjectsV2(ListObjectsV2Request {
                bucket = bucketName
                prefix = "${folderName}/"
            })
            (response.contents ?: listOf()).mapNotNull { obj ->
                obj.key?.let { AwsS3FileInfo(it, obj) }
            }
        }
    }

    override suspend fun checkAccessibility(): Boolean {
        return api { s3, bucketName, folderName ->
            val response = s3.listBuckets(ListBucketsRequest {})
            getAwsS3BucketName() in (response.buckets ?: listOf()).map { it.name }
        }
    }
}

class GoogleDriveFileInfo(
    filename: String,
    override val fileObject: File,
) : FileInfo<File>(filename)

class GoogleDriveStorage(settings: Settings) : Storage<GoogleDriveFileInfo>(settings) {
    private var _credential: GoogleAccountCredential? = null
    private suspend fun credential(): GoogleAccountCredential {
        val googleAccount = settings.getGoogleAccount()
        if (_credential?.selectedAccountName != googleAccount) {
            GoogleAccountCredential.usingOAuth2(settings.context, SCOPES).also { cred ->
                cred.backOff = ExponentialBackOff()
                settings.getGoogleAccount()
                    .takeIf { it != "" }
                    .let {
                        try {
                            cred.selectedAccountName = it
                        } catch (ex: IllegalArgumentException) {
                            // settings で登録されてるアカウントがエラーとかまぁ普通は起きない
//                            FirebaseCrashlytics.getInstance().recordException(ex)
                            throw ex
                        }
                    }
                _credential = cred
            }
        }
        return _credential!!
    }

    private var _api: Drive? = null
    private suspend fun api(): Drive {
        return _api ?: run {
            Drive.Builder(NetHttpTransport(), GsonFactory(), credential()).build().also {
                _api = it
            }
        }
    }

    override suspend fun read(fileInfo: GoogleDriveFileInfo): String {
        val response = withContext(Dispatchers.IO) {
            api().files()
                .get(fileInfo.fileObject.id)
                .executeMedia()
        }
        return object : ByteSource() {
            override fun openStream() = response.content
        }.asCharSource(Charset.defaultCharset()).read()
    }

    override suspend fun ls(): List<GoogleDriveFileInfo> {
        val response = withContext(Dispatchers.IO) {
            api()
                .files().list()
                .setQ(
                    listOf(
                        "'${getFolderId()}' in parents",
                        "trashed = false"
                    ).joinToString(" and ")
                )
                .execute()
        }
        return response.files.map { file -> GoogleDriveFileInfo(file.name, file) }
    }

    private suspend fun getFolderId(): String? {
        val response = withContext(Dispatchers.IO) {
            api()
                .files().list()
                .setQ(
                    listOf(
                        "name = '${settings.getGoogleDriveFolderName()}'",
                        "'root' in parents",
                        "trashed = false"
                    ).joinToString(" and ")
                )
                .execute()
        }
        return response.files.firstOrNull()?.id
    }

    /**
     * 試しに今の認証情報でアクセスしてみる．
     * エラーの Exception は全部素通し．
     */
    override suspend fun checkAccessibility(): Boolean {
        withContext(Dispatchers.IO) {
            api().files().get("root").execute()
        }
        return true
    }

    companion object {
        val SCOPES: List<String> = listOf(DriveScopes.DRIVE)
    }
}


suspend fun storageFactory(settings: Settings): Storage<FileInfo<*>> {
    val currentService = Services.entries[settings.getCurrentService()]
    @Suppress("UNCHECKED_CAST")
    return when (currentService) {
        Services.GoogleDrive -> GoogleDriveStorage(settings)
        Services.AwsS3 -> AwsS3Storage(settings)
    } as Storage<FileInfo<*>>
}
