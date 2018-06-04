package to.sava.cloudmarksandroid.models

import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.lang.reflect.Type
import java.util.*


enum class MarkType(val rawValue: Int) {
    Folder(0),
    Bookmark(1),
}


open class MarkNode (@PrimaryKey open var id: String = UUID.randomUUID().toString(),
                     open var typeValue: Int = MarkType.Bookmark.rawValue,
                     open var title: String = "",
                     open var url: String = "",
                     open var parent: MarkNode? = null): RealmObject() {

    open var type: MarkType
        get() = MarkType.values().first { it.rawValue == typeValue }
        set(value) {
            typeValue = value.rawValue
        }
}

class MarkNodeJson(val type: MarkType, val title: String, val url: String, val children: List<MarkNodeJson>) {
    companion object {
        fun jsonSerialize(mark: MarkNodeJson, typeOfSrc: Type, context: JsonSerializationContext): JsonObject {
            // ブラウザ側に合わせてプロパティ順序を保存しつつシリアライズするやつ
            return JsonObject().apply {
                addProperty("type", mark.type.rawValue)
                addProperty("title", mark.title)
                addProperty("url", mark.url)
                add("children", context.serialize(mark.children))
            }
        }
    }
}
