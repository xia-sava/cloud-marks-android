package to.sava.cloudmarksandroid.modules

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import to.sava.cloudmarksandroid.databases.models.MarkNode
import java.io.IOException


object PreferenceKeys {
    val FOLDER_NAME = stringPreferencesKey("folder_name")
    val LAST_SYNCED = longPreferencesKey("last_synced")
    val LAST_BOOKMARK_MODIFIED = longPreferencesKey("last_bookmark_modified")
    val LAST_OPENED_MARK_ID = longPreferencesKey("last_opened_mark_id")
    val GOOGLE_DRIVE_ACCOUNT = stringPreferencesKey("google_drive_account")
}

class Settings(
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

    private suspend fun getString(key: Preferences.Key<String>, default: String = ""): String {
        return prefs.map { it[key] ?: default }.first()
    }

    private suspend fun getInt(key: Preferences.Key<Int>, default: Int = 0): Int {
        return prefs.map { it[key] ?: default }.first()
    }

    private suspend fun getLong(key: Preferences.Key<Long>, default: Long = 0L): Long {
        return prefs.map { it[key] ?: default }.first()
    }

    private suspend fun getFloat(key: Preferences.Key<Float>, default: Float = 0f): Float {
        return prefs.map { it[key] ?: default }.first()
    }

    private suspend fun getDouble(key: Preferences.Key<Double>, default: Double = 0.0): Double {
        return prefs.map { it[key] ?: default }.first()
    }

    private suspend fun getBoolean(
        key: Preferences.Key<Boolean>,
        default: Boolean = false
    ): Boolean {
        return prefs.map { it[key] ?: default }.first()
    }

    private suspend fun getSet(
        key: Preferences.Key<Set<String>>,
        default: Set<String> = setOf()
    ): Set<String> {
        return prefs.map { it[key] ?: default }.first()
    }

    private suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        dataStore.edit {
            it[key] = value
        }
    }

    suspend fun getFolderName() =
        getString(PreferenceKeys.FOLDER_NAME, "cloud_marks")

    suspend fun setFolderName(value: String) =
        setValue(PreferenceKeys.FOLDER_NAME, value)

    suspend fun getLastSynced() =
        getLong(PreferenceKeys.LAST_SYNCED)

    suspend fun setLastSynced(value: Long) =
        setValue(PreferenceKeys.LAST_SYNCED, value)

    suspend fun getLastBookmarkModified() =
        getLong(PreferenceKeys.LAST_BOOKMARK_MODIFIED)

    suspend fun setLastBookmarkModified(value: Long) =
        setValue(PreferenceKeys.LAST_BOOKMARK_MODIFIED, value)

    suspend fun getLastOpenedMarkId() =
        getLong(PreferenceKeys.LAST_OPENED_MARK_ID, MarkNode.ROOT_ID)

    suspend fun setLastOpenedMarkId(value: Long) =
        setValue(PreferenceKeys.LAST_OPENED_MARK_ID, value)


    // サービス設定
    suspend fun getCurrentService() = Services.Gdrive

    suspend fun getGoogleAccount() =
        getString(PreferenceKeys.GOOGLE_DRIVE_ACCOUNT, "")

    suspend fun setGoogleAccount(value: String) =
        setValue(PreferenceKeys.GOOGLE_DRIVE_ACCOUNT, value)

    suspend fun googleConnected() = getGoogleAccount() != ""
}
