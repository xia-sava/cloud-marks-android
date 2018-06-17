package to.sava.cloudmarksandroid.libs

import android.preference.PreferenceManager
import to.sava.cloudmarksandroid.CloudMarksAndroidApplication
import to.sava.cloudmarksandroid.R


class Settings {
    val context = CloudMarksAndroidApplication.instance
    val pref = PreferenceManager.getDefaultSharedPreferences(CloudMarksAndroidApplication.instance)!!

    // アプリ設定

    var folderName: String
        get() {
            return pref.getString(context.getString(R.string.pref_key_app_folder_name),
                    context.getString(R.string.pref_default_app_folder_name))
        }
        set(value) {
            pref.edit()
                    .putString(context.getString(R.string.pref_key_app_folder_name), value)
                    .apply()
        }

    var lastSynced: Long
        get() {
            return pref.getLong(context.getString(R.string.pref_key_app_last_synced), 0L)
        }
        set(value) {
            pref.edit()
                    .putLong(context.getString(R.string.pref_key_app_last_synced), value)
                    .apply()
        }

    var lastBookmarkModify: Long
        get() {
            return pref.getLong(context.getString(R.string.pref_key_app_last_bookmark_modify), 0L)
        }
        set(value) {
            pref.edit()
                    .putLong(context.getString(R.string.pref_key_app_last_bookmark_modify), value)
                    .apply()
        }

    var lastOpenedMarkId: String
        get() {
            return pref.getString(context.getString(R.string.pref_key_app_last_opened_mark_id), "")
        }
        set(value) {
            pref.edit()
                    .putString(context.getString(R.string.pref_key_app_last_opened_mark_id), value)
                    .apply()
        }

    // サービス設定

    var currentService: Services = Services.Gdrive

    var googleAccount: String
        get() {
            return pref.getString(context.getString(R.string.pref_key_google_drive_account), "")
        }
        set(value) {
            pref.edit()
                    .putString(context.getString(R.string.pref_key_google_drive_account), value)
                    .apply()
        }

    var googleConnected: Boolean = false
        get() = googleAccount != ""
}
