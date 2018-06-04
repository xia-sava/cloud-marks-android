package to.sava.cloudmarksandroid.libs

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.delete
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkType
import java.math.BigDecimal
import java.util.*


class Marks (private val context: Context) {

    private val settings: Settings by lazy {
        Settings(context)
    }

    private val storage: Storage by lazy {
        Storage.factory(settings)
    }

    private var remoteFile: FileInfo? = null
    private var remoteFileCreated: BigDecimal? = null

    fun load() {
        // ストレージの最新ファイルを取得
        fetchLatestRemoteFile()
        val remoteFile = this.remoteFile
        val remoteFileCreated = this.remoteFileCreated
        if (remoteFile == null || remoteFile.filename == "") {
            throw FileNotFoundException("ブックマークがまだ保存されていません")
        }
        val remote = storage.readMarks(remoteFile)

//         差分を取って適用
//        let bookmark = await this.getBookmarkRoot();
//        await this.applyRemote(remote, bookmark);
//
//         最終ロード日時保存
//        storage.settings.lastSynced = remoteFileCreated;
//        storage.settings.lastBookmarkModify = Date.now();
//        await storage.settings.save();




        try {
            storage.checkAccessibility()
        }
        catch (userAuthIoEx: UserRecoverableAuthIOException) {
            throw ServiceAuthenticationException(userAuthIoEx.message ?: "")
        }
    }


    private fun fetchLatestRemoteFile() {
        // ストレージのファイル一覧を取得して最新ファイルを取得
        if (remoteFile == null || remoteFileCreated == null) {
            val remoteFiles = storage.lsDir(settings.folderName)
                    .filter {
                        f -> Regex("""^bookmarks\.\d+\.json$""").containsMatchIn(f.filename)
                    }
                    .sortedBy { it.filename }
            if (!remoteFiles.isEmpty()) {
                remoteFile = remoteFiles.last()
                remoteFileCreated = Regex("""\d+""").find(remoteFile!!.filename)?.groupValues?.get(0)?.toBigDecimal()
            }
        }
    }



    private fun createMark(id: String = UUID.randomUUID().toString(),
                           type: MarkType = MarkType.Bookmark,
                           title: String = "",
                           url: String = "",
                           parent: MarkNode? = null): MarkNode {
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        val mark = realm.createObject<MarkNode>(id)
        mark.type = type
        mark.title = title
        mark.url = url
        mark.parent = parent
        realm.commitTransaction()
        return mark
    }

    fun setupFixture() {
        val f = MarkType.Folder
        val b = MarkType.Bookmark

        Realm.getDefaultInstance().executeTransaction {
            Realm.getDefaultInstance().delete<MarkNode>()
        }

        val root = createMark(id = "root", type = f, title = "root")
        val menu = createMark(type = f, title = "ブックマークメニュー", parent = root)
        createMark(type = f, title = "ブックマークツールバー", parent = root)
        createMark(type = f, title = "他のブックマーク", parent = root)
        createMark(type = f, title = "モバイルのブックマーク", parent = root)

        for (i in 1..3) {
            val parent1 = createMark(type = f, title = "フォルダ$i", parent = menu)
            for (j in 1..25) {
                when (j) {
                    in 1..3 -> {
                        val parent2 = createMark(type = f, title = "フォルダ$i-$j", parent = parent1)
                        for (k in 1..3) {
                            val parent3 = createMark(type = f, title = "フォルダ$i-$j-$k", parent = parent2)
                            for (m in 1..5) {
                                createMark(type = b, title = "ブックマーク $i-$j-$k-$m",
                                        url = "https://xia.sava.to/$i-$j-$k-$m", parent = parent3)
                            }
                        }
                    }
                    else -> {
                        createMark(type = b, title = "ブックマーク $i-$j",
                                url = "https://xia.sava.to/$i-$j", parent = parent1)
                    }
                }
            }
        }
    }
}