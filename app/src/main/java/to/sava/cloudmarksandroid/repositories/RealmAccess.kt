package to.sava.cloudmarksandroid.repositories

import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmQuery
import java.util.*
import kotlin.reflect.KClass
import io.realm.Sort as RealmSort

enum class Sort(val code: RealmSort) {
    ASCENDING(RealmSort.ASCENDING),
    DESCENDING(RealmSort.DESCENDING)
}

class RealmAccess<T: RealmObject> {

    private val modelClass: Class<T>
    private val realmInstance: Realm?

    constructor(modelClass: Class<T>, realmInstance: Realm? = null) {
        this.modelClass = modelClass
        this.realmInstance = realmInstance
        this.copyFromRealmMaxDepth = Int.MAX_VALUE
    }

    constructor(modelClass: KClass<T>, realmInstance: Realm? = null)
            : this(modelClass.java, realmInstance)

    /**
     * copyFromRealm() の際の maxDepth デフォルト値
     */
    private var copyFromRealmMaxDepth: Int

    /**
     * 対象DBを全件取得する．
     */
    fun findAll(): List<T> {
        return realmListQuery { it }
    }

    /**
     * 対象DBで指定キーの値に一致するものを全件取得する．
     */
    fun findAll(key: String, value: String): List<T> {
        return realmListQuery { it.equalTo(key, value) }
    }
    fun findAll(key: String, value: Boolean): List<T> {
        return realmListQuery { it.equalTo(key, value) }
    }
    fun findAll(key: String, value: Int): List<T> {
        return realmListQuery { it.equalTo(key, value) }
    }
    fun findAll(key: String, value: Long): List<T> {
        return realmListQuery { it.equalTo(key, value) }
    }
    fun findAll(key: String, value: Double): List<T> {
        return realmListQuery { it.equalTo(key, value) }
    }
    fun findAll(key: String, value: Date): List<T> {
        return realmListQuery { it.equalTo(key, value) }
    }

    /**
     * 対象DBを，指定キーでソートされた状態で全件取得する．
     */
    fun findAll(sortKey: String, sortDir: Sort): List<T> {
        return realmSortedListQuery(sortKey, sortDir) { it }
    }

    /**
     * 対象DBで指定キーの値に一致するものをソートされた状態で全件取得する．
     */
    fun findAll(key: String, value: String, sortKey: String, sortDir: Sort): List<T> {
        return realmSortedListQuery(sortKey, sortDir) { it.equalTo(key, value) }
    }
    fun findAll(key: String, value: Boolean, sortKey: String, sortDir: Sort): List<T> {
        return realmSortedListQuery(sortKey, sortDir) { it.equalTo(key, value) }
    }
    fun findAll(key: String, value: Int, sortKey: String, sortDir: Sort): List<T> {
        return realmSortedListQuery(sortKey, sortDir) { it.equalTo(key, value) }
    }
    fun findAll(key: String, value: Long, sortKey: String, sortDir: Sort): List<T> {
        return realmSortedListQuery(sortKey, sortDir) { it.equalTo(key, value) }
    }
    fun findAll(key: String, value: Double, sortKey: String, sortDir: Sort): List<T> {
        return realmSortedListQuery(sortKey, sortDir) { it.equalTo(key, value) }
    }
    fun findAll(key: String, value: Date, sortKey: String, sortDir: Sort): List<T> {
        return realmSortedListQuery(sortKey, sortDir) { it.equalTo(key, value) }
    }

    /**
     * 対象DBで先頭のレコードを取得する．
     */
    fun find(): T? {
        return realmObjectQuery { it }
    }

    /**
     * 対象DBで指定キーの値に一致するものの先頭のレコードを取得する．
     */
    fun find(key: String, value: String): T? {
        return realmObjectQuery { it.equalTo(key, value) }
    }
    fun find(key: String, value: Boolean): T? {
        return realmObjectQuery { it.equalTo(key, value) }
    }
    fun find(key: String, value: Int): T? {
        return realmObjectQuery { it.equalTo(key, value) }
    }
    fun find(key: String, value: Long): T? {
        return realmObjectQuery { it.equalTo(key, value) }
    }
    fun find(key: String, value: Double): T? {
        return realmObjectQuery { it.equalTo(key, value) }
    }
    fun find(key: String, value: Date): T? {
        return realmObjectQuery { it.equalTo(key, value) }
    }

    /**
     * 対象DBを全件削除する．
     */
    fun deleteAll() {
        executeTransaction { realm ->
            realm.delete(modelClass)
        }
    }

    /**
     * レコードを削除する
     */
    fun delete(entity: T) {
        delete(listOf(entity))
    }

    /**
     * 複数のレコードを削除する
     */
    fun delete(entities: Collection<T>) {
        return executeTransaction {
            for (entity in entities) {
                if (entity.isManaged) {
                    entity.deleteFromRealm()
                } else {
                    save(entity).deleteFromRealm()
                }
            }
        }
    }

    /**
     * 新規レコードを作成する．
     */
    fun createEntity(id: String): T {
        return executeFetchObject {
            executeTransaction { it.createObject(modelClass, id) }
        } !!
    }
    fun createEntity(id: Int): T {
        return executeFetchObject {
            executeTransaction { it.createObject(modelClass, id) }
        } !!
    }
    fun createEntity(id: Long): T {
        return executeFetchObject {
            executeTransaction { it.createObject(modelClass, id) }
        } !!
    }
    fun createEntity(id: Double): T {
        return executeFetchObject {
            executeTransaction { it.createObject(modelClass, id) }
        } !!
    }
    fun createEntity(id: Date): T {
        return executeFetchObject {
            executeTransaction { it.createObject(modelClass, id) }
        } !!
    }

