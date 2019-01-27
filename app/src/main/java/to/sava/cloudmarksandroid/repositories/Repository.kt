package to.sava.cloudmarksandroid.repositories

import io.realm.Realm
import io.realm.RealmObject

abstract class Repository<T : RealmObject> {

    abstract val modelClass: Class<T>

    fun findFirst(): T? {
        val realm = Realm.getDefaultInstance()
        val managed = realm.where(modelClass).findFirst()
        val detached = managed?.let { realm.copyFromRealm(it) }
        realm.close()
        return detached
    }

    companion object {
        /**
         * リポジトリ即席生成メソッド．
         *   val repo = Repository.create<ModelClass>()
         * のように使用する．
         * リポジトリとして特に専用処理が必要なければ基本機能はこれで使える．
         */
        inline fun <reified RT : RealmObject> create(): Repository<RT> {
            return object : Repository<RT>() {
                override val modelClass = RT::class.java
            }
        }

        /**
         * リポジトリ即席生成メソッド．
         * Javaからの呼び方が判らなかったので分けた．
         *   Repository<ModelClass> repo = Repository.create(ModelClass.class);
         * のように使用する．
         */
        @JvmStatic
        fun <JT: RealmObject> create(classType: Class<JT>): Repository<JT> {
            return object : Repository<JT>() {
                override val modelClass = classType
            }
        }
    }
}
