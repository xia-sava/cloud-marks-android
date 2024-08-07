package to.sava.cloudmarksandroid.modules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import to.sava.cloudmarksandroid.databases.models.Favicon
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.databases.models.MarkTreeNode
import to.sava.cloudmarksandroid.databases.models.MarkType
import to.sava.cloudmarksandroid.databases.repositories.FaviconRepository
import to.sava.cloudmarksandroid.databases.repositories.MarkNodeRepository
import java.io.IOException


/**
 * ブックマークを色々するまとめクラス．
 * cloud_marks形式JSONを保持するリモートストレージと，Marksツリーと，
 * Room DBの仲介をする．
 */
class Marks(
    private val settings: Settings,
    private val repos: MarkNodeRepository,
    private val faviconRepos: FaviconRepository,
) {

    /**
     * 設定画面で指定されたリモートストレージを保持する．
     */
    private var _storage: Storage<FileInfo<*>>? = null
    private suspend fun storage(): Storage<FileInfo<*>> {
        return _storage ?: run {
            storageFactory(settings).also {
                _storage = it
            }
        }
    }

    /**
     * リモートJSONの一番新しそうなやつの情報．
     * 複数回呼ばれると取得に時間がかかるのでキャッシュする用プロパティ．
     */
    private var _remoteFile: FileInfo<*>? = null
    private var _remoteFileCreated: Long? = null

    /**
     * 作業状況を外部へ伝えるためのリスナー
     */
    var progressListener: (suspend (folder: String, percent: Int) -> Unit)? = null

    /**
     * 処理対象フォルダ数
     */
    private var folderCount = -1L

    /**
     * いま何個目のフォルダを処理しているか
     */
    private var currentFolderNum = 0L

    /**
     * リモートJSONからRoom DBを上書きで取り込む．
     */
    suspend fun load() {
        // ストレージの最新ファイルを取得
        val (remoteFile, remoteFileCreated) = getLatestRemoteFile()
        val remote = storage().readMarkFile(remoteFile)

        // 差分を取って適用
        try {
            applyMarkTreeNodeToDB(remote, getRootMarkNode())
        } catch (exception: IOException) {
            throw ServiceAuthenticationException(exception.message ?: "DB反映で何かのエラーです")
        }

        // 最終ロード日時保存
        settings.setLastSynced(remoteFileCreated)
        settings.setLastBookmarkModified(System.currentTimeMillis())
    }

    /**
     * リモートJSONの一覧から最新っぽいファイル名を取得する．
     */
    private suspend fun getLatestRemoteFile(): Pair<FileInfo<*>, Long> {
        // ストレージのファイル一覧を取得して最新ファイルを取得
        // 複数回呼ばれると結果が変わらないのに時間がかかるのでプロパティにキャッシュ
        _remoteFile?.let { rf ->
            _remoteFileCreated?.let { ts ->
                return Pair(rf, ts)
            }
        }
        val remoteFileInfo = storage().listDir()
            .maxByOrNull { it.timestamp }
        if (remoteFileInfo == null) {
            throw FileNotFoundException("ブックマークがまだ保存されていません")
        }
        _remoteFile = remoteFileInfo
        _remoteFileCreated = remoteFileInfo.timestamp
        return Pair(remoteFileInfo, remoteFileInfo.timestamp)
    }

    /**
     * Room DBから指定名のノードを取得する．
     */
    suspend fun initializeDb() {
        if (repos.getRootMarkNode() == null) {
            val root = createBookmark(null, MarkType.Folder, "root")
            createBookmark(root, MarkType.Folder, "デフォルトブックマークフォルダ")
        }
    }

    /**
     * Room DBからルートノードを取得する．
     * 取得できなかった場合（まだ一度も保存していないとか）は新規に作成．
     */
    private suspend fun getRootMarkNode(): MarkNode {
        return repos.getRootMarkNode()
            ?: createBookmark(null, MarkType.Folder, "root")
    }

    /**
     * Room DBから指定名のノードを取得する．
     */
    suspend fun getMark(id: Long): MarkNode? {
        return repos.getMarkNode(id)
    }

    /**
     * Room DBから指定ノードの子ノードを取得する．
     */
    suspend fun getMarkChildren(parent: MarkNode): List<MarkNode> {
        return repos.getMarkNodeChildren(parent)
    }

    /**
     * ルートから指定ノードへ辿り着くための名前のリストを取得する．
     * 要するに path みたいな．/root/fooFolder/barMark とかそういう．
     */
    suspend fun getMarkPath(child: MarkNode): List<MarkNode> {
        return child.parentId?.let { parentId ->
            repos.getMarkNode(parentId)?.let { parent ->
                getMarkPath(parent).toMutableList().apply {
                    add(child)
                }
            }
        } ?: listOf(child)
    }

    /**
     * MarkTreeNodeをRoom DBへ反映する．
     */
    private suspend fun applyMarkTreeNodeToDB(remote: MarkTreeNode, local: MarkNode): Boolean {
        if (remote.isFolder) {
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
            repos.saveMarkNode(local)
            // フォルダの反映．いったん全消しして追加しなおす（乱暴）
            if (remote.isFolder) {
                val children = repos.getMarkNodeChildren(local)
                for (child in children) {
                    removeBookmark(child)
                }
                for ((order, child) in remote.children.withIndex()) {
                    createBookmark(local, child, order)
                }
            }
            return true
        }
        // ターゲットに差分がなさそうでも子階層は違うかもしれないので再帰チェックする
        if (remote.isFolder) {
            val children = repos.getMarkNodeChildren(local)
            if (children.isNotEmpty()) {
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
    private suspend fun diffMarks(remote: MarkTreeNode, bookmark: MarkNode): Boolean {
        if (remote.title != bookmark.title) {
            return true
        }
        if (remote.url != bookmark.url) {
            return true
        }
        // children の比較は個数まで，中身の比較は他ループに任せる
        if (remote.isFolder) {
            val childrenCount = repos.getMarkNodeChildren(bookmark).count()
            if (remote.children.size != childrenCount) {
                return true
            }
        }
        return false
    }

    /**
     * Room DBにノードを新規に作成する．
     */
    private suspend fun createBookmark(
        parent: MarkNode?,
        mark: MarkTreeNode,
        order: Int
    ): MarkNode {
        return createBookmark(parent, mark.type, mark.title, mark.url, order, mark.children)
    }

    /**
     * Room DBにノードを新規に作成する．
     */
    private suspend fun createBookmark(
        parent: MarkNode?, type: MarkType, title: String = "",
        url: String = "", order: Int = 0,
        children: List<MarkTreeNode> = listOf()
    ): MarkNode {

        val mark = repos.createMarkNode(type, title, url, order, parent?.id)
        for ((ord, child) in children.withIndex()) {
            createBookmark(mark, child, ord)
        }
        return mark
    }

    /**
     * Room DBからノードを削除する．
     */
    private suspend fun removeBookmark(target: MarkNode) {
        if (target.isFolder) {
            for (child in repos.getMarkNodeChildren(target)) {
                removeBookmark(child)
            }
        }
        repos.deleteMarkNode(target)
    }

    /**
     * favicon を Google から HTTP で取得する
     */
    suspend fun fetchFavicons(
        domains: List<String>,
        onFetched: (favicons: List<Favicon>) -> Unit = {},
    ) = coroutineScope {
        domains
            .map { domain ->
                async(Dispatchers.Default) {
                    faviconRepos.fetchFavicon(domain)
                }
            }
            .awaitAll()
            .filterNotNull()
            .let {
                faviconRepos.saveFavicons(it)
                onFetched(it)
            }
    }

    suspend fun fetchAllFavicons() {
        repos.getAllMarkNode()
            .filter { it.isBookmark }
            .map { it.domain }
            .distinct()
            .minus(faviconRepos.findAllFavicons().map { it.domain }.toSet())
            .let {
                fetchFavicons(it)
            }
    }

    suspend fun findFavicons(domains: List<String>): List<Favicon> {
        return faviconRepos.findFavicons(domains)
    }
}