    /**
     * レコードを copyToRealm して managed にする．
     */
    fun save(entity: T): T {
        return save(listOf(entity))[0]
    }

    /**
     * 複数のレコードを copyToRealm して managed にする．
     */
    fun save(entities: Collection<T>): MutableList<T> {
        return executeTransaction { realm ->
             realm.copyToRealmOrUpdate(entities)
        }
    }

    /**
     * 対象DBのレコード数を取得する．
     * RealmQuery から func で条件設定等して最終的に Long を返す．
     */
    fun count(func: (RealmQuery<T>) -> RealmQuery<T>): Long {
        return executeProcedure { realm ->
            func(realm.where(modelClass)).count()
        }
    }

    /**
     * 対象DBのレコード数を取得する．
     */
    fun count(): Long {
        return count { it }
    }


    /**
     * Realm 新規インスタンスを作成する．
     */
    fun newRealmInstance(): Realm {
        return Realm.getDefaultInstance()
    }

    /**
     * Realm インスタンスを取得する．
     */
    fun getRealmInstance(): Realm {
        return realmInstance ?: newRealmInstance()
    }

    /**
     * managed モードの RealAccess<T> を取得する．
     * 普通は realmTransaction() で足りると思われる．
     */
    fun managed(realm: Realm? = null): RealmAccess<T> {
        return RealmAccess(modelClass, realm ?: newRealmInstance())
    }

    /**
     * copyFromRealm() の maxDepth パラメータをセットする．
     */
    fun withMaxDepth(depth: Int): RealmAccess<T> {
        val repos = RealmAccess(modelClass, realmInstance)
        repos.copyFromRealmMaxDepth = depth
        return repos
    }

    /**
     * 任意のトランザクションを実行する．
     * func には managed モードの RealmAccess<T> が渡され，
     * 処理は transaction スコープ内で実行される．
     */
    fun <R> realmTransaction(func: (realmAccess: RealmAccess<T>) -> R): R {
        return executeTransaction { realm ->
            func(managed(realm))
        }
    }

    /**
     * 複数オブジェクトを返す Realm クエリを実行する．
     * managed モードでなければ copyFromRealm() と close() を実行する．
     */
    fun realmListQuery(func: (query: RealmQuery<T>) -> RealmQuery<T>): List<T> {
        return executeFetchList { realm ->
            func(realm.where(modelClass)).findAll()
        }
    }

    /**
     * 複数オブジェクトをソートして返す Realm クエリを実行する．
     * managed モードでなければ copyFromRealm() と close() を実行する．
     *
     * この findAllSorted は Realm の後のバージョンで非推奨となる．
     * Realm アップデートに従い，この処理は不要になり realmListQuery() に統合可能になる．
     */
    fun realmSortedListQuery(sortKey: String, sortDir: Sort, func: (query: RealmQuery<T>) -> RealmQuery<T>): List<T> {
        return executeFetchList { realm ->
            func(realm.where(modelClass)).sort(sortKey, sortDir.code).findAll()
        }
    }

    /**
     * 単一オブジェクトを返す Realm クエリを実行する．
     * managed モードでなければ copyFromRealm() と close() を実行する．
     */
    fun realmObjectQuery(func: (query: RealmQuery<T>) -> RealmQuery<T>): T? {
        return executeFetchObject { realm ->
            func(realm.where(modelClass)).findFirst()
        }
    }


    /**
     * Realm スコープ内で任意の find() 処理を実行する．
     * managed モードでなければ copyFromRealm() と close() を実行する．
     */
    private fun executeFetchObject(func: (realm: Realm) -> T?): T? {
        return if (realmInstance != null) {
            func(realmInstance)
        } else {
            newRealmInstance().use { realm ->
                val entity = func(realm)
                entity?.let { realm.copyFromRealm(it, copyFromRealmMaxDepth) }
            }
        }
    }

    /**
     * Realm スコープ内で任意の findAll() 処理を実行する．
     * managed モードでなければ copyFromRealm() と close() を実行する．
     */
    private fun executeFetchList(func: (realm: Realm) -> List<T>): List<T> {
        return if (realmInstance != null) {
            func(realmInstance)
        } else {
            newRealmInstance().use { realm ->
                realm.copyFromRealm(func(realm), copyFromRealmMaxDepth)
            }
        }
    }

    /**
     * トランザクション内で任意の処理を実行する．
     * managed モードでなければ close() で Realm を閉じる．．
     */
    private fun <R> executeTransaction(func: (realm: Realm) -> R): R {
        return if (realmInstance != null) {
            inTransaction(realmInstance, func)
        } else {
            newRealmInstance().use { realm ->
                inTransaction(realm, func)
            }
        }
    }

    /**
     * Realm スコープ内で任意の処理を実行する．
     * managed モードでなければ close() で Realm を閉じる．．
     */
    private fun <R> executeProcedure(func: (realm: Realm) -> R): R {
        return if (realmInstance != null) {
            func(realmInstance)
        } else {
            newRealmInstance().use { realm ->
                func(realm)
            }
        }
    }

    private fun <R> inTransaction(realm: Realm, operation: (realm: Realm) -> R): R {
        val ret: R
        if (realm.isInTransaction) {
            ret = operation(realm)
        } else {
            realm.beginTransaction()
            ret = operation(realm)
            realm.commitTransaction()
        }
        return ret
    }

}
