package to.sava.cloudmarksandroid.repositories

import io.realm.*
import java.util.*

/**
 * Realm ヘルパーの実装．
 *
 * 基本的には Realm で出来ることをラップしているだけ．
 * 処理ごとに Realm を close する．
 */
open class RealmHelper {
    companion object {
        /**
         * Realm インスタンスを生成する．
         */
        @JvmStatic
        fun getNewRealmInstance(): Realm {
            return Realm.getDefaultInstance()
        }

        /**
         * execute() を単発処理で行なう．
         */
        @JvmStatic
        fun execute(func: (Realm) -> Unit) {
            RealmInstanceHelper().use {
                it.execute(func)
            }
        }

        /**
         * singleExecute() の Java interface 向け実装．
         */
        @JvmStatic
        fun execute(func: Execute) {
            execute(func::execute)
        }

        /**
         * transaction() を単発処理で行なう．
         */
        @JvmStatic
        fun transaction(func: (Realm) -> Unit) {
            RealmInstanceHelper().use {
                it.transaction(func)
            }
        }

        /**
         * singleTransaction() の Realm Transaction を使える実装．
         */
        @JvmStatic
        fun transaction(func: Realm.Transaction) {
            RealmInstanceHelper().use {
                it.transaction(func)
            }
        }
    }

    interface Execute {
        fun execute(realm: Realm)
    }
}

/**
 * Realm インスタンスヘルパーの実装．
 *
 * 基本的には Realm で出来ることをラップしているだけ．
 * 最後に close() されるよう，use {} で使うこと．
 */
open class RealmInstanceHelper : AutoCloseable {

    constructor(realm: Realm? = null) {
        this.realm = realm ?: RealmHelper.getNewRealmInstance()
    }

    /**
     * Realm インスタンス．
     */
    val realm: Realm

    /**
     * Realmで自由にリード系操作をするラッパー．
     */
    open fun execute(func: (Realm) -> Unit) {
        func(realm)
    }

    /**
     * execute() の Java interface 向け実装．
     */
    open fun execute(func: RealmHelper.Execute) {
        func.execute(realm)
    }

    /**
     * Realmで自由にトランザクション系操作をするラッパー．
     */
    open fun transaction(func: (Realm) -> Unit) {
        realm.executeTransaction {
            func(it)
        }
    }

    /**
     * transaction() の Realm Transaction を使える実装．
     */
    open fun transaction(func: Realm.Transaction) {
        realm.executeTransaction(func)
    }

    /**
     * AutoCloseable 実装．
     */
    override fun close() {
        realm.close()
    }
}


/**
 * Realm リポジトリクラスの抽象．
 *
 * ManagedRepository / Repository のベースで，共通処理はここへ．
 */
abstract class AbstractRepository<T : RealmObject>(realm: Realm? = null): AutoCloseable {

    /**
     * リポジトリがターゲットとする RealmObject のクラス
     */
    abstract val modelClass: Class<T>

    /**
     * インスタンスが使用する Realm オブジェクト
     */
    open val realm: Realm = realm ?: RealmHelper.getNewRealmInstance()

    /**
     * キー指定してオブジェクトを新規作成する．
     */
    open fun <P> create(id: P): T {
        return realm.createObject(modelClass, id)
    }

    /**
     * String のキー指定してオブジェクトを新規作成する．
     */
    open fun create(id: String): T {
        return realm.createObject(modelClass, id)
    }

    /**
     * String のキー指定してオブジェクトを新規作成する．
     */
    open fun create(id: Boolean): T {
        return realm.createObject(modelClass, id)
    }

    /**
     * Int のキー指定してオブジェクトを新規作成する．
     */
    open fun create(id: Int): T {
        return realm.createObject(modelClass, id)
    }

    /**
     * Long のキー指定してオブジェクトを新規作成する．
     */
    open fun create(id: Long): T {
        return realm.createObject(modelClass, id)
    }

    /**
     * Double のキー指定してオブジェクトを新規作成する．
     */
    open fun create(id: Double): T {
        return realm.createObject(modelClass, id)
    }

    /**
     * Date のキー指定してオブジェクトを新規作成する．
     */
    open fun create(id: Date): T {
        return realm.createObject(modelClass, id)
    }

    /**
     * オブジェクトを新規作成する．
     */
    open fun create(): T {
        return realm.createObject(modelClass)
    }

