package to.sava.cloudmarksandroid.modules

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import to.sava.cloudmarksandroid.databases.models.Favicon
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.databases.models.MarkTreeNode
import to.sava.cloudmarksandroid.databases.models.MarkType
import to.sava.cloudmarksandroid.databases.repositories.FaviconRepository
import to.sava.cloudmarksandroid.databases.repositories.MarkNodeRepository

class MarksTest {

    private lateinit var settings: Settings
    private lateinit var repos: MarkNodeRepository
    private lateinit var faviconRepos: FaviconRepository
    private lateinit var storage: Storage
    private var fixedClock = 99999L
    private lateinit var marks: Marks

    @BeforeEach
    fun setup() {
        settings = mockk(relaxed = true)
        repos = mockk(relaxed = true)
        faviconRepos = mockk(relaxed = true)
        storage = mockk(relaxed = true)
        fixedClock = 99999L
        marks = Marks(settings, repos, faviconRepos, { storage }, { fixedClock })
    }

    /** テスト用 MarkNode を作成するヘルパー */
    private fun markNode(
        id: Long = 0L,
        type: MarkType = MarkType.Bookmark,
        title: String = "",
        url: String = "",
        order: Int = 0,
        parentId: Long? = null,
    ) = MarkNode(type, title, url, order, parentId).also { it.id = id }

    @Nested
    inner class GetMark {

        /** 存在する ID のノードを取得する */
        @Test
        fun existingId() = runTest {
            val node = markNode(id = 42, title = "test")
            coEvery { repos.getMarkNode(42) } returns node
            assertEquals(node, marks.getMark(42))
        }

        /** 存在しない ID は null を返す */
        @Test
        fun nonExistingId() = runTest {
            coEvery { repos.getMarkNode(999) } returns null
            assertNull(marks.getMark(999))
        }
    }

    @Nested
    inner class GetMarkChildren {

        /** 親ノードの子ノード一覧を取得する */
        @Test
        fun returnsChildren() = runTest {
            val parent = markNode(id = 1, type = MarkType.Folder)
            val children = listOf(
                markNode(id = 2, title = "a"),
                markNode(id = 3, title = "b"),
            )
            coEvery { repos.getMarkNodeChildren(parent) } returns children
            assertEquals(children, marks.getMarkChildren(parent))
        }
    }

    @Nested
    inner class GetMarkPath {

        /** ルートノードはそのノードだけのリストを返す */
        @Test
        fun rootNode() = runTest {
            val root = markNode(id = 1, type = MarkType.Folder, title = "root", parentId = null)
            assertEquals(listOf(root), marks.getMarkPath(root))
        }

        /** 子ノードはルートからのパスを返す */
        @Test
        fun childNode() = runTest {
            val root = markNode(id = 1, type = MarkType.Folder, title = "root", parentId = null)
            val child = markNode(id = 2, title = "child", parentId = 1)
            coEvery { repos.getMarkNode(1) } returns root
            val path = marks.getMarkPath(child)
            assertEquals(2, path.size)
            assertEquals("root", path[0].title)
            assertEquals("child", path[1].title)
        }

        /** 深いネストのパスを返す */
        @Test
        fun deepNested() = runTest {
            val root = markNode(id = 1, type = MarkType.Folder, title = "root", parentId = null)
            val mid = markNode(id = 2, type = MarkType.Folder, title = "mid", parentId = 1)
            val leaf = markNode(id = 3, title = "leaf", parentId = 2)
            coEvery { repos.getMarkNode(1) } returns root
            coEvery { repos.getMarkNode(2) } returns mid
            val path = marks.getMarkPath(leaf)
            assertEquals(3, path.size)
            assertEquals(listOf("root", "mid", "leaf"), path.map { it.title })
        }
    }

