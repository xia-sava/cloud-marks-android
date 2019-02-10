package to.sava.cloudmarksandroid.libs

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkTreeNode
import to.sava.cloudmarksandroid.models.MarkType
import to.sava.cloudmarksandroid.repositories.MarkNodeRepository
import to.sava.cloudmarksandroid.repositories.RealmHelper
import javax.inject.Inject


/**
 * ブックマークを色々するまとめクラス．
 * cloud_marks形式JSONを保持するリモートストレージと，Marksツリーと，
 * Realm DBの仲介をする．
 */
class Marks @Inject constructor(
        private val repos: MarkNodeRepository) {

    private val settings: Settings by lazy {
        Settings()
    }

    /**
     * 設定画面で指定されたリモートストレージを保持する．
     */
    private val storage: Storage by lazy {
        Storage.factory(settings)
    }

    /**
     * リモートJSONの一番新しそうなやつの情報．
     * 複数回呼ばれると取得に時間がかかるのでキャッシュする用プロパティ．
     */
    private var remoteFile: FileInfo? = null
    private var remoteFileCreated: Long? = null

    /**
     * 作業状況を外部へ伝えるためのリスナー
     */
    var progressListener: ((folder: String, percent: Int) -> Unit)? = null

    /**
     * 処理対象フォルダ数
     */
    private var folderCount = -1L
    /**
     * いま何個目のフォルダを処理しているか
     */
    private var currentFolderNum = 0L

    /**
     * リモートJSONからRealm DBを上書きで取り込む．
     */
    fun load() {
        // ストレージの最新ファイルを取得
        val (remoteFile, remoteFileCreated) = getLatestRemoteFile()
        if (remoteFile == null || remoteFileCreated == null) {
            throw FileNotFoundException("ブックマークがまだ保存されていません")
        }
        val remote = storage.readMarksContents(remoteFile)

        // 差分を取って適用
        RealmHelper.transaction {
            try {
                applyMarkTreeNodeToDB(remote, getMarkNodeRoot())
            } catch (userAuthIoEx: UserRecoverableAuthIOException) {
                throw ServiceAuthenticationException(userAuthIoEx.message ?: "DB反映で何かのエラーです")
            }
        }

        // 最終ロード日時保存
        settings.lastSynced = remoteFileCreated
        settings.lastBookmarkModify = System.currentTimeMillis()
    }

    /**
     * リモートJSONの一覧から最新っぽいファイル名を取得する．
     */
    private fun getLatestRemoteFile(): Pair<FileInfo?, Long?> {
        // ストレージのファイル一覧を取得して最新ファイルを取得
        // 複数回呼ばれると結果が変わらないのに時間がかかるのでプロパティにキャッシュ
        if (remoteFile == null || remoteFileCreated == null) {
            val remoteFiles = storage.lsDir(settings.folderName)
                    .filter {
                        f -> Regex("""^bookmarks\.\d+\.json$""").containsMatchIn(f.filename)
                    }
                    .sortedBy { it.filename }
            if (remoteFiles.isEmpty()) {
                throw FileNotFoundException("ブックマークがまだ保存されていません")
            }
            remoteFile = remoteFiles.last()
            remoteFileCreated = Regex("""\d+""")
                    .find(remoteFile!!.filename)?.groupValues?.get(0)?.toLong()
        }
        return Pair(remoteFile, remoteFileCreated)
    }

    /**
     * Realm DBからルートノードを取得する．
     * 取得できなかった場合（まだ一度も保存していないとか）は新規に作成．
     */
    private fun getMarkNodeRoot(): MarkNode {
        val root = getMark(MarkNode.ROOT_ID)
        if (root != null) {
            return root
        }
        return createBookmark(null, MarkType.Folder, markId = MarkNode.ROOT_ID)
    }

    /**
     * Realm DBから指定名のノードを取得する．
     */
    fun getMark(id: String): MarkNode? {
        return repos.getMarkNode(id)
    }

    /**
     * Realm DBから指定ノードの子ノードを取得する．
     */
    fun getMarkChildren(parent: MarkNode): List<MarkNode> {
        return repos.getMarkNodeChildren(parent)
    }

    /**
     * ルートから指定ノードへ辿り着くための名前のリストを取得する．
     * 要するに path みたいな．/root/fooFolder/barMark とかそういう．
     */
    fun getMarkPath(child: MarkNode): MutableList<MarkNode> {
        return child.parent?.let { parent ->
            val list = getMarkPath(parent)
            list.add(child)
            list
        } ?: mutableListOf(child)
    }

    /**
     * MarkTreeNodeをRealm DBへ反映する．
     */
    private fun applyMarkTreeNodeToDB(remote: MarkTreeNode, local: MarkNode): Boolean {
        if (remote.type == MarkType.Folder) {
            if (folderCount == -1L) {
                folderCount = remote.countChildren(MarkType.Folder)
            }
            currentFolderNum++
            progressListener?.invoke(remote.title, (100 * currentFolderNum / folderCount).toInt())
        }

        if (diffMarks(remote, local)) {
            // remote の url と title をブックマークに反映
            local.title = remote.title
            local.url = remote.url
            // フォルダの反映．いったん全消しして追加しなおす（乱暴）
            if (remote.type == MarkType.Folder) {
                val children = repos.getMarkNodeChildren(local)
                for (child in children) {
                    removeBookmark(child)
                }
                for (child in remote.children) {
                    createBookmark(local, child)
                }
            }
            return true
        }
        // ターゲットに差分がなさそうでも子階層は違うかもしれないので再帰チェックする
        if (remote.type == MarkType.Folder) {
            val children = repos.getMarkNodeChildren(local)
            if (!children.isEmpty()) {
                var rc = false
                for (i in children.indices) {
                    rc = applyMarkTreeNodeToDB(remote.children[i], children[i]) || rc
                }
                return rc
            }
        }
        return false
    }

    /**
     * MarkTreeとMarkNodeで差分があるかどうか判定する．
     * というほど賢くはない．名前かURLか，フォルダの場合は子ノードの数が違えば，差分ありと見なす．
     */
    private fun diffMarks(remote: MarkTreeNode, bookmark: MarkNode): Boolean {
        if (remote.title != bookmark.title) {
            return true
        }
        if (remote.url != bookmark.url) {
            return true
        }
        // children の比較は個数まで，中身の比較は他ループに任せる
        if (remote.type == MarkType.Folder) {
            val childrenCount = repos.getMarkNodeChildren(bookmark).count()
            if (remote.children.size != childrenCount) {
                return true
            }
        }
        return false
    }

    /**
     * Realm DBにノードを新規に作成する．
     */
    private fun createBookmark(parentMark: MarkNode?, mark: MarkTreeNode, markOrder: Int = 0,
                       markId: String = MarkNode.newKey()): MarkNode {

        return createBookmark(parentMark, mark.type, mark.title, mark.url, markOrder,
                markId, mark.children)
    }

    /**
     * Realm DBにノードを新規に作成する．
     */
    private fun createBookmark(parentMark: MarkNode?, markType: MarkType, markTitle: String = "",
                       markUrl: String = "", markOrder: Int = 0, markId: String = MarkNode.newKey(),
                       markChildren: List<MarkTreeNode> = listOf()): MarkNode {

        val added = repos.create(markId).apply {
            type = markType
            title = markTitle
            url = markUrl
            order = markOrder
            parent = parentMark
        }
        repos.save(added)
        for ((order, child) in markChildren.withIndex()) {
            createBookmark(added, child, order)
        }
        return added
    }

    /**
     * Realm DBからノードを削除する．
     */
    private fun removeBookmark(target: MarkNode) {
        if (target.type == MarkType.Folder) {
            for (child in repos.getMarkNodeChildren(target)) {
                removeBookmark(child)
            }
        }
        repos.remove(target)
    }
}
