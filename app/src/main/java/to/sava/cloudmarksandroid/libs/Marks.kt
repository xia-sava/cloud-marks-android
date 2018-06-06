package to.sava.cloudmarksandroid.libs

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkNodeJson
import to.sava.cloudmarksandroid.models.MarkType
import java.math.BigDecimal


class Marks (private val realm: Realm) {

    private val settings: Settings by lazy {
        Settings()
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
        val bookmark = getMarkRoot()
        val remote = storage.readMarks(remoteFile)

        // 差分を取って適用
        realm.executeTransaction {
            try {
                MarksManipulator(realm).applyRemote(remote, bookmark)
            }
            catch (userAuthIoEx: UserRecoverableAuthIOException) {
                throw ServiceAuthenticationException(userAuthIoEx.message ?: "")
            }
        }

//         最終ロード日時保存
//        storage.settings.lastSynced = remoteFileCreated;
//        storage.settings.lastBookmarkModify = Date.now();
//        await storage.settings.save();
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
                remoteFileCreated = Regex("""\d+""")
                        .find(remoteFile!!.filename)?.groupValues?.get(0)?.toBigDecimal()
            }
        }
    }

    private fun getMarkRoot(): MarkNode {
        val root = getMark(MarkNode.ROOT_ID)
        if (root != null) {
            return root
        }
        Realm.getDefaultInstance().use {realm ->
            realm.beginTransaction()
            val newRoot = realm.createObject<MarkNode>(MarkNode.ROOT_ID)
            newRoot.title = ""
            newRoot.type = MarkType.Folder
            realm.commitTransaction()
            return newRoot
        }
    }

    fun getMark(id: String): MarkNode? {
        return realm.where<MarkNode>().equalTo("id", id).findFirst()
    }

    fun getMarkChildren(parent: MarkNode): RealmResults<MarkNode> {
        return MarksManipulator(realm).getMarkChildren(parent)
    }

//
//
//    private fun createMark(id: String = UUID.randomUUID().toString(),
//                           type: MarkType = MarkType.Bookmark,
//                           title: String = "",
//                           url: String = "",
//                           parent: MarkNode? = null): MarkNode {
//        val realm = Realm.getDefaultInstance()
//        realm.beginTransaction()
//        val mark = realm.createObject<MarkNode>(id)
//        mark.type = type
//        mark.title = title
//        mark.url = url
//        mark.parent = parent
//        realm.commitTransaction()
//        return mark
//    }
//
//    fun setupFixture() {
//        val f = MarkType.Folder
//        val b = MarkType.Bookmark
//
//        Realm.getDefaultInstance().executeTransaction {
//            Realm.getDefaultInstance().delete<MarkNode>()
//        }
//
//        val root = createMark(id = "root", type = f, title = "root")
//        val menu = createMark(type = f, title = "ブックマークメニュー", parent = root)
//        createMark(type = f, title = "ブックマークツールバー", parent = root)
//        createMark(type = f, title = "他のブックマーク", parent = root)
//        createMark(type = f, title = "モバイルのブックマーク", parent = root)
//
//        for (i in 1..3) {
//            val parent1 = createMark(type = f, title = "フォルダ$i", parent = menu)
//            for (j in 1..25) {
//                when (j) {
//                    in 1..3 -> {
//                        val parent2 = createMark(type = f, title = "フォルダ$i-$j", parent = parent1)
//                        for (k in 1..3) {
//                            val parent3 = createMark(type = f, title = "フォルダ$i-$j-$k", parent = parent2)
//                            for (m in 1..5) {
//                                createMark(type = b, title = "ブックマーク $i-$j-$k-$m",
//                                        url = "https://xia.sava.to/$i-$j-$k-$m", parent = parent3)
//                            }
//                        }
//                    }
//                    else -> {
//                        createMark(type = b, title = "ブックマーク $i-$j",
//                                url = "https://xia.sava.to/$i-$j", parent = parent1)
//                    }
//                }
//            }
//        }
//    }
}

class MarksManipulator(private val realm: Realm) {

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

    private fun createBookmark(parent: MarkNode, mark: MarkNodeJson, index: Int? = null): MarkNode {
        val added = realm.createObject<MarkNode>(MarkNode.newKey()).apply {
            type = mark.type
            title = mark.title
            url = mark.url
            this.parent = parent
        }
        for (child in mark.children) {
            createBookmark(added, child)
        }
        return added
    }

    private fun removeBookmark(target: MarkNode) {
        if (target.id == MarkNode.ROOT_ID || target.parent == null || target.parent?.id == MarkNode.ROOT_ID) {
            for (child in getMarkChildren(target)) {
                removeBookmark(child)
            }
        } else {
            fun remove (mark: MarkNode) {
                for (child in getMarkChildren(mark)) {
                    remove(child)
                }
                remove(mark)
            }
            remove(target)
        }
    }

    private fun updateBookmark(target: MarkNode, modify: MarkNodeJson) {
        if (target.id == MarkNode.ROOT_ID || target.parent == null || target.parent?.id == MarkNode.ROOT_ID) {
            return
        }
        realm.executeTransaction {
            target.title = modify.title
            target.url = modify.url
        }
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

    fun getMarkChildren(target: MarkNode): RealmResults<MarkNode> {
        return realm.where<MarkNode>().equalTo("parent.id", target.id).findAll()
    }
}