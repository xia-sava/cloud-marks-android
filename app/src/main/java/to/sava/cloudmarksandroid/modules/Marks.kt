package to.sava.cloudmarksandroid.modules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private var _storage: Storage? = null
    private suspend fun storage(): Storage {
        return _storage ?: run {
            Storage.factory(settings).also {
                _storage = it
            }
        }
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
        if (remoteFile == null || remoteFileCreated == null) {
            throw FileNotFoundException("ブックマークがまだ保存されていません")
        }
        val remote = storage().readMarksContents(remoteFile)

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
    private suspend fun getLatestRemoteFile(): Pair<FileInfo?, Long?> {
        // ストレージのファイル一覧を取得して最新ファイルを取得
        // 複数回呼ばれると結果が変わらないのに時間がかかるのでプロパティにキャッシュ
        if (remoteFile == null || remoteFileCreated == null) {
            val remoteFiles = storage().lsDir(settings.getFolderNameValue())
                .filter { f ->
                    Regex("""^bookmarks\.\d+\.json$""").containsMatchIn(f.filename)
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
        val children = repos.getMarkNodeChildren(parent)
        val faviconMap = children
            .filter { it.type == MarkType.Bookmark }
            .map { it.domain }
            .distinct()
            .let {
                faviconRepos.findFavicons(it)
            }.associateBy {
                it.domain
            }
        children.forEach {
            it.favicon = faviconMap[it.domain]
        }
        return children
    }

    /**
     * ルートから指定ノードへ辿り着くための名前のリストを取得する．
     * 要するに path みたいな．/root/fooFolder/barMark とかそういう．
     */
    suspend fun getMarkPath(child: MarkNode): List<MarkNode> {
        return child.parent_id?.let { parent_id ->
            repos.getMarkNode(parent_id)?.let { parent ->
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
            repos.saveMarkNode(local)
            // フォルダの反映．いったん全消しして追加しなおす（乱暴）
            if (remote.type == MarkType.Folder) {
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
        if (remote.type == MarkType.Folder) {
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
        if (remote.type == MarkType.Folder) {
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
        if (target.type == MarkType.Folder) {
            for (child in repos.getMarkNodeChildren(target)) {
                removeBookmark(child)
            }
        }
        repos.deleteMarkNode(target)
    }

    /**
     * favicon を Google から HTTP で取得する
     */
    suspend fun fetchFavicons(domains: List<String>) = coroutineScope {
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
            }
    }

    suspend fun fetchFavicon(domain: String) {
        faviconRepos.fetchFavicon(domain)
            ?.let {
                faviconRepos.saveFavicon(it)
            }
    }
}
