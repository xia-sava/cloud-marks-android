package to.sava.cloudmarksandroid.activities

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceActivity
import android.support.v14.preference.PreferenceFragment
import android.support.v14.preference.SwitchPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.view.MenuItem
import com.crashlytics.android.Crashlytics
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import org.jetbrains.anko.*
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.libs.GoogleDriveStorage
import to.sava.cloudmarksandroid.libs.Settings
import java.io.IOException

class SettingsActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onIsMultiPane(): Boolean {
        return this.resources.configuration.screenLayout and
                Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class ApplicationPreferenceFragment : PreferenceFragment(),
            Preference.OnPreferenceChangeListener {

        override fun onCreatePreferences(bundle: Bundle?, s: String?) {
            addPreferencesFromResource(R.xml.pref_application)
            setHasOptionsMenu(true)

            val sharedPrefs = Settings().pref
            listOf(R.string.pref_key_app_folder_name, R.string.pref_key_app_autosync).forEach {id ->
                val pref = findPreference(getString(id))
                pref.onPreferenceChangeListener = this
                onPreferenceChange(pref, sharedPrefs.getString(pref.key, ""))
            }
        }

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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class GoogleDrivePreferenceFragment : PreferenceFragment(),
            Preference.OnPreferenceClickListener {

        private val storage: GoogleDriveStorage by lazy {
            GoogleDriveStorage(Settings())
        }
        private val connectionPref: SwitchPreference by lazy {
            findPreference(getString(R.string.pref_key_google_drive_connection)) as SwitchPreference
        }

        companion object {
            const val REQUEST_PICK_ACCOUNT = 1
            const val REQUEST_AUTHENTICATE = 2
            const val REQUEST_GMS_ERROR_DIALOG = 3
        }

        override fun onCreatePreferences(bundle: Bundle?, s: String?) {
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
                            Crashlytics.log("SettingsActivity.onPreferenceClick.startActivityForResult")
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
            Crashlytics.log("SettingsActivity.onActivityResult")
            super.onActivityResult(requestCode, resultCode, data)
            when (requestCode) {
                REQUEST_PICK_ACCOUNT -> {
                    Crashlytics.log("SettingsActivity.onActivityResult.REQUEST_PICK_ACCOUNT")
                    if (resultCode == Activity.RESULT_OK && data?.extras != null) {
                        val name = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                        Crashlytics.log("SettingsActivity.onActivityResult.REQUEST_PICK_ACCOUNT / name: '$name'")
                        if (name != null) {
                            storage.credential.selectedAccount = Account(name, "com.google")
                            tryAuthenticate()
                        }
                    } else {
                        toast(getString(R.string.cant_confirm_user_account))
                    }
                }
                REQUEST_AUTHENTICATE -> {
                    Crashlytics.log("SettingsActivity.onActivityResult.REQUEST_AUTHENTICATE")
                    if (resultCode == Activity.RESULT_OK) {
                        tryAuthenticate()
                    } else {
                        toast(getString(R.string.connecting_google_drive_denied))
                    }
                }
                else -> {
                    Crashlytics.log("SettingsActivity.onActivityResult.else")
                    throw RuntimeException("requestCode: $requestCode resultCode: $resultCode")
                }
            }
        }

        private fun tryAuthenticate() {
            val account = storage.credential.selectedAccount
            Crashlytics.log("SettingsActivity.tryAuthenticate / account:'${account.name}")
            doAsync {
                var doCheck = false
                try {
                    Crashlytics.log("SettingsActivity.tryAuthenticate.getToken")
                    GoogleAuthUtil.getToken(activity, account, GoogleDriveStorage.SCOPES_STR)
                    doCheck = true
                }
                catch (playEx: GooglePlayServicesAvailabilityException) {
                    // Google Play サービス自体が使えない？
                    Crashlytics.log("SettingsActivity.tryAuthenticate.GooglePlayServicesAvailabilityException")
                    GoogleApiAvailability.getInstance().getErrorDialog(
                            activity, playEx.connectionStatusCode, REQUEST_GMS_ERROR_DIALOG).show()
                }
                catch (userAuthEx: UserRecoverableAuthException) {
                    // ユーザの許可を得るためのダイアログを表示する
                    Crashlytics.log("SettingsActivity.tryAuthenticate.UserRecoverableAuthException")
                    startActivityForResult(userAuthEx.intent, REQUEST_AUTHENTICATE)
                }
                catch (transientEx: IOException) {
                    // ネットワークエラーとかか？
                    Crashlytics.log("SettingsActivity.tryAuthenticate.IOException")
                    uiThread { toast(transientEx.message ?: "") }
                }
                catch (authEx: GoogleAuthException) {
                    Crashlytics.log("SettingsActivity.tryAuthenticate.GoogleAuthException / message:'${authEx.message}")
                    // 本来は認証エラーだが，エラーなしでも Unknown でここに来る時がある
                    if (authEx.message == "Unknown") {
                        // そういう時はひとまず認証が通ったと思ってアクセスチェックをしてみる
                        doCheck = true
                    } else {
                        uiThread { toast(authEx.message ?: "") }
                    }
                }
                catch (ex: RuntimeException) {
                    // その他もう何だかわからないけどおかしい
                    uiThread {
                        toast(ex.message!!)
                    }
                    Crashlytics.logException(ex)
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
                    }
                    catch (userAuthIoEx: UserRecoverableAuthIOException) {
                        // ユーザの許可を得るためのダイアログを表示する
                        startActivityForResult(userAuthIoEx.intent, REQUEST_AUTHENTICATE)
                    }
                    catch (illArgEx: IllegalArgumentException) {
                        // 持っていた credential がなぜかおかしい
                        uiThread {
                            toast(illArgEx.message!!)
                        }
                    }
                    catch (ex: RuntimeException) {
                        // その他もう何だかわからないけどおかしい
                        uiThread {
                            toast(ex.message!!)
                        }
                        Crashlytics.logException(ex)
                    }
                }
            }
        }
    }
}
