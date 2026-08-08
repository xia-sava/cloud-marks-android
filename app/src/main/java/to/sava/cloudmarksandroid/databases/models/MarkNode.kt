package to.sava.cloudmarksandroid.databases.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


@Serializable(with = MarkTypeSerializer::class)
enum class MarkType(val rawValue: Int) {
    Folder(0),
    Bookmark(1),
}

/**
 * cloud_marks形式のJSONでは MarkType を列挙子の名前ではなく rawValue の整数で表現する．
 */
object MarkTypeSerializer : KSerializer<MarkType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MarkType", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: MarkType) {
        encoder.encodeInt(value.rawValue)
    }

    override fun deserialize(decoder: Decoder): MarkType {
        val rawValue = decoder.decodeInt()
        return MarkType.entries.firstOrNull { it.rawValue == rawValue }
            ?: throw SerializationException("未知のMarkTypeです: $rawValue")
    }
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
    @ColumnInfo(name = "parent_id")
    var parentId: Long? = null
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L

    override fun toString() = "${type.name}/${parentId}/${order}/${title}/<${url}>"

    val domain: String get() = parseDomain(url)

    val isBookmark get() = this.type == MarkType.Bookmark
    val isFolder get() = this.type == MarkType.Folder

    companion object {
        const val ROOT_ID = 1L
        fun parseDomain(url: String): String =
            runCatching { java.net.URI(url).host }.getOrNull() ?: ""
    }
}

/**
 * JSONから変換されたデータをMarkTreeNodeとほぼ同じ形式で保持する用のクラス．
 * Androidではこちらが内部処理のメインのMarksツリー．
 * ツリー構造は再帰して持つ．
 */
@Serializable
class MarkTreeNode(
    val type: MarkType,
    val title: String,
    val url: String,
    val children: List<MarkTreeNode>
) {
    val isBookmark get() = this.type == MarkType.Bookmark
    val isFolder get() = this.type == MarkType.Folder

    override fun toString() = "${type.name}/${title}/<${url}>/${children.size}"

    /**
     * ツリー構造を辿ってアイテム数カウントする．
     */
    fun countChildren(filter: MarkType? = null): Long {
        return when {
            filter == null -> 1
            type == filter -> 1
            else -> 0
        } + if (isFolder)
            children.sumOf { it.countChildren(type) }
        else
            0
    }
}
