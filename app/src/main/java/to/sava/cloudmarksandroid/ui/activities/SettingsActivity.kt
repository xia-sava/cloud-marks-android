package to.sava.cloudmarksandroid.ui.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.preference.*
import kotlinx.android.synthetic.main.activity_main.*
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.libs.Settings
import to.sava.cloudmarksandroid.ui.fragments.ApplicationPreferenceFragment
import to.sava.cloudmarksandroid.ui.fragments.GoogleDrivePreferenceFragment
import to.sava.cloudmarksandroid.ui.fragments.SettingsFragment
import java.security.InvalidParameterException
import javax.inject.Inject

class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    @Inject
    lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.commit {
            replace(R.id.settings_fragment, SettingsFragment())
        }
    }

    /**
     * バックボタンの処理．
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * フラグメント生成処理．
     * フラグメント名に応じてクラス生成してやる．
     * バックボタンで前のフラグメントに戻る処理もやる．
     */
    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val fragment = when (pref.fragment) {
            SettingsFragment::class.java.name -> SettingsFragment()
            ApplicationPreferenceFragment::class.java.name -> ApplicationPreferenceFragment()
            GoogleDrivePreferenceFragment::class.java.name -> GoogleDrivePreferenceFragment()
            else -> throw InvalidParameterException("Invalid fragment: ${pref.fragment}")
        }
        supportFragmentManager.commit {
            replace(R.id.settings_fragment, fragment)
            addToBackStack(null)
        }
        return true
    }
}