    @Nested
    inner class InitializeDb {

        /** root が存在しない場合は root とデフォルトフォルダを作成する */
        @Test
        fun createsRootAndDefault() = runTest {
            coEvery { repos.getRootMarkNode() } returns null
            val titleSlot = mutableListOf<String>()
            coEvery {
                repos.createMarkNode(any(), capture(titleSlot), any(), any(), any())
            } answers {
                markNode(id = titleSlot.size.toLong(), type = MarkType.Folder, title = titleSlot.last())
            }
            marks.initializeDb()
            coVerify(exactly = 2) { repos.createMarkNode(MarkType.Folder, any(), any(), any(), any()) }
            assertEquals("root", titleSlot[0])
            assertEquals("デフォルトブックマークフォルダ", titleSlot[1])
        }

        /** root が存在する場合は何もしない */
        @Test
        fun rootExists_doesNothing() = runTest {
            coEvery { repos.getRootMarkNode() } returns markNode(id = 1, type = MarkType.Folder)
            marks.initializeDb()
            coVerify(exactly = 0) { repos.createMarkNode(any(), any(), any(), any(), any()) }
        }
    }

    @Nested
    inner class FindFavicons {

        /** ドメインリストに対応する favicon を返す */
        @Test
        fun returnsFavicons() = runTest {
            val favicons = listOf(
                Favicon("example.com", ByteArray(0), 16),
            )
            coEvery { faviconRepos.findFavicons(listOf("example.com")) } returns favicons
            assertEquals(favicons, marks.findFavicons(listOf("example.com")))
        }
    }

