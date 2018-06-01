package to.sava.cloudmarksandroid.activities

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.view.MenuItem
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.libs.GoogleDriveStorage
import to.sava.cloudmarksandroid.libs.Settings
import java.io.IOException

/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 * See [Android Design: Settings](http://developer.android.com/design/patterns/settings.html)
 * for design guidelines and the [Settings API Guide](http://developer.android.com/guide/topics/ui/settings.html)
 * for more information on developing a Settings UI.
 */
class SettingsActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * {@inheritDoc}
     */
    override fun onIsMultiPane(): Boolean {
        return this.resources.configuration.screenLayout and
                Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
    }

    /**
     * {@inheritDoc}
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName
                || ApplicationPreferenceFragment::class.java.name == fragmentName
                || GoogleDrivePreferenceFragment::class.java.name == fragmentName
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class ApplicationPreferenceFragment : PreferenceFragment(), Preference.OnPreferenceClickListener {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_application)
            setHasOptionsMenu(true)

            findPreference(getString(R.string.pref_key_app_folder_name)).onPreferenceClickListener = this
            findPreference(getString(R.string.pref_key_app_autosync)).onPreferenceClickListener = this
        }

        override fun onPreferenceClick(preference: Preference): Boolean {
            val value = PreferenceManager
                    .getDefaultSharedPreferences(preference.context)
                    .getString(preference.key, "")
            when (preference) {
                is ListPreference -> {
                    val index = preference.findIndexOfValue(value)
                    preference.summary = if (index >= 0) preference.entries[index] else null
                }
                else -> {
                    preference.summary = value
                }
            }
            return true
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class GoogleDrivePreferenceFragment : PreferenceFragment(),
            Preference.OnPreferenceClickListener {

        private val storage: GoogleDriveStorage by lazy {
            GoogleDriveStorage(Settings(activity))
        }
        private val connectionPref: SwitchPreference by lazy {
            findPreference(getString(R.string.pref_key_google_drive_connection)) as SwitchPreference
        }

        companion object {
            const val REQUEST_PICK_ACCOUNT = 1
            const val REQUEST_AUTHENTICATE = 2
            const val REQUEST_GMS_ERROR_DIALOG = 3
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_google_drive)
            setHasOptionsMenu(true)

            connectionPref.onPreferenceClickListener = this
            val account = storage.settings.googleAccount
            connectionPref.summary = when (account) { "" -> "未接続" else -> account}
        }

        override fun onPreferenceClick(preference: Preference): Boolean {
            when (preference.key) {
                getString(R.string.pref_key_google_drive_connection) -> {
                    when (connectionPref.isChecked) {
                        true -> {
                            // 接続処理をする
                            toast(getString(R.string.connect_to_google_drive))
                            startActivityForResult(storage.credential.newChooseAccountIntent(), REQUEST_PICK_ACCOUNT)
                        }
                        false -> {
                            toast(getString(R.string.disconnect_from_google_drive))
                            storage.settings.googleAccount = ""
                            connectionPref.summary = "未接続"
                        }
                    }
                    connectionPref.isChecked = false
                    return true
                }
            }
            return false
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            when (requestCode) {
                REQUEST_PICK_ACCOUNT -> {
                    if (resultCode == Activity.RESULT_OK && data?.extras != null) {
                        val name = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                        if (name != null) {
                            storage.credential.selectedAccount = Account(name, "com.google")
                            tryAuthenticate()
                        }
                    } else {
                        toast(getString(R.string.cant_confirm_user_account))
                    }
                }
                REQUEST_AUTHENTICATE -> {
                    if (resultCode == Activity.RESULT_OK) {
                        tryAuthenticate()
                    } else {
                        toast(getString(R.string.connecting_google_drive_denied))
                    }
                }
            }
        }

        private fun tryAuthenticate() {
            val account = storage.credential.selectedAccount
            doAsync {
                var doCheck = false
                try {
                    GoogleAuthUtil.getToken(activity, account, GoogleDriveStorage.SCOPES_STR)
                    doCheck = true
                } catch (playEx: GooglePlayServicesAvailabilityException) {
                    // Google Play サービス自体が使えない？
                    GoogleApiAvailability.getInstance().getErrorDialog(
                            activity, playEx.connectionStatusCode, REQUEST_GMS_ERROR_DIALOG)
                } catch (userAuthEx: UserRecoverableAuthException) {
                    // ユーザの許可を得るためのダイアログを表示する
                    startActivityForResult(userAuthEx.intent, REQUEST_AUTHENTICATE)
                } catch (transientEx: IOException) {
                    // ネットワークエラーとかか？
                    uiThread { toast(transientEx.message ?: "") }
                } catch (authEx: GoogleAuthException) {
                    // 本来は認証エラーだが，エラーなしでも Unknown でここに来る時がある
                    if (authEx.message == "Unknown") {
                        // そういう時はひとまず認証が通ったと思ってアクセスチェックをしてみる
                        doCheck = true
                    } else {
                        uiThread { toast(authEx.message ?: "") }
                    }
                }
                if (doCheck) {
                    try {
                        val accessOk = storage.checkAccessibility()
                        uiThread {
                            if (accessOk) {
                                storage.settings.googleAccount = storage.credential.selectedAccountName
                                connectionPref.isChecked = true
                                connectionPref.summary = storage.settings.googleAccount
                                toast(getString(R.string.connected_to_google_drive))
                            } else {
                                toast(getString(R.string.error_occurred_on_connecting))
                            }
                        }
                    } catch (userAuthIoEx: UserRecoverableAuthIOException) {
                        // ユーザの許可を得るためのダイアログを表示する
                        startActivityForResult(userAuthIoEx.intent, REQUEST_AUTHENTICATE)
                    } catch (illArgEx: IllegalArgumentException) {
                        // 持っていた credential がなぜかおかしい
                        uiThread {
                            toast(illArgEx.message!!)
                        }
                    } catch (ex: RuntimeException) {
                        // その他もう何だかわからないけどおかしい
                        uiThread {
                            toast(ex.message!!)
                        }
                    }
                }
            }
        }
    }
}
