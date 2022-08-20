package to.sava.cloudmarksandroid.databases.repositories

import kotlinx.coroutines.flow.Flow
import to.sava.cloudmarksandroid.databases.dao.MarkNodeDao
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.databases.models.MarkType

class MarkNodeRepository(
    private val access: MarkNodeDao
) {

    /**
     * root MarkNode を取得する．
     */
    suspend fun getRootMarkNode(): MarkNode? {
        return access.getRootMarkNode()
    }

    /**
     * 指定 ID の MarkNode を取得する．
     */
    suspend fun getMarkNode(id: Long): MarkNode? {
        return if (id == MarkNode.ROOT_ID) {
            access.getRootMarkNode()
        } else {
            access.getMarkNode(id)
        }
    }

    /**
     * 指定 ID の MarkNode Flow を取得する．
     */
    fun getMarkNodeFlow(id: Long): Flow<MarkNode> {
        return if (id == MarkNode.ROOT_ID) {
            access.getRootMarkNodeFlow()
        } else {
            access.getMarkNodeFlow(id)
        }
    }

    /**
     * 指定 ID の直接の children を取得する．
     */
    suspend fun getMarkNodeChildren(parent_id: Long): List<MarkNode> {
        return access.getMarkNodeChildren(parent_id)
    }

    suspend fun getMarkNodeChildren(parent: MarkNode): List<MarkNode> {
        return getMarkNodeChildren(parent.id)
    }

    /**
     * 指定 ID の直接の children の数を取得する．
     */
    private suspend fun getMarkNodeChildrenCount(parent_id: Long): Long {
        return access.getMarkNodeChildrenCount(parent_id)
    }

    suspend fun getMarkNodeChildrenCount(parent: MarkNode): Long {
        return getMarkNodeChildrenCount(parent.id)
    }

    /**
     * 指定idのMarkNodeのドメイン名ユニークリストを取得する．
     * MarkNodeがFolderだった場合はその配下のBookmark全てのリストを作る．
     */
    suspend fun getUniqueListOfFaviconDomains(id: Long): List<String> {
        val target = getMarkNode(id)
        return target?.let { mark ->
            when (mark.type) {
                MarkType.Bookmark -> listOf(mark.domain)
                MarkType.Folder -> {
                    getMarkNodeChildren(mark)
                        .filter { it.type == MarkType.Bookmark }
                        .map { it.domain }
                        .filter { it.isNotEmpty() }
                        .distinct()
                }
            }
        } ?: listOf()
    }

    /**
     * 新規 MarkNode を作成する．
     */
    suspend fun createMarkNode(
        type: MarkType, title: String, url: String,
        order: Int, parentId: Long?
    ): MarkNode {
        val mark = MarkNode(type, title, url, order, parentId)
        mark.id = access.save(mark)
        return mark
    }

    suspend fun saveMarkNode(entity: MarkNode) = access.save(entity)
    suspend fun saveMarkNodes(entities: List<MarkNode>) = access.save(entities)

    suspend fun deleteMarkNode(entity: MarkNode) = access.delete(entity)
    suspend fun deleteMarkNodes(entities: List<MarkNode>) = access.delete(entities)
}