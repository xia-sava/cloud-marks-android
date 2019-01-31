package to.sava.cloudmarksandroid.repositories

import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.kotlin.where
import to.sava.cloudmarksandroid.models.MarkNode

class MarkNodeRepository : Repository<MarkNode>() {
    override val modelClass = MarkNode::class.java


    /**
     * 指定名のMarkNodeを取得する．
     */
    fun getMarkNode(id: String): MarkNode? {
        return find {
            it.equalTo("id", id)
                    .findFirst()
        }
    }

    /**
     * MarkNodeの直接のchildrenを取得する．
     */
    fun getMarkNodeChildren(parent: MarkNode): MutableList<MarkNode> {
        return findAll {
            it
                    .equalTo("parent.id", parent.id)
                    .sort("order")
                    .findAll()
        }
    }

    /**
     * MarkNodeの直接のchildrenを managed 状態で取得する．
     */
    fun getMarkNodeChildrenManaged(realm: Realm, parent: MarkNode): OrderedRealmCollection<MarkNode> {
        return realm
                .where<MarkNode>()
                .equalTo("parent.id", parent.id)
                .sort("order")
                .findAll()
    }
}