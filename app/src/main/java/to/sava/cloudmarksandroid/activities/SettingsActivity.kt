package to.sava.cloudmarksandroid.activities

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v14.preference.SwitchPreference
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.MenuItem
import com.crashlytics.android.Crashlytics
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.libs.GoogleDriveStorage
import to.sava.cloudmarksandroid.libs.Settings
import java.io.IOException
import java.lang.Exception
import java.security.InvalidParameterException


class SettingsActivity : AppCompatActivity(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, SettingsFragment())
                    .commit()
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
    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat?, pref: Preference?): Boolean {
        val fragment = when (pref?.fragment) {
            SettingsFragment::class.java.name -> SettingsFragment()
            ApplicationPreferenceFragment::class.java.name -> ApplicationPreferenceFragment()
            GoogleDrivePreferenceFragment::class.java.name -> GoogleDrivePreferenceFragment()
            else -> throw InvalidParameterException("Invalid fragment: ${pref?.fragment}")
        }
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        return true
    }


    /**
     * プリファレンスフラグメントの汎用のやつ
     */
    open class SettingsFragment : PreferenceFragmentCompat() {
        /**
         * プリファレンスが生成された時の処理．
         * キーに応じた画面設定を読み込む．
         * 画面ごとの個別設定なんかもここでやれる．
         */
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, rootKey)
        }

        fun toast(message: CharSequence) {
            activity?.toast(message)
        }
        fun toast(message: Int) {
            activity?.toast(message)
        }
    }

    /**
     * Application Settings フラグメント
     */
    class ApplicationPreferenceFragment : SettingsFragment(),
            Preference.OnPreferenceChangeListener {

        /**
         * フラグメント初回生成処理
         */
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, getString(R.string.pref_key_application))
            setHasOptionsMenu(true)

            // onCreate時に初回のonPreferenceChange相当をコールしとく
            val sharedPrefs = Settings().pref
            listOf(R.string.pref_key_app_folder_name, R.string.pref_key_app_autosync).forEach { id ->
                val pref = findPreference(getString(id))
                pref.onPreferenceChangeListener = this
                onPreferenceChange(pref, sharedPrefs.getString(pref.key, ""))
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

    /**
     * Google Drive Settings フラグメント
     */
    class GoogleDrivePreferenceFragment : SettingsFragment(),
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
            const val REQUEST_GRANT_PERMISSION = 4
        }

        /**
         * フラグメント初回生成処理
         */
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, getString(R.string.pref_key_google_drive))
            setHasOptionsMenu(true)

            connectionPref.onPreferenceClickListener = this
            val account = storage.settings.googleAccount
            connectionPref.summary = when (account) {
                "" -> "未接続"
                else -> account
            }
        }

        /**
         * プリファレンスに何か変更があった時
         * Switch が変化する前に処理を走らせるので Change でなく Click の方．
         */
        override fun onPreferenceClick(preference: Preference): Boolean {
            when (preference.key) {
                getString(R.string.pref_key_google_drive_connection) -> {
                    when (connectionPref.isChecked) {
                        true -> {
                            // 接続処理をする
                            if (checkPermission()) {
                                toast(R.string.connect_to_google_drive)
                                Crashlytics.log("SettingsActivity.onPreferenceClick.startActivityForResult")
                                startActivityForResult(storage.credential.newChooseAccountIntent(), REQUEST_PICK_ACCOUNT)
                            }
                        }
                        false -> {
                            toast(R.string.disconnect_from_google_drive)
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

        /**
         * GET_ACCOUNT 権限チェックをして，権限がなければ requestPermission() する．
         */
        private fun checkPermission(): Boolean {
            val context = storage.settings.context
            val perm = Manifest.permission.GET_ACCOUNTS
            if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
                return true
            }
            // onRequestPermissionsResult は activity 側に飛ぶ
            requestPermissions(arrayOf(perm), REQUEST_GRANT_PERMISSION)
            return false
        }

        /**
         * requestPermissions の戻り先
         */
        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
            if (requestCode == REQUEST_GRANT_PERMISSION) {
                for ((i, perm) in permissions.withIndex()) {
                    val granted = grantResults[i]
                    when (perm) {
                        Manifest.permission.GET_ACCOUNTS -> {
                            if (granted != PackageManager.PERMISSION_GRANTED) {
                                toast(R.string.require_get_account_permission)
                            } else {
                                toast(R.string.granted_get_account_permission)
                            }
                        }
                    }
                }
            }
        }

        /**
         * Google アカウントピックダイアログからの戻り先．
         */
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
                            tryAuthenticate(name)
                        }
                    } else {
                        toast(R.string.cant_confirm_user_account)
                    }
                }
                REQUEST_AUTHENTICATE -> {
                    Crashlytics.log("SettingsActivity.onActivityResult.REQUEST_AUTHENTICATE")
                    if (resultCode == Activity.RESULT_OK) {
                        tryAuthenticate(storage.settings.googleAccount)
                    } else {
                        toast(R.string.connecting_google_drive_denied)
                    }
                }
                else -> {
                    Crashlytics.log("SettingsActivity.onActivityResult.else")
                    throw RuntimeException("requestCode: $requestCode resultCode: $resultCode")
                }
            }
        }

        /**
         * Google Drive アクセスチェック．
         */
        private fun tryAuthenticate(name: String) {
            doAsync {
                try {
                    Crashlytics.log("SettingsActivity.tryAuthenticate.checkAccessibility")
                    storage.credential.selectedAccountName = name
                    val accessOk = storage.checkAccessibility()
                    uiThread {
                        if (accessOk) {
                            storage.settings.googleAccount = storage.credential.selectedAccountName
                            connectionPref.isChecked = true
                            connectionPref.summary = storage.settings.googleAccount
                            toast(R.string.connected_to_google_drive)
                        } else {
                            toast(R.string.error_occurred_on_connecting)
                        }
                    }
                } catch (ex: Exception) {
                    when (ex) {
                        is GooglePlayServicesAvailabilityIOException -> {
                            // Google Play サービス自体が使えない？
                            Crashlytics.log("SettingsActivity.tryAuthenticate.GooglePlayServicesAvailabilityException")
                            uiThread {
                                GoogleApiAvailability.getInstance().getErrorDialog(
                                        activity, ex.connectionStatusCode, REQUEST_GMS_ERROR_DIALOG).show()
                            }
                        }
                        is UserRecoverableAuthIOException -> {
                            // ユーザの許可を得るためのダイアログを表示する
                            Crashlytics.log("SettingsActivity.tryAuthenticate.UserRecoverableAuthException")
                            uiThread {
                                startActivityForResult(ex.intent, REQUEST_AUTHENTICATE)
                            }
                        }
                        is GoogleAuthIOException -> {
                            Crashlytics.log("SettingsActivity.tryAuthenticate.GoogleAuthIOException / message:'${ex.message}")
                            // 本来は認証エラーだが，エラーなしでも Unknown でここに来る時がある
                            // どうも getCause を辿るとこの場で UserRecoverableAuthException に辿り着けることもあるらしい
                            // 次にこれ発生したら調べるけどホントこれ再現性ない
                            val cause = ex.cause
                            if (cause is UserRecoverableAuthIOException) {
                                // ひょっとしたら cause が UserRecoverable かもしれない
                                uiThread {
                                    startActivityForResult(cause.intent, REQUEST_AUTHENTICATE)
                                }
                            } else {
                                if (ex.cause?.message == "Unknown") {
                                    // ひとまず認証が通ったと思ってOKとしてみる
                                    uiThread {
                                        storage.settings.googleAccount = storage.credential.selectedAccountName
                                        connectionPref.isChecked = true
                                        connectionPref.summary = storage.settings.googleAccount
                                        toast(R.string.connected_to_google_drive_probably)
                                    }
                                } else {
                                    uiThread { toast("tryAuthenticate: GoogleAuthIOException: ${ex.message}") }
                                }
                            }
                            Crashlytics.logException(ex)
                        }
                        is IOException -> {
                            // ネットワークエラーとかか？
                            Crashlytics.log("SettingsActivity.tryAuthenticate.IOException")
                            uiThread { toast("tryAuthenticate: IOException: " + ex.message) }
                            Crashlytics.logException(ex)
                        }
                        is RuntimeException -> {
                            // その他もう何だかわからないけどおかしい
                            uiThread { toast("tryAuthenticate: RuntimeException: " + ex.message) }
                            Crashlytics.logException(ex)
                        }
                        else -> {
                            throw ex
                        }
                    }
                }
            }
        }
    }
}
