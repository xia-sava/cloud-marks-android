package to.sava.cloudmarksandroid.libs

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.delete
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkType
import java.util.*


class Marks (settings: Settings) {

    fun load() {

    }


    private fun createMark(id: String = UUID.randomUUID().toString(),
                         type: MarkType = MarkType.Bookmark,
                         title: String = "",
                         url: String = "",
                         parent: MarkNode? = null): MarkNode {
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        val mark = realm.createObject<MarkNode>(id)
        mark.type = type
        mark.title = title
        mark.url = url
        mark.parent = parent
        realm.commitTransaction()
        return mark
    }

    fun setupFixture() {
        val f = MarkType.Folder
        val b = MarkType.Bookmark

        Realm.getDefaultInstance().executeTransaction {
            Realm.getDefaultInstance().delete<MarkNode>()
        }

        val root = createMark(id = "root", type = f, title = "root")
        val menu = createMark(type = f, title = "ブックマークメニュー", parent = root)
        createMark(type = f, title = "ブックマークツールバー", parent = root)
        createMark(type = f, title = "他のブックマーク", parent = root)
        createMark(type = f, title = "モバイルのブックマーク", parent = root)

        for (i in 1..3) {
            val parent1 = createMark(type = f, title = "フォルダ$i", parent = menu)
            for (j in 1..25) {
                when (j) {
                    in 1..3 -> {
                        val parent2 = createMark(type = f, title = "フォルダ$i-$j", parent = parent1)
                        for (k in 1..3) {
                            val parent3 = createMark(type = f, title = "フォルダ$i-$j-$k", parent = parent2)
                            for (m in 1..5) {
                                createMark(type = b, title = "ブックマーク $i-$j-$k-$m",
                                        url = "https://xia.sava.to/$i-$j-$k-$m", parent = parent3)
                            }
                        }
                    }
                    else -> {
                        createMark(type = b, title = "ブックマーク $i-$j",
                                url = "https://xia.sava.to/$i-$j", parent = parent1)
                    }
                }
            }
        }
    }
}