package to.sava.cloudmarksandroid.databases.dao

import androidx.room.*
import to.sava.cloudmarksandroid.databases.models.MarkNode

@Dao
interface MarkNodeDao {
    /**
     * root MarkNode を取得する．
     */
    @Query("SELECT * FROM mark_node WHERE parent_id is NULL")
    fun getRootMarkNode(): MarkNode?

    /**
     * 指定 ID の MarkNode を取得する．
     */
    @Query("SELECT * FROM mark_node WHERE id = :id")
    fun getMarkNode(id: Long): MarkNode?

    /**
     * 指定 ID の直接の children を取得する．
     */
    @Query("SELECT * FROM mark_node WHERE parent_id = :parentId ORDER BY `order`")
    fun getMarkNodeChildren(parentId: Long): List<MarkNode>

    /**
     * 指定 ID の直接の children の数を取得する．
     */
    @Query("SELECT COUNT(*) FROM mark_node WHERE parent_id = :parentId")
    fun getMarkNodeChildrenCount(parentId: Long): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(entity: MarkNode): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(entities: List<MarkNode>): List<Long>

    @Delete
    fun delete(entity: MarkNode): Int

    @Delete
    fun delete(entities: List<MarkNode>): Int
}