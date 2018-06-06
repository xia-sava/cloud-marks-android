package to.sava.cloudmarksandroid.libs

import android.preference.PreferenceManager
import to.sava.cloudmarksandroid.CloudMarksAndroidApplication
import to.sava.cloudmarksandroid.R

class Settings {
    val context = CloudMarksAndroidApplication.instance
    private val pref = PreferenceManager.getDefaultSharedPreferences(CloudMarksAndroidApplication.instance)

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
