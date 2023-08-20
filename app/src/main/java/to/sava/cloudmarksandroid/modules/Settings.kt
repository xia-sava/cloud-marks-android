package to.sava.cloudmarksandroid.modules

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
    val FOLDER_NAME = stringPreferencesKey("folder_name")
    val FOLDER_COLUMNS = intPreferencesKey("folder_columns")
    val LAST_SYNCED = longPreferencesKey("last_synced")
    val LAST_BOOKMARK_MODIFIED = longPreferencesKey("last_bookmark_modified")
    val LAST_OPENED_MARK_ID = longPreferencesKey("last_opened_mark_id")
    val MARK_READ_TO_HERE = longPreferencesKey("mark_read_to_here")
    val GOOGLE_DRIVE_ACCOUNT = stringPreferencesKey("google_drive_account")
}

abstract class BaseSettings(
    val context: Context,
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
    context: Context,
    dataStore: DataStore<Preferences>
) : BaseSettings(context, dataStore) {

    fun getFolderName() =
        getString(PreferenceKeys.FOLDER_NAME, "cloud_marks")

    suspend fun getFolderNameValue() =
        getStringValue(PreferenceKeys.FOLDER_NAME, "cloud_marks")

    suspend fun setFolderName(value: String) =
        setValue(PreferenceKeys.FOLDER_NAME, value)

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
    suspend fun getCurrentService() = Services.Gdrive

    suspend fun getGoogleAccount() =
        getStringValue(PreferenceKeys.GOOGLE_DRIVE_ACCOUNT, "")

    suspend fun setGoogleAccount(value: String) =
        setValue(PreferenceKeys.GOOGLE_DRIVE_ACCOUNT, value)

    fun isGoogleConnected() =
        getString(PreferenceKeys.GOOGLE_DRIVE_ACCOUNT, "")
            .map { it != "" }
}
