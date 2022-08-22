package to.sava.cloudmarksandroid.databases.models

import android.net.Uri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey


enum class MarkType(val rawValue: Int) {
    Folder(0),
    Bookmark(1),
}

/**
 * ブックマークツリーを内部的に保持する用のクラス．ノードと呼ぼう．
 * ツリー構造は親へのリンクだけ持つ．
 * Realm DBに保存される形式もこちら．
 */
@Entity(tableName = "mark_node")
class MarkNode(
    var type: MarkType = MarkType.Bookmark,
    var title: String = "",
    var url: String = "",
    var order: Int = 0,
    var parent_id: Long? = null
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L

    override fun toString() = "${type.name}/${parent_id}/${order}/${title}/<${url}>"

    val domain: String get() = parseDomain(url)

    @Ignore
    var favicon: Favicon? = null

    companion object {
        const val ROOT_ID = 1L
        fun parseDomain(url: String): String = Uri.parse(url).host ?: ""
    }
}

/**
 * JSONから変換されたデータをMarkTreeNodeとほぼ同じ形式で保持する用のクラス．
 * Androidではこちらが内部処理のメインのMarksツリー．
 * ツリー構造は再帰して持つ．
 */
class MarkTreeNode(
    val type: MarkType,
    val title: String,
    val url: String,
    val children: List<MarkTreeNode>
) {
    override fun toString() = "${type.name}/${title}/<${url}>/${children.size}"

    /**
     * ツリー構造を辿ってアイテム数カウントする．
     */
    fun countChildren(filter: MarkType? = null): Long {
        return when {
            filter == null -> 1
            type == filter -> 1
            else -> 0
        } + if (type == MarkType.Folder)
            children.map { it.countChildren(type) }.sum()
        else
            0
    }
}
