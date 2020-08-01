package to.sava.cloudmarksandroid.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.libs.GoogleDriveStorage
import to.sava.cloudmarksandroid.libs.Settings
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Google Drive Settings フラグメント
 */
class GoogleDrivePreferenceFragment : SettingsFragment(),
    Preference.OnPreferenceClickListener,
    CoroutineScope {

    @Inject
    internal lateinit var settings: Settings

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val handler = Handler(Looper.getMainLooper())

    private val storage: GoogleDriveStorage by lazy {
        GoogleDriveStorage(settings)
    }

    private val connectionPref: SwitchPreference by lazy {
        findPreference<SwitchPreference>(
            getString(
                R.string.pref_key_google_drive_connection
            )
        ) as SwitchPreference
    }

    private val fc = FirebaseCrashlytics.getInstance()

    companion object {
        const val REQUEST_PICK_ACCOUNT = 1
        const val REQUEST_AUTHENTICATE = 2
        const val REQUEST_GMS_ERROR_DIALOG = 3
        const val REQUEST_GRANT_PERMISSION = 4
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)

        super.onAttach(context)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    /**
     * フラグメント初回生成処理
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(
            R.xml.settings, getString(
                R.string.pref_key_google_drive
            )
        )
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
                    true -> signInGoogle()
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
     * Google サインイン
     */
    private fun signInGoogle() {
        // 接続処理をする
        if (checkPermission()) {
            toast(R.string.connect_to_google_drive)
            fc.log("SettingsActivity.onPreferenceClick.startActivityForResult")

            val scopes = GoogleDriveStorage.SCOPES.map { Scope(it) }
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestScopes(scopes[0], *scopes.drop(1).toTypedArray())
                .build()
            val intent = GoogleSignIn.getClient(requireContext(), gso).signInIntent
            startActivityForResult(intent, REQUEST_PICK_ACCOUNT)
        }
    }

    /**
     * GET_ACCOUNT 権限チェックをして，権限がなければ requestPermission() する．
     */
    private fun checkPermission(): Boolean {
        val context = storage.settings.context
        val perm = Manifest.permission.GET_ACCOUNTS
        if (ContextCompat.checkSelfPermission(
                context,
                perm
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        requestPermissions(arrayOf(perm), REQUEST_GRANT_PERMISSION)
        return false
    }

    /**
     * requestPermissions の戻り先
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_GRANT_PERMISSION) {
            for ((i, perm) in permissions.withIndex()) {
                val granted = grantResults[i]
                when (perm) {
                    Manifest.permission.GET_ACCOUNTS -> {
                        if (granted != PackageManager.PERMISSION_GRANTED) {
                            toast(R.string.require_get_account_permission)
                        } else {
                            signInGoogle()
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
        fc.log("SettingsActivity.onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_PICK_ACCOUNT -> {
                fc.log("SettingsActivity.onActivityResult.REQUEST_PICK_ACCOUNT")
                try {
                    GoogleSignIn.getSignedInAccountFromIntent(data)
                        .getResult(ApiException::class.java)?.let { account ->
                            fc.log("SettingsActivity.onActivityResult.REQUEST_PICK_ACCOUNT / name: '${account.email}'")
                            tryAuthenticate(account.email ?: "")
                        }
                } catch (e: ApiException) {
                    toast(getString(R.string.cant_confirm_user_account, e.statusCode))
                }
            }
            REQUEST_AUTHENTICATE -> {
                fc.log("SettingsActivity.onActivityResult.REQUEST_AUTHENTICATE")
                if (resultCode == Activity.RESULT_OK) {
                    tryAuthenticate(storage.settings.googleAccount)
                } else {
                    toast(R.string.connecting_google_drive_denied)
                }
            }
            else -> {
                fc.log("SettingsActivity.onActivityResult.else")
                throw RuntimeException("requestCode: $requestCode resultCode: $resultCode")
            }
        }
    }

    /**
     * Google Drive アクセスチェック．
     */
    private fun tryAuthenticate(name: String) {
        launch {
            try {
                fc.log("SettingsActivity.tryAuthenticate.checkAccessibility")
                storage.credential.selectedAccountName = name
                val accessOk = storage.checkAccessibility()
                handler.post {
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
                        fc.recordException(ex)
                        fc.log("SettingsActivity.tryAuthenticate.GooglePlayServicesAvailabilityException")
                        handler.post {
                            GoogleApiAvailability.getInstance()
                                .getErrorDialog(
                                    activity, ex.connectionStatusCode, REQUEST_GMS_ERROR_DIALOG
                                ).show()
                        }
                    }
                    is UserRecoverableAuthIOException -> {
                        // ユーザの許可を得るためのダイアログを表示する
                        fc.recordException(ex)
                        fc.log("SettingsActivity.tryAuthenticate.UserRecoverableAuthException")
                        handler.post {
                            startActivityForResult(ex.intent, REQUEST_AUTHENTICATE)
                        }
                    }
                    is GoogleAuthIOException -> {
                        fc.recordException(ex)
                        fc.log("SettingsActivity.tryAuthenticate.GoogleAuthIOException / message:'${ex.message}'")
                        // 本来は認証エラーだが，エラーなしでも Unknown でここに来る時がある
                        // どうも getCause を辿るとこの場で UserRecoverableAuthException に辿り着けることもあるらしい
                        // 次にこれ発生したら調べるけどホントこれ再現性ない
                        var handled = false
                        var cause: Throwable? = ex.cause
                        while (cause != null) {
                            val c = cause
                            // ひょっとしたら cause が UserRecoverable かもしれない
                            if (c is UserRecoverableAuthIOException) {
                                handler.post {
                                    startActivityForResult(c.intent, REQUEST_AUTHENTICATE)
                                }
                                handled = true
                                break
                            } else if (c is UserRecoverableAuthException) {
                                handler.post {
                                    startActivityForResult(c.intent, REQUEST_AUTHENTICATE)
                                }
                                handled = true
                                break
                            }
                            cause = cause.cause
                        }
                        if (!handled) {
                            fc.log("SettingsActivity.tryAuthenticate.GoogleAuthIOException / message:'${ex.message}")
                            handler.post {
                                toast("tryAuthenticate: GoogleAuthIOException: ${ex.message}")
                            }
                        }
                    }
                    is IOException -> {
                        // ネットワークエラーとかか？
                        fc.recordException(ex)
                        fc.log("SettingsActivity.tryAuthenticate.IOException")
                        handler.post {
                            toast("tryAuthenticate: IOException: ${ex.message}")
                        }
                    }
                    is RuntimeException -> {
                        // その他もう何だかわからないけどおかしい
                        fc.recordException(ex)
                        handler.post {
                            toast("tryAuthenticate: RuntimeException: ${ex.message}")
                        }
                    }
                    else -> {
                        fc.recordException(ex)
                        throw ex
                    }
                }
            }
        }
    }
}