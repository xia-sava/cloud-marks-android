package to.sava.cloudmarksandroid.repositories

import to.sava.cloudmarksandroid.models.MarkNode

class MarkNodeRepository : Repository<MarkNode>() {
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
    fun getMarkNodeChildren(parent: MarkNode): MutableList<MarkNode> {
        return findAll {
            it.equalTo("parent.id", parent.id)
                    .sort("order")
        }
    }
}