package to.sava.cloudmarksandroid.repositories

import io.realm.OrderedRealmCollection
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkType

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

    /**
     * 指定idのMarkNodeのドメイン名ユニークリストを取得する．
     * MarkNodeがFolderだった場合はその配下のBookmark全てのリストを作る．
     */
    fun getUniqueListOfFaviconDomains(id: String): List<String> {
        val target = getMarkNode(id)
        return target?.let { mark ->
            when (mark.type) {
                MarkType.Bookmark -> listOf(mark.domain)
                MarkType.Folder -> {
                    getMarkNodeChildren(mark)
                            .filter { it.type == MarkType.Bookmark }
                            .map { it.domain }
                            .distinct()
                }
            }
        } ?: listOf()
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