package to.sava.cloudmarksandroid.repositories

import io.realm.OrderedRealmCollection
import to.sava.cloudmarksandroid.models.MarkNode

class MarkNodeRepository {
    private val access by lazy { RealmAccess(MarkNode::class) }


    /**
     * 指定名のMarkNodeを取得する．
     */
    fun getMarkNode(id: String): MarkNode? {
        return access.find("id", id)
    }

    /**
     * MarkNodeの直接のchildrenを取得する．
     */
    fun getMarkNodeChildren(parent: MarkNode): MutableList<MarkNode> {
        return access.realmListQuery {
            it.equalTo("parent.id", parent.id)
                    .sort("order")
        }.toMutableList()
    }

    /**
     * 指定名のMarkNodeを取得する．
     */
    fun getManagedMarkNode(id: String): MarkNode? {
        return access.managed().find("id", id)
    }

    /**
     * MarkNodeの直接のchildrenを取得する．
     */
    fun getManagedMarkNodeChildren(parent: MarkNode): OrderedRealmCollection<MarkNode> {
        return access.managed().realmListQuery { query ->
            query.equalTo("parent.id", parent.id)
                    .sort("order")
        } as OrderedRealmCollection<MarkNode>
    }

    fun createMarkNode(markId: String): MarkNode {
        return access.createEntity(markId)
    }

    fun saveMarkNode(entity: MarkNode) {
        access.save(entity)
    }

    fun deleteMarkNode(entity: MarkNode) {
        access.delete(entity)
    }

    fun transactionScope(func: () -> Unit) {
        access.realmTransaction { func() }
    }
}