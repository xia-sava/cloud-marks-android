package to.sava.cloudmarksandroid.modules

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import to.sava.cloudmarksandroid.databases.models.MarkNode
import java.io.IOException


object PreferenceKeys {
    val CURRENT_SERVICE = intPreferencesKey("current_service")
    val FOLDER_COLUMNS = intPreferencesKey("folder_columns")
    val LAST_SYNCED = longPreferencesKey("last_synced")
    val LAST_BOOKMARK_MODIFIED = longPreferencesKey("last_bookmark_modified")
    val LAST_OPENED_MARK_ID = longPreferencesKey("last_opened_mark_id")
    val MARK_READ_TO_HERE = longPreferencesKey("mark_read_to_here")
    val AWS_S3_ACCESS_KEY_ID = stringPreferencesKey("aws_s3_access_key_id")
    val AWS_S3_SECRET_ACCESS_KEY = stringPreferencesKey("aws_s3_secret_access_key")
    val AWS_S3_REGION = stringPreferencesKey("aws_s3_region")
    val AWS_S3_BUCKET_NAME = stringPreferencesKey("aws_s3_bucket_name")
    val AWS_S3_FOLDER_NAME = stringPreferencesKey("aws_s3_folder_name")
    val AWS_S3_CONNECTED = booleanPreferencesKey("aws_s3_connected")
}

abstract class BaseSettings(
    private val dataStore: DataStore<Preferences>
) {
    private val prefs: Flow<Preferences>
        get() = dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }

    protected fun getString(key: Preferences.Key<String>, default: String = ""): Flow<String> {
        return prefs.map { it[key] ?: default }
    }

    protected fun getInt(key: Preferences.Key<Int>, default: Int = 0): Flow<Int> {
        return prefs.map { it[key] ?: default }
    }

    protected fun getLong(key: Preferences.Key<Long>, default: Long = 0L): Flow<Long> {
        return prefs.map { it[key] ?: default }
    }

    protected fun getFloat(key: Preferences.Key<Float>, default: Float = 0f): Flow<Float> {
        return prefs.map { it[key] ?: default }
    }

    protected fun getDouble(key: Preferences.Key<Double>, default: Double = 0.0): Flow<Double> {
        return prefs.map { it[key] ?: default }
    }

    protected fun getBoolean(
        key: Preferences.Key<Boolean>,
        default: Boolean = false
    ): Flow<Boolean> {
        return prefs.map { it[key] ?: default }
    }

    protected suspend fun getSet(
        key: Preferences.Key<Set<String>>,
        default: Set<String> = setOf()
    ): Flow<Set<String>> {
        return prefs.map { it[key] ?: default }
    }

    protected suspend fun getStringValue(
        key: Preferences.Key<String>,
        default: String = ""
    ): String {
        return prefs.map { it[key] ?: default }.first()
    }

    protected suspend fun getIntValue(key: Preferences.Key<Int>, default: Int = 0): Int {
        return prefs.map { it[key] ?: default }.first()
    }

    protected suspend fun getLongValue(key: Preferences.Key<Long>, default: Long = 0L): Long {
        return prefs.map { it[key] ?: default }.first()
    }

    protected suspend fun getFloatValue(key: Preferences.Key<Float>, default: Float = 0f): Float {
        return prefs.map { it[key] ?: default }.first()
    }

    protected suspend fun getDoubleValue(
        key: Preferences.Key<Double>,
        default: Double = 0.0
    ): Double {
        return prefs.map { it[key] ?: default }.first()
    }

    protected suspend fun getBooleanValue(
        key: Preferences.Key<Boolean>,
        default: Boolean = false
    ): Boolean {
        return prefs.map { it[key] ?: default }.first()
    }

    protected suspend fun getSetValue(
        key: Preferences.Key<Set<String>>,
        default: Set<String> = setOf()
    ): Set<String> {
        return prefs.map { it[key] ?: default }.first()
    }

    protected suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        dataStore.edit {
            it[key] = value
        }
    }
}

