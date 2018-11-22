package to.sava.cloudmarksandroid.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*


enum class MarkType(val rawValue: Int) {
    Folder(0),
    Bookmark(1),
}

/**
 * ブックマークツリーを内部的に保持する用のクラス．ノードと呼ぼう．
 * ツリー構造は親へのリンクだけ持つ．
 * Realm DBに保存される形式もこちら．
 */
open class MarkNode (@PrimaryKey open var id: String = newKey(),
                     open var typeValue: Int = MarkType.Bookmark.rawValue,
                     open var title: String = "",
                     open var url: String = "",
                     open var order: Int = 0,
                     open var parent: MarkNode? = null): RealmObject() {

    companion object {
        const val ROOT_ID = "root________"

        fun newKey() = UUID.randomUUID().toString()
    }

    open var type: MarkType
        get() = MarkType.values().first { it.rawValue == typeValue }
        set(value) {
            typeValue = value.rawValue
        }
}

/**
 * JSONから変換されたデータをMarkTreeNodeとほぼ同じ形式で保持する用のクラス．
 * Androidではこちらが内部処理のメインのMarksツリー．
 * ツリー構造は再帰して持つ．
 */
class MarkTreeNode(val type: MarkType,
                   val title: String,
                   val url: String,
                   val children: List<MarkTreeNode>) {

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
