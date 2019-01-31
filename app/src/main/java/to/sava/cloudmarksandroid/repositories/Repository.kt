package to.sava.cloudmarksandroid.repositories

import io.realm.*

/**
 * Realm リポジトリパターンの実装．
 *
 * 基本的には継承クラスで modelClass に RealmObject クラスを初期値としたリポジトリクラスを作成する．
 * 基本機能以外いらなければ直接に Repository<T>.create() で生成できる．
 */
abstract class Repository<T : RealmObject> {

    /**
     * リポジトリがターゲットとする RealmObject のクラス
     */
    abstract val modelClass: Class<T>

    /**
     * Realm インスタンス．
     * 基本は毎回 getDefaultInstance() して close() する．
     */
    private val realm
        get() = getNewRealmInstance()

    fun getNewRealmInstance(): Realm {
        return Realm.getDefaultInstance()
    }

    /**
     * キー指定してオブジェクトを新規作成する．
     */
    fun <P> create(id: P): T {
        return realm.use { realm ->
            realm.copyFromRealm(realm.createObject(modelClass, id))
        }
    }

    /**
     * オブジェクトを新規作成する．
     */
    fun create(): T {
        return realm.use { realm ->
            realm.copyFromRealm(realm.createObject(modelClass))
        }
    }

    /**
     * オブジェクトを保存する．
     */
    fun save(instance: T) {
        save(listOf(instance))
    }

    /**
     * 複数のオブジェクトを保存する．
     * トランザクション中でない場合は単発トランザクションを発行する．
     */
    fun save(instances: Iterable<T>) {
        return realm.use { realm ->
            if (realm.isInTransaction) {
                realm.copyToRealmOrUpdate(instances)
            } else {
                realm.executeTransaction {
                    realm.copyToRealmOrUpdate(instances)
                }
            }
        }
    }

    /**
     * オブジェクトを削除する．
     */
    fun remove(instance: T) {
        remove(listOf(instance))
    }

    /**
     * 複数のオブジェクトを削除する．
     * トランザクション中でない場合は単発トランザクションを発行する．
     */
    fun remove(instances: Iterable<T>) {
        return realm.use { realm ->
            val operation = {
                for (instance in instances) {
                    if (instance.isManaged) {
                        instance.deleteFromRealm()
                    } else {
                        realm.copyToRealmOrUpdate(instance).deleteFromRealm()
                    }
                }
            }
            if (realm.isInTransaction) {
                operation()
            } else {
                realm.executeTransaction {
                    operation()
                }
            }
        }
    }

    /**
     * 対象オブジェクト（standalone）のリストを返す．
     * RealmQuery から lambda でアレコレして最終的に MutableList<T> を返す．
     */
    fun findAll(lambda: (RealmQuery<T>) -> Collection<T>): MutableList<T> {
        var result: MutableList<T> = mutableListOf()
        realm.use { realm ->
            result = realm.copyFromRealm(lambda(realm.where(modelClass)))
        }
        return result
    }

    /**
     * findAll() の Java 向け実装．
     */
    fun findAll(lambda: RepositoryFindAllFunction<T>): MutableList<T> {
        return findAll { query ->
            lambda.invoke(query)
        }
    }

    /**
     * 対象オブジェクト（standalone）をひとつだけ返す．
     * RealmQuery から lambda でアレコレして最終的に T? を返す．
     */
    fun find(lambda: (RealmQuery<T>) -> T?): T? {
        var result: T? = null
        realm.use { realm ->
            result = lambda(realm.where(modelClass))
            result = result?.let { realm.copyFromRealm(it) }
        }
        return result
    }

    /**
     * find() の Java 向け実装．
     */
    fun find(lambda: RepositoryFindFunction<T>): T? {
        return find { query ->
            lambda.invoke(query)
        }
    }

    /**
     * Realmで自由にクエリ系操作をするラッパー．
     */
    fun query(lambda: (Realm) -> OrderedRealmCollection<T>): OrderedRealmCollection<T> {
        return realm.use { realm ->
            lambda(realm)
        }
    }

    /**
     * query() の Java 向け実装．
     */
    fun query(lambda: RepositoryQueryFunction<T>): OrderedRealmCollection<T> {
        return query { query ->
            lambda.invoke(query)
        }
    }

    /**
     * Realmで自由にトランザクション系操作をするラッパー．
     */
    fun transaction(lambda: (Realm) -> Unit) {
        realm.use { realm ->
            realm.executeTransaction {
                lambda(it)
            }
        }
    }

    /**
     * transaction() の Java 向け実装．
     */
    fun transaction(lambda: RepositoryTransactionFunction) {
        transaction { query -> lambda.invoke(query) }
    }

    interface RepositoryFindAllFunction<T> {
        fun invoke(query: RealmQuery<T>): Collection<T>
    }

    interface RepositoryFindFunction<T> {
        fun invoke(query: RealmQuery<T>): T?
    }

    interface RepositoryQueryFunction<T> {
        fun invoke(query: Realm): OrderedRealmCollection<T>
    }

    interface RepositoryTransactionFunction {
        fun invoke(query: Realm)
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
        fun <JT : RealmObject> create(classType: Class<JT>): Repository<JT> {
            return object : Repository<JT>() {
                override val modelClass = classType
            }
        }
    }
}
