package to.sava.cloudmarksandroid.databases.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ブックマークツリーを内部的に保持する用のクラス．ノードと呼ぼう．
 * ツリー構造は親へのリンクだけ持つ．
 * Realm DBに保存される形式もこちら．
 */
@Entity(tableName = "favicon")
class Favicon(
    @PrimaryKey
    var domain: String = "",
    var favicon: ByteArray = ByteArray(0),
    var size: Int = 0
) {
    override fun toString() = domain
}
