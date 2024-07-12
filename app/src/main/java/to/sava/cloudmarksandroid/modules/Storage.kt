package to.sava.cloudmarksandroid.modules

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


enum class Services {
    GoogleDrive
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


//class JsonContainer(val version: Int, val hash: String, val contents: Any)
class MarksJsonContainer(val version: Int, val hash: String, val contents: MarkTreeNode)


abstract class Storage<T: FileInfo<*>>(val settings: Settings) {
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
    // 将来は対応サービス増やしたい意思表示だけど今はこんだけ
    return when (settings.getCurrentService()) {
        Services.GoogleDrive -> GoogleDriveStorage(settings) as Storage<FileInfo<*>>
    }
}
