package to.sava.cloudmarksandroid.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
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