class Settings(
    dataStore: DataStore<Preferences>
) : BaseSettings(dataStore) {

    fun getLastSynced() =
        getLong(PreferenceKeys.LAST_SYNCED)

    suspend fun getLastSyncedValue() =
        getLongValue(PreferenceKeys.LAST_SYNCED)

    suspend fun setLastSynced(value: Long) =
        setValue(PreferenceKeys.LAST_SYNCED, value)

    fun getLastBookmarkModified() =
        getLong(PreferenceKeys.LAST_BOOKMARK_MODIFIED)

    suspend fun getLastBookmarkModifiedValue() =
        getLongValue(PreferenceKeys.LAST_BOOKMARK_MODIFIED)

    suspend fun setLastBookmarkModified(value: Long) =
        setValue(PreferenceKeys.LAST_BOOKMARK_MODIFIED, value)

    fun getLastOpenedMarkId() =
        getLong(PreferenceKeys.LAST_OPENED_MARK_ID, MarkNode.ROOT_ID)

    suspend fun getLastOpenedMarkIdValue() =
        getLongValue(PreferenceKeys.LAST_OPENED_MARK_ID, MarkNode.ROOT_ID)

    suspend fun setLastOpenedMarkId(value: Long) =
        setValue(PreferenceKeys.LAST_OPENED_MARK_ID, value)

    fun getMarkReadToHere() =
        getLong(PreferenceKeys.MARK_READ_TO_HERE, MarkNode.ROOT_ID)

    suspend fun getMarkReadToHereValue() =
        getLongValue(PreferenceKeys.MARK_READ_TO_HERE, MarkNode.ROOT_ID)

    suspend fun setMarkReadToHere(value: Long) =
        setValue(PreferenceKeys.MARK_READ_TO_HERE, value)

    fun getFolderColumns() =
        getInt(PreferenceKeys.FOLDER_COLUMNS, 1)

    suspend fun getFolderColumnsValue() =
        getIntValue(PreferenceKeys.FOLDER_COLUMNS, 1)

    suspend fun setFolderColumns(value: Int) =
        setValue(PreferenceKeys.FOLDER_COLUMNS, value)

    // サービス設定
    suspend fun getCurrentService(): Int {
        val currentValue = getIntValue(PreferenceKeys.CURRENT_SERVICE, 0)
        return when (currentValue) {
            0, 1 -> 0
            else -> 0
        }
    }

    suspend fun setCurrentService(value: Int) =
        setValue(PreferenceKeys.CURRENT_SERVICE, value)


    suspend fun getAwsS3AccessKeyId() =
        getStringValue(PreferenceKeys.AWS_S3_ACCESS_KEY_ID, "")

    suspend fun setAwsS3AccessKeyId(value: String) =
        setValue(PreferenceKeys.AWS_S3_ACCESS_KEY_ID, value)

    suspend fun getAwsS3SecretAccessKey() =
        getStringValue(PreferenceKeys.AWS_S3_SECRET_ACCESS_KEY, "")

    suspend fun setAwsS3SecretAccessKey(value: String) =
        setValue(PreferenceKeys.AWS_S3_SECRET_ACCESS_KEY, value)

    suspend fun getAwsS3Region() =
        getStringValue(PreferenceKeys.AWS_S3_REGION, "")

    suspend fun setAwsS3Region(value: String) =
        setValue(PreferenceKeys.AWS_S3_REGION, value)

    suspend fun getAwsS3BucketName() =
        getStringValue(PreferenceKeys.AWS_S3_BUCKET_NAME, "")

    suspend fun setAwsS3BucketName(value: String) =
        setValue(PreferenceKeys.AWS_S3_BUCKET_NAME, value)

    suspend fun getAwsS3FolderName() =
        getStringValue(PreferenceKeys.AWS_S3_FOLDER_NAME, "")

    suspend fun setAwsS3FolderName(value: String) =
        setValue(PreferenceKeys.AWS_S3_FOLDER_NAME, value)

    suspend fun getAwsS3Connected() =
        getBooleanValue(PreferenceKeys.AWS_S3_CONNECTED, false)

    suspend fun setAwsS3Connected(value: Boolean) =
        setValue(PreferenceKeys.AWS_S3_CONNECTED, value)

    fun isAwsS3Connected() =
        getBoolean(PreferenceKeys.AWS_S3_CONNECTED, false)
            .map { it }
}
