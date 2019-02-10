package to.sava.cloudmarksandroid.repositories

import io.realm.OrderedRealmCollection
import to.sava.cloudmarksandroid.models.MarkNode

class MarkNodeManagedRepository : ManagedRepository<MarkNode>() {
    override val modelClass = MarkNode::class.java


    /**
     * 指定名のMarkNodeを取得する．
     */
    fun getMarkNode(id: String): MarkNode? {
        return find("id", id)
    }

    /**
     * MarkNodeの直接のchildrenを取得する．
     */
    fun getMarkNodeChildren(parent: MarkNode): OrderedRealmCollection<MarkNode> {
        return findAll { query ->
            query.equalTo("parent.id", parent.id)
                    .sort("order")
        }
    }
}