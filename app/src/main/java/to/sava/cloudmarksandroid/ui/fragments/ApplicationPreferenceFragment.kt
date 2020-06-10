package to.sava.cloudmarksandroid.ui.fragments

import android.content.Context
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import dagger.android.support.AndroidSupportInjection
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.libs.Settings
import javax.inject.Inject

/**
 * Application Settings フラグメント
 */
class ApplicationPreferenceFragment : SettingsFragment(),
    Preference.OnPreferenceChangeListener {

    @Inject
    internal lateinit var settings: Settings

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)

        super.onAttach(context)
    }

    /**
     * フラグメント初回生成処理
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(
            R.xml.settings, getString(
                R.string.pref_key_application
            )
        )
        setHasOptionsMenu(true)

        // onCreate時に初回のonPreferenceChange相当をコールしとく
        val sharedPrefs = settings.pref
        listOf(
            R.string.pref_key_app_folder_name,
            R.string.pref_key_app_autosync
        ).forEach { id ->
            findPreference<Preference>(getString(id))?.let {
                it.onPreferenceChangeListener = this
                onPreferenceChange(it, sharedPrefs.getString(it.key, ""))
            }
        }
    }

    /**
     * プリファレンスに何か変更があった時
     */
    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        when (preference) {
            is ListPreference -> {
                val index = preference.findIndexOfValue(newValue as String)
                preference.summary = if (index >= 0) preference.entries[index] else null
            }
            else -> {
                preference?.summary = newValue as String
            }
        }
        return true
    }
}