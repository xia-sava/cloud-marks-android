package to.sava.cloudmarksandroid.libs

import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkNodeJson
import to.sava.cloudmarksandroid.models.MarkType


class Marks (private val realm: Realm) {

    private val settings: Settings by lazy {
        Settings()
    }

    private val storage: Storage by lazy {
        Storage.factory(settings)
    }

    private var remoteFile: FileInfo? = null
    private var remoteFileCreated: Long? = null

    fun load() {
        // ストレージの最新ファイルを取得
        val (remoteFile, remoteFileCreated) = getLatestRemoteFile()
        if (remoteFile.isEmpty) {
            throw FileNotFoundException("ブックマークがまだ保存されていません")
        }
        val bookmark = getMarkRoot()
        val remote = storage.readMarks(remoteFile)

        // 差分を取って適用
        MarksManipulator(realm).use {
            try {
                it.applyRemote(remote, bookmark)
            } catch (userAuthIoEx: UserRecoverableAuthIOException) {
                throw ServiceAuthenticationException(userAuthIoEx.message ?: "")
            }
        }

        // 最終ロード日時保存
        settings.lastSynced = remoteFileCreated
        settings.lastBookmarkModify = System.currentTimeMillis()
    }


    private fun getLatestRemoteFile(): Pair<FileInfo, Long> {
        // ストレージのファイル一覧を取得して最新ファイルを取得
        // 複数回呼ばれると結果が変わらないのに時間がかかるのでプロパティにキャッシュ
        if (remoteFile == null || remoteFileCreated == null) {
            val remoteFiles = storage.lsDir(settings.folderName)
                    .filter {
                        f -> Regex("""^bookmarks\.\d+\.json$""").containsMatchIn(f.filename)
                    }
                    .sortedBy { it.filename }
            if (!remoteFiles.isEmpty()) {
                remoteFile = remoteFiles.last()
                remoteFileCreated = Regex("""\d+""")
                        .find(remoteFile!!.filename)?.groupValues?.get(0)?.toLong()
            }
        }
        return Pair(remoteFile ?: FileInfo(""), remoteFileCreated ?: 0L)
    }

    private fun getMarkRoot(): MarkNode {
        val root = getMark(MarkNode.ROOT_ID)
        if (root != null) {
            return root
        }
        Realm.getDefaultInstance().use {realm ->
            realm.beginTransaction()
            val newRoot = realm.createObject<MarkNode>(MarkNode.ROOT_ID)
            newRoot.type = MarkType.Folder
            realm.commitTransaction()
            return newRoot
        }
    }

    fun getMark(id: String): MarkNode? {
        return realm.where<MarkNode>().equalTo("id", id).findFirst()
    }

    fun getMarkChildren(parent: MarkNode): RealmResults<MarkNode> {
        return realm
                .where<MarkNode>()
                .equalTo("parent.id", parent.id)
                .sort("order")
                .findAll()
    }
}

class MarksManipulator(private val realm: Realm): AutoCloseable {

    init {
        realm.beginTransaction()
    }

    override fun close() {
        realm.commitTransaction()
    }

    fun applyRemote(remote: MarkNodeJson, bookmark: MarkNode): Boolean {
        if (diffMark(remote, bookmark)) {
            updateBookmark(bookmark, remote)
            if (remote.type == MarkType.Folder) {
                val children = getMarkChildren(bookmark)
                for (child  in children) {
                    removeBookmark(child)
                }
                for (child in remote.children) {
                    createBookmark(bookmark, child)
                }
            }
            return true
        }
        val children = getMarkChildren(bookmark)
        if (remote.type == MarkType.Folder && ! children.isEmpty()) {
            var rc = false
            for (i in children.indices) {
                rc = applyRemote(remote.children[i], children[i]!!) || rc
            }
            return rc
        }
        return false
    }

    private fun createBookmark(parent: MarkNode, mark: MarkNodeJson, markOrder: Int = 0): MarkNode {
        Log.d("cma:createBookmark", "${parent.title}[$markOrder]/${mark.title}")
        val added = realm.createObject<MarkNode>(MarkNode.newKey()).apply {
            type = mark.type
            title = mark.title
            url = mark.url
            order = markOrder
            this.parent = parent
        }
        for ((order, child) in mark.children.withIndex()) {
            createBookmark(added, child, order)
        }
        return added
    }

    private fun removeBookmark(target: MarkNode) {
        Log.d("cma:removeBookmark", target.title)
        if (target.type == MarkType.Folder) {
            for (child in getMarkChildren(target)) {
                removeBookmark(child)
            }
        }
        target.deleteFromRealm()
    }

    private fun updateBookmark(target: MarkNode, modify: MarkNodeJson) {
        Log.d("cma:updateBookmark", "${target.title} = ${modify.title}")
        target.title = modify.title
        target.url = modify.url
    }

    private fun diffMark(remote: MarkNodeJson, bookmark: MarkNode): Boolean {
        if (remote.title != bookmark.title) {
            return true
        }
        if (remote.url != bookmark.url) {
            return true
        }
        // children の比較は個数まで，中身の比較は他ループに任せる
        if (remote.type == MarkType.Folder) {
            val children = getMarkChildren(bookmark)
            if (children.isEmpty() || remote.children.size != children.size) {
                return true
            }
        }
        return false
    }

    private fun getMarkChildren(parent: MarkNode): RealmResults<MarkNode> {
        return realm
                .where<MarkNode>()
                .equalTo("parent.id", parent.id)
                .sort("order")
                .findAll()
    }
}