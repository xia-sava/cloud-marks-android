package to.sava.cloudmarksandroid.databases.repositories

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import to.sava.cloudmarksandroid.databases.dao.MarkNodeDao
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.databases.models.MarkType

class MarkNodeRepositoryTest {

    private lateinit var dao: MarkNodeDao
    private lateinit var repo: MarkNodeRepository

    @BeforeEach
    fun setup() {
        dao = mockk(relaxed = true)
        repo = MarkNodeRepository(dao)
    }

    private fun markNode(
        id: Long = 0L,
        type: MarkType = MarkType.Bookmark,
        title: String = "",
    ) = MarkNode(type, title, "", 0, null).also { it.id = id }

    @Nested
    inner class GetMarkNode {

        /** ROOT_ID は getRootMarkNode に委譲する */
        @Test
        fun rootId_delegatesToGetRoot() = runTest {
            val root = markNode(id = MarkNode.ROOT_ID, type = MarkType.Folder, title = "root")
            coEvery { dao.getRootMarkNode() } returns root
            assertEquals(root, repo.getMarkNode(MarkNode.ROOT_ID))
            coVerify { dao.getRootMarkNode() }
        }

        /** ROOT_ID 以外は getMarkNode(id) に委譲する */
        @Test
        fun nonRootId_delegatesToGetById() = runTest {
            val node = markNode(id = 42, title = "test")
            coEvery { dao.getMarkNode(42) } returns node
            assertEquals(node, repo.getMarkNode(42))
            coVerify { dao.getMarkNode(42) }
        }

        /** 存在しない ID は null を返す */
        @Test
        fun notFound_returnsNull() = runTest {
            coEvery { dao.getMarkNode(999) } returns null
            assertNull(repo.getMarkNode(999))
        }
    }

    @Nested
    inner class CreateMarkNode {

        /** MarkNode を作成し、save で得た ID を設定して返す */
        @Test
        fun createsAndSetsId() = runTest {
            coEvery { dao.save(any<MarkNode>()) } returns 10L
            val result = repo.createMarkNode(MarkType.Bookmark, "test", "https://example.com", 0, 1L)
            assertEquals(10L, result.id)
            assertEquals("test", result.title)
            assertEquals(MarkType.Bookmark, result.type)
        }
    }

    @Nested
    inner class GetMarkNodeChildren {

        /** 親ノードの子を取得する */
        @Test
        fun byParentNode() = runTest {
            val parent = markNode(id = 5, type = MarkType.Folder)
            val children = listOf(markNode(id = 6), markNode(id = 7))
            coEvery { dao.getMarkNodeChildren(5L) } returns children
            assertEquals(children, repo.getMarkNodeChildren(parent))
        }
    }
}
