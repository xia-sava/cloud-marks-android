package to.sava.cloudmarksandroid.libs

import android.content.Context
import android.preference.PreferenceManager

class Settings(context: Context) {
    private val pref = PreferenceManager.getDefaultSharedPreferences(context)
}
