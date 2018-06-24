package to.sava.cloudmarksandroid.libs

import android.accounts.Account
import com.crashlytics.android.Crashlytics
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.gson.*
import org.apache.commons.io.IOUtils
import to.sava.cloudmarksandroid.models.MarkNodeJson
import to.sava.cloudmarksandroid.models.MarkType
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*


enum class Services(rawvalue: Int) {
    Gdrive(0)
}


class FileInfo(var filename: String, var fileObject: Map<String, String> = mutableMapOf()) {
    val isEmpty: Boolean
        get() = filename == ""
}


//class JsonContainer(val version: String, val hash: String, val contents: Any)
class MarksJsonContainer(val version: String, val hash: String, val contents: MarkNodeJson)


abstract class Storage(val settings: Settings) {
    companion object {
        fun factory(settings: Settings): Storage {
            // 将来は対応サービス増やしたい意思表示だけど今はこんだけ
            return when (settings.currentService) {
                Services.Gdrive -> GoogleDriveStorage(settings)
            }
        }
    }

    open val gson: Gson by lazy {
        GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeAdapter(MarkType::class.java, JsonDeserializer<MarkType> { json, _, _->
                    MarkType.values().first { it.rawValue == json.asInt }
                })
                .registerTypeAdapter(MarkNodeJson::class.java, JsonSerializer<MarkNodeJson> { src, typeOfSrc, context ->
                    MarkNodeJson.jsonSerialize(src, typeOfSrc, context)
                })
                .create()
    }

    abstract fun checkAccessibility(): Boolean
    abstract fun lsDir(dirName: String, parent: FileInfo): List<FileInfo>
    abstract fun lsDir(dirName: String, parentName: String): List<FileInfo>
    abstract fun lsDir(dirName: String): List<FileInfo>
    abstract fun lsFile(filename: String, parent: FileInfo): FileInfo
    abstract fun lsFile(filename: String, parentName: String): FileInfo
    abstract fun lsFile(filename: String): FileInfo
    abstract fun read(fileInfo: FileInfo): String

    fun readMarks(file: FileInfo): MarkNodeJson {
        val jsonStr = read(file)
        try {
            // コール側から Container を指定できるようにしたかったけどやり方全然ワカンネ
            val container = gson.fromJson(jsonStr, MarksJsonContainer::class.java)
            if (container.hash != hashContents(container.contents)) {
                throw InvalidJsonException("読込みデータの整合性エラーです")
            }
            return container.contents
        }
        catch (jsonEx: JsonParseException) {
            throw InvalidJsonException("読込みデータの形式が不正です")
        }
    }

    private fun hashContents(contents: Any): String {
        val json = gson.toJson(contents).trim()
        return MessageDigest.getInstance("SHA-256").digest(json.toByteArray()).joinToString("") {
            String.format("%02x", it)
        }
    }
}


class GoogleDriveStorage(settings: Settings): Storage(settings) {
    val credential: GoogleAccountCredential by lazy {
        val cred = GoogleAccountCredential.usingOAuth2(settings.context, SCOPES)
        if (settings.googleAccount != "") {
            try {
                cred.selectedAccount = Account(settings.googleAccount, "com.google")
            }
            catch (ex: IllegalArgumentException) {
                // settings で登録されてるアカウントがエラーとかまぁ普通は起きない
                Crashlytics.logException(ex)
            }
        }
        cred
    }

    private val api: Drive by lazy {
        Drive.Builder(AndroidHttp.newCompatibleTransport(), GsonFactory(), credential).build()
    }

    companion object {
        val SCOPES: List<String> = Arrays.asList("https://www.googleapis.com/auth/drive")
        val SCOPES_STR: String
            get() = "oauth2 ${SCOPES.joinToString(" ")}"
    }

    override fun lsFile(filename: String, parent: FileInfo): FileInfo {
        val response = api
                .files().list()
                .setQ(listOf(
                        "name = '$filename'",
                        "'${parent.fileObject["id"]}' in parents",
                        "trashed = false"
                ).joinToString(" and "))
                .execute()
        if (response.files.size == 0) {
            return FileInfo("", mutableMapOf())
        }
        return mapFileInfo(response.files[0])
    }

    override fun lsFile(filename: String, parentName: String): FileInfo {
        return lsFile(filename, findDirectory(parentName))
    }

    override fun lsFile(filename: String): FileInfo {
        return lsFile(filename, FileInfo("root", mutableMapOf("id" to "root")))
    }

    override fun lsDir(dirName: String, parent: FileInfo): List<FileInfo> {
        val dirInfo = lsFile(dirName, parent)
        if (dirInfo.isEmpty) {
            return listOf()
        }
        val response = api
                .files().list()
                .setQ(listOf(
                        "'${dirInfo.fileObject["id"]}' in parents",
                        "trashed = false"
                ).joinToString(" and "))
                .execute()
        return response.files.map {file -> mapFileInfo(file)}
    }

    override fun lsDir(dirName: String, parentName: String): List<FileInfo> {
        return lsDir(dirName, findDirectory(parentName))
    }

    override fun lsDir(dirName: String): List<FileInfo> {
        return lsDir(dirName, FileInfo("root", mutableMapOf("id" to "root")))
    }


    override fun read(fileInfo: FileInfo): String {
        val response = api.files()
                .get(fileInfo.fileObject["id"])
                .executeMedia()
        return IOUtils.toString(response.content, Charset.defaultCharset())
    }



    private fun findDirectory(dirName: String): FileInfo {
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
    override fun checkAccessibility(): Boolean {
        api.files().get("root").execute()
        return true
    }


}