    /**
     * standalone オブジェクトを realm 管理下に置く．
     * 既に managed なオブジェクトには手を付けない．
     */
    open fun manage(standalone: T): T? {
        return manage(listOf(standalone))[0]
    }

    /**
     * standalone オブジェクトを realm 管理下に置く．
     * 既に managed なオブジェクトには手を付けない．
     */
    open fun manage(standalones: Collection<T>): MutableList<T> {
        val managed = mutableListOf<T>()
        if (realm.isInTransaction) {
            managed.addAll(realm.copyToRealmOrUpdate(standalones))
        } else {

            realm.executeTransaction {
                managed.addAll(realm.copyToRealmOrUpdate(standalones))
            }
        }
        return managed
    }

    /**
     * standalone オブジェクトを削除する．
     */
    open fun remove(instance: T) {
        remove(listOf(instance))
    }

    /**
     * 複数の standalone オブジェクトを削除する．
     * トランザクション中でない場合は単発トランザクションを発行する．
     */
    open fun remove(instances: Collection<T>) {
        if (realm.isInTransaction) {
            manage(instances).forEach { it.deleteFromRealm() }
        } else {
            realm.executeTransaction {
                manage(instances).forEach { it.deleteFromRealm() }
            }
        }
    }

    /**
     * 対象オブジェクトのリストを返す．
     * RealmQuery から func で条件設定等して最終的に MutableList<T> を返す．
     */
    open fun findAll(func: (RealmQuery<T>) -> RealmQuery<T>): MutableList<T> {
        return func(realm.where(modelClass)).findAll()
    }

    /**
     * findAll() の Java interface 向け実装．
     */
    open fun findAll(func: Repository.FindAll<T>): MutableList<T> {
        return findAll(func::execute)
    }

    /**
     * findAll() の条件なし版．
     */
    open fun findAll(): MutableList<T> {
        return findAll { it }
    }

    /**
     * 対象オブジェクトをひとつだけ返す．
     * RealmQuery から func で条件設定等して最終的に T? を返す．
     */
    open fun find(func: (RealmQuery<T>) -> RealmQuery<T>): T? {
        return func(realm.where(modelClass)).findFirst()
    }

    /**
     * find(func) の Java interface 向け実装．
     */
    open fun find(func: Repository.Find<T>): T? {
        return find(func::execute)
    }

    /**
     * String で equalTo() した対象オブジェクトをひとつだけ返す．
     */
    open fun find(key: String, value: String): T? {
        return find { it.equalTo(key, value) }
    }

    /**
     * Boolean で equalTo() した対象オブジェクトをひとつだけ返す．
     */
    open fun find(key: String, value: Boolean): T? {
        return find { it.equalTo(key, value) }
    }

    /**
     * Int で equalTo() した対象オブジェクトをひとつだけ返す．
     */
    open fun find(key: String, value: Int): T? {
        return find { it.equalTo(key, value) }
    }

    /**
     * Long で equalTo() した対象オブジェクトをひとつだけ返す．
     */
    open fun find(key: String, value: Long): T? {
        return find { it.equalTo(key, value) }
    }

    /**
     * Double で equalTo() した対象オブジェクトをひとつだけ返す．
     */
    open fun find(key: String, value: Double): T? {
        return find { it.equalTo(key, value) }
    }

    /**
     * Date で equalTo() した対象オブジェクトをひとつだけ返す．
     */
    open fun find(key: String, value: Date): T? {
        return find { it.equalTo(key, value) }
    }

    /**
     * 対象オブジェクトをひとつだけ返す．
     */
    open fun findFirst(): T? {
        return find { it }
    }

    /**
     * 対象オブジェクト（standalone）のカウントを返す．
     * RealmQuery から func で条件設定等して最終的に Long を返す．
     */
    open fun count(func: (RealmQuery<T>) -> RealmQuery<T>): Long {
        return func(realm.where(modelClass)).count()
    }

    /**
     * count() の Java interface 向け実装．
     */
    open fun count(func: Repository.Count<T>): Long {
        return count(func::execute)
    }

    /**
     * count() の条件なし版．
     */
    open fun count(): Long {
        return count { it }
    }

    /**
     * Realmで自由にクエリ系操作をして MutableList<T> を返す．
     */
    open fun query(func: (RealmQuery<T>) -> RealmResults<T>): MutableList<T> {
        return func(realm.where(modelClass))
    }