    @Nested
    inner class Load {

        /** リモートの内容で DB を更新し、最終同期日時を保存する */
        @Test
        fun updatesDbAndSavesTimestamp() = runTest {
            fixedClock = 50000L
            val fileInfo = FileInfo("bookmarks/bookmarks.1000.json")
            val remote = MarkTreeNode(MarkType.Folder, "root", "", listOf())
            val root = markNode(id = 1, type = MarkType.Folder, title = "root")
            coEvery { storage.listDir() } returns listOf(fileInfo)
            coEvery { storage.readMarkFile(fileInfo) } returns remote
            coEvery { repos.getRootMarkNode() } returns root
            coEvery { repos.getMarkNodeChildren(root) } returns emptyList()
            marks.load()
            coVerify { settings.setLastSynced(1000L) }
            coVerify { settings.setLastBookmarkModified(50000L) }
        }

        /** リモートファイルが存在しない場合は FileNotFoundException を投げる */
        @Test
        fun noRemoteFile_throwsException() = runTest {
            coEvery { storage.listDir() } returns emptyList()
            assertThrows(FileNotFoundException::class.java) {
                kotlinx.coroutines.test.runTest {
                    marks.load()
                }
            }
        }

        /** タイトルに差分がある場合は DB を更新する */
        @Test
        fun titleDiff_updatesDb() = runTest {
            val fileInfo = FileInfo("bookmarks/bookmarks.2000.json")
            val remote = MarkTreeNode(MarkType.Folder, "new-root", "", listOf())
            val root = markNode(id = 1, type = MarkType.Folder, title = "old-root")
            coEvery { storage.listDir() } returns listOf(fileInfo)
            coEvery { storage.readMarkFile(fileInfo) } returns remote
            coEvery { repos.getRootMarkNode() } returns root
            coEvery { repos.getMarkNodeChildren(root) } returns emptyList()
            marks.load()
            assertEquals("new-root", root.title)
            coVerify { repos.saveMarkNode(root) }
        }

        /** 差分がない場合はノードを保存しない */
        @Test
        fun noDiff_doesNotSave() = runTest {
            val fileInfo = FileInfo("bookmarks/bookmarks.3000.json")
            val remote = MarkTreeNode(MarkType.Folder, "root", "", listOf())
            val root = markNode(id = 1, type = MarkType.Folder, title = "root")
            coEvery { storage.listDir() } returns listOf(fileInfo)
            coEvery { storage.readMarkFile(fileInfo) } returns remote
            coEvery { repos.getRootMarkNode() } returns root
            coEvery { repos.getMarkNodeChildren(root) } returns emptyList()
            marks.load()
            coVerify(exactly = 0) { repos.saveMarkNode(any()) }
        }

        /** 子ノード数に差分がある場合は全消し＋再作成する */
        @Test
        fun childCountDiff_recreatesChildren() = runTest {
            val fileInfo = FileInfo("bookmarks/bookmarks.4000.json")
            val childTree = MarkTreeNode(MarkType.Bookmark, "new-child", "https://new.example.com", listOf())
            val remote = MarkTreeNode(MarkType.Folder, "root", "", listOf(childTree))
            val root = markNode(id = 1, type = MarkType.Folder, title = "root")
            coEvery { storage.listDir() } returns listOf(fileInfo)
            coEvery { storage.readMarkFile(fileInfo) } returns remote
            coEvery { repos.getRootMarkNode() } returns root
            // 既存の子は0個なのにリモートは1個 → 差分あり
            coEvery { repos.getMarkNodeChildren(root) } returns emptyList()
            coEvery { repos.createMarkNode(any(), any(), any(), any(), any()) } returns
                markNode(id = 10, type = MarkType.Bookmark, title = "new-child")
            marks.load()
            coVerify { repos.createMarkNode(MarkType.Bookmark, "new-child", "https://new.example.com", 0, 1L) }
        }

        /** 子ノードに差分がある場合は既存の子を削除してから再作成する */
        @Test
        fun existingChildren_deletedBeforeRecreate() = runTest {
            val fileInfo = FileInfo("bookmarks/bookmarks.5000.json")
            val remote = MarkTreeNode(MarkType.Folder, "root", "", listOf())
            val root = markNode(id = 1, type = MarkType.Folder, title = "root")
            val existingChild = markNode(id = 2, type = MarkType.Bookmark, title = "old-child")
            coEvery { storage.listDir() } returns listOf(fileInfo)
            coEvery { storage.readMarkFile(fileInfo) } returns remote
            coEvery { repos.getRootMarkNode() } returns root
            // 既存の子は1個なのにリモートは0個 → 差分あり
            coEvery { repos.getMarkNodeChildren(root) } returns listOf(existingChild)
            marks.load()
            coVerify { repos.deleteMarkNode(existingChild) }
        }

        /** 最新 timestamp のファイルが選ばれる */
        @Test
        fun selectsLatestFile() = runTest {
            val oldFile = FileInfo("bookmarks/bookmarks.100.json")
            val newFile = FileInfo("bookmarks/bookmarks.999.json")
            val remote = MarkTreeNode(MarkType.Folder, "root", "", listOf())
            val root = markNode(id = 1, type = MarkType.Folder, title = "root")
            coEvery { storage.listDir() } returns listOf(oldFile, newFile)
            coEvery { storage.readMarkFile(newFile) } returns remote
            coEvery { repos.getRootMarkNode() } returns root
            coEvery { repos.getMarkNodeChildren(root) } returns emptyList()
            marks.load()
            coVerify { storage.readMarkFile(newFile) }
            coVerify { settings.setLastSynced(999L) }
        }

        /** root が DB に存在しない場合は新規作成して続行する */
        @Test
        fun noRoot_createsRootAndContinues() = runTest {
            val fileInfo = FileInfo("bookmarks/bookmarks.6000.json")
            val remote = MarkTreeNode(MarkType.Folder, "root", "", listOf())
            val newRoot = markNode(id = 1, type = MarkType.Folder, title = "root")
            coEvery { storage.listDir() } returns listOf(fileInfo)
            coEvery { storage.readMarkFile(fileInfo) } returns remote
            coEvery { repos.getRootMarkNode() } returns null
            coEvery { repos.createMarkNode(MarkType.Folder, "root", "", 0, null) } returns newRoot
            coEvery { repos.getMarkNodeChildren(newRoot) } returns emptyList()
            marks.load()
            coVerify { repos.createMarkNode(MarkType.Folder, "root", "", 0, null) }
        }

        /** progressListener が設定されている場合は進捗が通知される */
        @Test
        fun progressListenerCalled() = runTest {
            val fileInfo = FileInfo("bookmarks/bookmarks.7000.json")
            val remote = MarkTreeNode(MarkType.Folder, "root", "", listOf())
            val root = markNode(id = 1, type = MarkType.Folder, title = "root")
            coEvery { storage.listDir() } returns listOf(fileInfo)
            coEvery { storage.readMarkFile(fileInfo) } returns remote
            coEvery { repos.getRootMarkNode() } returns root
            coEvery { repos.getMarkNodeChildren(root) } returns emptyList()
            val progress = mutableListOf<Pair<String, Int>>()
            marks.progressListener = { folder, percent -> progress.add(folder to percent) }
            marks.load()
            assertEquals(1, progress.size)
            assertEquals("root", progress[0].first)
        }

        /** ネストしたフォルダ構造の再帰的な反映 */
        @Test
        fun nestedFolders_appliedRecursively() = runTest {
            val fileInfo = FileInfo("bookmarks/bookmarks.8000.json")
            val leaf = MarkTreeNode(MarkType.Bookmark, "leaf", "https://leaf.example.com", listOf())
            val subFolder = MarkTreeNode(MarkType.Folder, "sub", "", listOf(leaf))
            val remote = MarkTreeNode(MarkType.Folder, "root", "", listOf(subFolder))
            val root = markNode(id = 1, type = MarkType.Folder, title = "root")
            coEvery { storage.listDir() } returns listOf(fileInfo)
            coEvery { storage.readMarkFile(fileInfo) } returns remote
            coEvery { repos.getRootMarkNode() } returns root
            // 子ノード数が違う → 差分あり → 全消し＋再作成
            coEvery { repos.getMarkNodeChildren(root) } returns emptyList()
            val subNode = markNode(id = 10, type = MarkType.Folder, title = "sub")
            val leafNode = markNode(id = 11, type = MarkType.Bookmark, title = "leaf")
            coEvery { repos.createMarkNode(MarkType.Folder, "sub", "", 0, 1L) } returns subNode
            coEvery { repos.createMarkNode(MarkType.Bookmark, "leaf", "https://leaf.example.com", 0, 10L) } returns leafNode
            marks.load()
            coVerify { repos.createMarkNode(MarkType.Folder, "sub", "", 0, 1L) }
            coVerify { repos.createMarkNode(MarkType.Bookmark, "leaf", "https://leaf.example.com", 0, 10L) }
        }

        /** 読込みのたびにファイル一覧を取り直す */
        @Test
        fun refetchesFileListOnEachLoad() = runTest {
            val fileInfo = FileInfo("bookmarks/bookmarks.1000.json")
            val remote = MarkTreeNode(MarkType.Folder, "root", "", listOf())
            val root = markNode(id = 1, type = MarkType.Folder, title = "root")
            coEvery { storage.listDir() } returns listOf(fileInfo)
            coEvery { storage.readMarkFile(fileInfo) } returns remote
            coEvery { repos.getRootMarkNode() } returns root
            coEvery { repos.getMarkNodeChildren(root) } returns emptyList()

            marks.load()
            marks.load()

            coVerify(exactly = 2) { storage.listDir() }
        }

        /** 前回の読込み以降に書き込まれたファイルを次の読込みで拾う */
        @Test
        fun picksUpFileAddedAfterPreviousLoad() = runTest {
            val oldFile = FileInfo("bookmarks/bookmarks.1000.json")
            val newFile = FileInfo("bookmarks/bookmarks.2000.json")
            val remote = MarkTreeNode(MarkType.Folder, "root", "", listOf())
            val root = markNode(id = 1, type = MarkType.Folder, title = "root")
            coEvery { storage.listDir() } returns listOf(oldFile) andThen listOf(oldFile, newFile)
            coEvery { storage.readMarkFile(any()) } returns remote
            coEvery { repos.getRootMarkNode() } returns root
            coEvery { repos.getMarkNodeChildren(root) } returns emptyList()

            marks.load()
            marks.load()

            coVerify { storage.readMarkFile(newFile) }
            coVerify { settings.setLastSynced(2000L) }
        }
    }

    @Nested
    inner class FetchAllFavicons {

        /** 既に favicon がある domain は取得しない */
        @Test
        fun skipsExistingFavicons() = runTest {
            val nodes = listOf(
                markNode(id = 1, url = "https://example.com/page"),
                markNode(id = 2, url = "https://other.com/page"),
            )
            coEvery { repos.getAllMarkNode() } returns nodes
            coEvery { faviconRepos.findAllFavicons() } returns listOf(
                Favicon("example.com", ByteArray(0), 16),
            )
            coEvery { faviconRepos.fetchFavicon("other.com") } returns
                Favicon("other.com", ByteArray(0), 16)
            marks.fetchAllFavicons()
            coVerify(exactly = 0) { faviconRepos.fetchFavicon("example.com") }
            coVerify { faviconRepos.fetchFavicon("other.com") }
        }
    }
}
