package to.sava.cloudmarksandroid.databases.repositories

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
     * 指定 ID の直接の children を取得する．
     */
    suspend fun getMarkNodeChildren(parentId: Long): List<MarkNode> {
        return access.getMarkNodeChildren(parentId)
    }

    suspend fun getMarkNodeChildren(parent: MarkNode): List<MarkNode> {
        return getMarkNodeChildren(parent.id)
    }

    /**
     * 指定 ID の直接の children の数を取得する．
     */
    private suspend fun getMarkNodeChildrenCount(parentId: Long): Long {
        return access.getMarkNodeChildrenCount(parentId)
    }

    suspend fun getMarkNodeChildrenCount(parent: MarkNode): Long {
        return getMarkNodeChildrenCount(parent.id)
    }

    /**
     * 全 MarkNode を取得する．
     */
    suspend fun getAllMarkNode(): List<MarkNode> {
        return access.getAllMarkNode()
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
