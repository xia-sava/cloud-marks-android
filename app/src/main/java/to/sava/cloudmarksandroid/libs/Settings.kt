package to.sava.cloudmarksandroid.libs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.databases.models.MarkNode
import javax.inject.Inject


class Settings @Inject constructor(var context: Context) {

    val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    // アプリ設定

    var folderName: String
        get() {
            return pref.getString(context.getString(R.string.pref_key_app_folder_name), null)
                ?: context.getString(R.string.pref_default_app_folder_name)
        }
        set(value) {
            pref.edit {
                putString(context.getString(R.string.pref_key_app_folder_name), value)
            }
        }

    var lastSynced: Long
        get() {
            return pref.getLong(context.getString(R.string.pref_key_app_last_synced), 0L)
        }
        set(value) {
            pref.edit {
                putLong(context.getString(R.string.pref_key_app_last_synced), value)
            }
        }

    var lastBookmarkModify: Long
        get() {
            return pref.getLong(context.getString(R.string.pref_key_app_last_bookmark_modify), 0L)
        }
        set(value) {
            pref.edit {
                putLong(context.getString(R.string.pref_key_app_last_bookmark_modify), value)
            }
        }

    var lastOpenedMarkId: Long
        get() {
            return pref.getLong(
                context.getString(R.string.pref_key_app_last_opened_mark_id),
                MarkNode.ROOT_ID
            )
        }
        set(value) {
            pref.edit {
                putLong(context.getString(R.string.pref_key_app_last_opened_mark_id), value)
            }
        }

    // サービス設定

    var currentService: Services = Services.Gdrive

    var googleAccount: String
        get() {
            return pref.getString(context.getString(R.string.pref_key_google_drive_account), null)
                ?: ""
        }
        set(value) {
            pref.edit {
                putString(context.getString(R.string.pref_key_google_drive_account), value)
            }
        }

    val googleConnected: Boolean
        get() = googleAccount != ""
}