    /**
     * query() の Java interface 向け実装．
     */
    open fun query(func: Repository.Query<T>): MutableList<T> {
        return query(func::execute)
    }

    /**
     * AutoCloseable 実装．
     */
    override fun close() {
        realm.close()
    }
}

/**
 * Realm リポジトリクラスの抽象．
 *
 * 基本的には継承クラスで modelClass に RealmObject クラスを初期値としたリポジトリクラスを作成する．
 * 基本機能以外いらなければ直接に Repository<T>.create() で生成できる．
 *
 * Realm インスタンスは毎回の操作ごとに close() するので，
 * オブジェクトは standalone が基本となり，毎回 copyFrom や copyTo が発生する．
 * 重いのが困る時は素直に RealmInstanceHelper で作業してください．
 */
abstract class ManagedRepository<T : RealmObject>(realm: Realm? = null): AbstractRepository<T>(realm), AutoCloseable {

    companion object {
        /**
         * リポジトリ即席生成メソッド．
         *   val repo = Repository.create<ModelClass>()
         * のように使用する．
         * リポジトリとして特に専用処理が必要なければ基本機能はこれで使える．
         */
        inline fun <reified RT : RealmObject> create(realm: Realm): ManagedRepository<RT> {
            return object : ManagedRepository<RT>(realm) {
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
        fun <JT : RealmObject> create(classType: Class<JT>, realm: Realm): ManagedRepository<JT> {
            return object : ManagedRepository<JT>(realm) {
                override val modelClass = classType
            }
        }
    }

    /**
     * 対象オブジェクトのリストを返す．
     * RealmQuery から func で条件設定等して最終的に RealmResults<T> を返す．
     */
    override fun findAll(func: (RealmQuery<T>) -> RealmQuery<T>): RealmResults<T> {
        return super.findAll(func) as RealmResults<T>
    }

    /**
     * findAll() の Java interface 向け実装．
     */
    override fun findAll(func: Repository.FindAll<T>): RealmResults<T> {
        return findAll(func::execute)
    }

    /**
     * findAll() の条件なし版．
     */
    override fun findAll(): RealmResults<T> {
        return findAll { it }
    }

    /**
     * Realmで自由にクエリ系操作をして RealmResults<T> を返す．
     */
    override fun query(func: (RealmQuery<T>) -> RealmResults<T>): RealmResults<T> {
        return super.query(func) as RealmResults<T>
    }

    /**
     * query() の Java interface 向け実装．
     */
    override fun query(func: Repository.Query<T>): RealmResults<T> {
        return query(func::execute)
    }
}

/**
 * Realm リポジトリクラスの抽象．
 *
 * 基本的には継承クラスで modelClass に RealmObject クラスを初期値としたリポジトリクラスを作成する．
 * 基本機能以外いらなければ直接に Repository<T>.create() で生成できる．
 *
 * Realm インスタンスは毎回の操作ごとに close() するので，
 * オブジェクトは standalone が基本となり，毎回 copyFrom や copyTo が発生する．
 * 重いのが困る時は素直に RealmInstanceHelper で作業してください．
 */
abstract class Repository<T : RealmObject>: AbstractRepository<T>() {

    /**
     * copyFromRealm() の際の maxDepth．
     */
    private var copyFromRealmMaxDepth: Int = Int.MAX_VALUE

    /**
     * Realm インスタンスは単発ごとに取り直す．
     */
    override val realm: Realm
        get() = RealmHelper.getNewRealmInstance()

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

    /**
     * maxDepth をセットする．
     */
    open fun setMaxDepth(maxDepth: Int): Repository<T> {
        copyFromRealmMaxDepth = maxDepth
        return this
    }

    /**
     * 処理委譲のための ManagedRepository を作る処理．
     */
    open fun repos(realm: Realm): ManagedRepository<T>
            = ManagedRepository.create(modelClass, realm)

    /**
     * キー指定してオブジェクトを新規作成する．
     */
    override fun <P> create(id: P): T {
        realm.use { realm ->
            return realm.copyFromRealm(repos(realm).create(id), copyFromRealmMaxDepth)
        }
    }

    /**
     * String のキー指定してオブジェクトを新規作成する．
     */
    override fun create(id: String): T {
        realm.use { realm ->
            return realm.copyFromRealm(repos(realm).create(id), copyFromRealmMaxDepth)
        }
    }

    /**
     * String のキー指定してオブジェクトを新規作成する．
     */
    override fun create(id: Boolean): T {
        realm.use { realm ->
            return realm.copyFromRealm(repos(realm).create(id), copyFromRealmMaxDepth)
        }
    }

    /**
     * Int のキー指定してオブジェクトを新規作成する．
     */
    override fun create(id: Int): T {
        realm.use { realm ->
            return realm.copyFromRealm(repos(realm).create(id), copyFromRealmMaxDepth)
        }
    }

    /**
     * Long のキー指定してオブジェクトを新規作成する．
     */
    override fun create(id: Long): T {
        realm.use { realm ->
            return realm.copyFromRealm(repos(realm).create(id), copyFromRealmMaxDepth)
        }
    }

    /**
     * Double のキー指定してオブジェクトを新規作成する．
     */
    override fun create(id: Double): T {
        realm.use { realm ->
            return realm.copyFromRealm(repos(realm).create(id), copyFromRealmMaxDepth)
        }
    }

    /**
     * Date のキー指定してオブジェクトを新規作成する．
     */
    override fun create(id: Date): T {
        realm.use { realm ->
            return realm.copyFromRealm(repos(realm).create(id), copyFromRealmMaxDepth)
        }
    }

    /**
     * オブジェクトを新規作成する．
     */
    override fun create(): T {
        realm.use { realm ->
            return realm.copyFromRealm(repos(realm).create(), copyFromRealmMaxDepth)
        }
    }

    /**
     * オブジェクトを保存する．
     */
    open fun save(instance: T) {
        save(listOf(instance))
    }

    /**
     * 複数のオブジェクトを保存する．
     * トランザクション中でない場合は単発トランザクションを発行する．
     */
    open fun save(instances: Collection<T>) {
        realm.use { realm ->
            repos(realm).manage(instances)
        }
    }

    /**
     * 複数のオブジェクトを削除する．
     * トランザクション中でない場合は単発トランザクションを発行する．
     */
    override fun remove(instances: Collection<T>) {
        realm.use { realm ->
            repos(realm).remove(instances)
        }
    }

    /**
     * 対象オブジェクトのリストを返す．
     * RealmQuery から func で条件設定等して最終的に MutableList<T> を返す．
     */
    override fun findAll(func: (RealmQuery<T>) -> RealmQuery<T>): MutableList<T> {
        realm.use { realm ->
            return realm.copyFromRealm(repos(realm).findAll(func), copyFromRealmMaxDepth)
        }
    }

    /**
     * 対象オブジェクトをひとつだけ返す．
     * RealmQuery から func で条件設定等して最終的に T? を返す．
     */
    override fun find(func: (RealmQuery<T>) -> RealmQuery<T>): T? {
        realm.use { realm ->
            return repos(realm).find(func)?.let { realm.copyFromRealm(it, copyFromRealmMaxDepth) }
        }
    }

    /**
     * 対象オブジェクト（standalone）のカウントを返す．
     * RealmQuery から func で条件設定等して最終的に MutableList<T> を返す．
     */
    override fun count(func: (RealmQuery<T>) -> RealmQuery<T>): Long {
        realm.use { realm ->
            return repos(realm).count(func)
        }
    }

    /**
     * Realmで自由にクエリ系操作をするラッパー．
     */
    override fun query(func: (RealmQuery<T>) -> RealmResults<T>): MutableList<T> {
        realm.use { realm ->
            return realm.copyFromRealm(repos(realm).query(func), copyFromRealmMaxDepth)
        }
    }

    /**
     * findAll() の Java interface．
     */
    interface FindAll<T : RealmModel> {
        fun execute(query: RealmQuery<T>): RealmQuery<T>
    }

    /**
     * find() の Java interface．
     */
    interface Find<T : RealmModel> {
        fun execute(query: RealmQuery<T>): RealmQuery<T>
    }

    /**
     * count() の Java interface．
     */
    interface Count<T : RealmModel> {
        fun execute(query: RealmQuery<T>): RealmQuery<T>
    }

    /**
     * query() の Java interface．
     */
    interface Query<T : RealmModel> {
        fun execute(query: RealmQuery<T>): RealmResults<T>
    }
}
