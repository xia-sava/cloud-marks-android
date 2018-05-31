package to.sava.cloudmarksandroid.activities

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
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
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import to.sava.cloudmarksandroid.R
import java.io.IOException
import java.util.*

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
        return isXLargeTablet(this)
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
    class ApplicationPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_application)
            setHasOptionsMenu(true)

            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_app_folder_name)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_app_autosync)))
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class GoogleDrivePreferenceFragment : PreferenceFragment(), Preference.OnPreferenceClickListener  {
        private lateinit var credential: GoogleAccountCredential
        private val scopes = Arrays.asList("https://www.googleapis.com/auth/drive")
        private lateinit var connectionPref: SwitchPreference

        companion object {
            const val REQUEST_PICK_ACCOUNT = 1
            const val REQUEST_AUTHENTICATE = 2
            const val REQUEST_GMS_ERROR_DIALOG = 3
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_google_drive)
            setHasOptionsMenu(true)

            connectionPref = (findPreference(getString(R.string.pref_key_google_drive_connection)) as SwitchPreference)
            connectionPref.onPreferenceClickListener = this
        }

        override fun onPreferenceClick(preference: Preference?): Boolean {
            when (preference?.key) {
                getString(R.string.pref_key_google_drive_connection) -> {
                    when (connectionPref.isChecked) {
                        true -> {
                            // 接続処理をする
                            toast("Google Drive へ接続します")
                            credential = GoogleAccountCredential.usingOAuth2(activity, scopes)
                            startActivityForResult(credential.newChooseAccountIntent(), REQUEST_PICK_ACCOUNT)
                        }
                        false -> {
                            // 接続をリセットする
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
                            val account = Account(name, "com.google")
                            credential.selectedAccount = account
                            tryAuthenticate(account)
                        }
                    } else {
                        toast("アカウントが確認できませんでした")
                    }
                }
                REQUEST_AUTHENTICATE -> {
                    if (resultCode == Activity.RESULT_OK) {
                        tryAuthenticate(credential.selectedAccount)
                    } else {
                        toast("Google Drive との接続が許可されませんでした")
                    }
                }
            }
        }

        private fun tryAuthenticate(account: Account) {
            doAsync {
                try {
                    GoogleAuthUtil.getToken(activity, account, "oauth2" + scopes.joinToString(" "))
                    listFiles()
                } catch (playEx: GooglePlayServicesAvailabilityException) {
                    GoogleApiAvailability.getInstance().getErrorDialog(activity, playEx.connectionStatusCode, REQUEST_GMS_ERROR_DIALOG)
                } catch (userAuthEx: UserRecoverableAuthException) {
                    startActivityForResult(userAuthEx.intent, REQUEST_AUTHENTICATE)
                } catch (transientEx: IOException) {
                    uiThread { toast(transientEx.message ?: "") }
                } catch (authEx: GoogleAuthException) {
                    if (authEx.message == "Unknown") {
                        listFiles()
                    } else {
                        uiThread { toast(authEx.message ?: "") }
                    }
                }
            }
        }

        private fun listFiles() {
            val service = Drive.Builder(AndroidHttp.newCompatibleTransport(), GsonFactory(), credential).build()

            doAsync {
                try {
                    val result = service.files()?.list()
                            ?.setQ("name = 'cloud_marks' and trashed = false")
                            ?.execute()
                    uiThread {
                        connectionPref.isChecked = true
                        toast(result.toString())
                    }
                } catch (userAuthIoEx: UserRecoverableAuthIOException) {
                    startActivityForResult(userAuthIoEx.intent, REQUEST_AUTHENTICATE)
                } catch (illArgEx: IllegalArgumentException) {
                    uiThread { toast(illArgEx.message!!) }
                } catch (ex: Exception) {
                    uiThread {
                        connectionPref.isChecked = false
                        toast(ex.message!!)
                    }
                }
            }
        }
    }

    companion object {

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val index = preference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(
                        if (index >= 0)
                            preference.entries[index]
                        else
                            null)

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.summary = stringValue
            }
            true
        }

        /**
         * Helper method to determine if the device has an extra-large screen. For
         * example, 10" tablets are extra-large.
         */
        private fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
        }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.

         * @see .sBindPreferenceSummaryToValueListener
         */
        private fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.context)
                            .getString(preference.key, ""))
        }
    }
}
