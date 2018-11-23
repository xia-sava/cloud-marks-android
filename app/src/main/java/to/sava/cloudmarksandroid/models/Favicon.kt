package to.sava.cloudmarksandroid.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * ブックマークツリーを内部的に保持する用のクラス．ノードと呼ぼう．
 * ツリー構造は親へのリンクだけ持つ．
 * Realm DBに保存される形式もこちら．
 */
open class Favicon(@PrimaryKey open var domain: String = "",
                   open var favicon: ByteArray = byteArrayOf(),
                   open var size: Int = 0
) : RealmObject()
