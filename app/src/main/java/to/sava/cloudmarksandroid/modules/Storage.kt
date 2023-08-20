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
    Gdrive
}


class FileInfo(var filename: String, var fileObject: Map<String, String> = mutableMapOf()) {
    val isEmpty: Boolean
        get() = filename == ""
}


//class JsonContainer(val version: Int, val hash: String, val contents: Any)
class MarksJsonContainer(val version: Int, val hash: String, val contents: MarkTreeNode)


abstract class Storage(val settings: Settings) {
    companion object {
        suspend fun factory(settings: Settings): Storage {
            // 将来は対応サービス増やしたい意思表示だけど今はこんだけ
            return when (settings.getCurrentService()) {
                Services.Gdrive -> GoogleDriveStorage(settings)
            }
        }
    }

    open val gson: Gson by lazy {
        GsonBuilder()
            .disableHtmlEscaping()
            .registerTypeAdapter(MarkType::class.java, JsonDeserializer { json, _, _ ->
                MarkType.values().first { it.rawValue == json.asInt }
            })
            .create()
    }

    abstract suspend fun checkAccessibility(): Boolean
    abstract suspend fun lsDir(dirName: String, parent: FileInfo): List<FileInfo>
    abstract suspend fun lsDir(dirName: String, parentName: String): List<FileInfo>
    abstract suspend fun lsDir(dirName: String): List<FileInfo>
    abstract suspend fun lsFile(filename: String, parent: FileInfo): FileInfo
    abstract suspend fun lsFile(filename: String, parentName: String): FileInfo
    abstract suspend fun lsFile(filename: String): FileInfo
    abstract suspend fun read(fileInfo: FileInfo): String

    /**
     * JSONをMarksツリーとして読み込む．
     *
     * readContents()みたく汎用の読み込みルーチンにしたかったけど，
     * gsonの入出力を共に外から与えるのが困難すぎて断念．
     */
    suspend fun readMarksContents(file: FileInfo): MarkTreeNode {
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


@Suppress("BlockingMethodInNonBlockingContext")
class GoogleDriveStorage(settings: Settings) : Storage(settings) {

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

    override suspend fun lsFile(filename: String, parent: FileInfo): FileInfo {
        val response = withContext(Dispatchers.IO) {
            api()
                .files().list()
                .setQ(
                    listOf(
                        "name = '$filename'",
                        "'${parent.fileObject["id"]}' in parents",
                        "trashed = false"
                    ).joinToString(" and ")
                )
                .execute()
        }
        if (response.files.size == 0) {
            return FileInfo("", mutableMapOf())
        }
        return mapFileInfo(response.files[0])
    }

    override suspend fun lsFile(filename: String, parentName: String): FileInfo {
        return lsFile(filename, findDirectory(parentName))
    }

    override suspend fun lsFile(filename: String): FileInfo {
        return lsFile(filename, FileInfo("root", mutableMapOf("id" to "root")))
    }

    override suspend fun lsDir(dirName: String, parent: FileInfo): List<FileInfo> {
        val dirInfo = lsFile(dirName, parent)
        if (dirInfo.isEmpty) {
            return listOf()
        }
        val response = withContext(Dispatchers.IO) {
            api()
                .files().list()
                .setQ(
                    listOf(
                        "'${dirInfo.fileObject["id"]}' in parents",
                        "trashed = false"
                    ).joinToString(" and ")
                )
                .execute()
        }
        return response.files.map { file -> mapFileInfo(file) }
    }

    override suspend fun lsDir(dirName: String, parentName: String): List<FileInfo> {
        return lsDir(dirName, findDirectory(parentName))
    }

    override suspend fun lsDir(dirName: String): List<FileInfo> {
        return lsDir(dirName, FileInfo("root", mutableMapOf("id" to "root")))
    }


    override suspend fun read(fileInfo: FileInfo): String {
        val response = withContext(Dispatchers.IO) {
            api().files()
                .get(fileInfo.fileObject["id"])
                .executeMedia()
        }
        return object : ByteSource() {
            override fun openStream() = response.content
        }.asCharSource(Charset.defaultCharset()).read()
    }


    private suspend fun findDirectory(dirName: String): FileInfo {
        val dirInfo = lsFile(dirName)
        if (dirInfo.isEmpty) {
            throw DirectoryNotFoundException("ディレクトリ $dirName が見つかりません")
        }
        return dirInfo
    }


    private fun mapFileInfo(file: File): FileInfo {
        val map = mutableMapOf<String, String>()
        for ((key, value) in file) {
            if (value is String) {
                map[key] = value
            }
        }
        return FileInfo(file.name, map)
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
