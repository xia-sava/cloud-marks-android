package to.sava.cloudmarksandroid

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Process
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import kotlinx.android.synthetic.main.activity_google_login_sample.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import android.content.Intent
import android.os.Handler
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import java.util.Arrays.asList
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import java.io.IOException


class GoogleLoginSampleActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private lateinit var credential: GoogleAccountCredential
    private val scopes = asList("https://www.googleapis.com/auth/drive")
    private lateinit var service: Drive

    companion object {
        const val REQUEST_PICK_ACCOUNT = 1
        const val REQUEST_AUTHENTICATE = 2
        const val REQUEST_GMS_ERROR_DIALOG = 3
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        alert("エラーよ")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_login_sample)

        credential = GoogleAccountCredential.usingOAuth2(this, scopes)

        sign_in_button.setOnClickListener {
            if (it.id == R.id.sign_in_button) {
                val signInIntent = credential.newChooseAccountIntent()
                startActivityForResult(signInIntent, REQUEST_PICK_ACCOUNT)
//                credential.selectedAccount = Account("xia013@gmail.com", "com.google")
//                listFiles()
            }
        }
        button.setOnClickListener {
            listFiles()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_PICK_ACCOUNT -> {
                if(resultCode == Activity.RESULT_OK && data.extras != null) {
                    val name = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                    if (name != null) {
                        val account = Account(name, "com.google")
                        credential.selectedAccount = account
                        tryAuthenticate(account)
                    }
                }
            }
            REQUEST_AUTHENTICATE -> {
                if(resultCode == Activity.RESULT_OK) {
                    tryAuthenticate(credential.selectedAccount)
                }
            }
        }
    }

    private fun tryAuthenticate(account: Account) {
        val handler = Handler()
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            var message = ""
            try {
                val token = GoogleAuthUtil.getToken(this, account, "oauth2" + scopes.joinToString(" "))
                message = token
            } catch (playEx: GooglePlayServicesAvailabilityException) {
                GoogleApiAvailability.getInstance().getErrorDialog(this,
                        playEx.connectionStatusCode, REQUEST_GMS_ERROR_DIALOG)
            } catch (userAuthEx: UserRecoverableAuthException) {
                startActivityForResult(userAuthEx.intent, REQUEST_AUTHENTICATE)
            } catch (transientEx: IOException) {
                message = transientEx.message ?: ""
            } catch (authEx: GoogleAuthException) {
                message = authEx.message ?: ""
            }
            if (message != "") {
                handler.post { toast(message) }
            }
        }).start()
    }

    private fun listFiles() {
        service = Drive.Builder(AndroidHttp.newCompatibleTransport(), GsonFactory(), credential).build()

        val handler = Handler()
        Thread {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                val result = service.files()?.list()
                        ?.setQ("name = 'cloud_marks' and trashed = false")
                        ?.execute()
                handler.post {
                    toast(result.toString())
                }
            } catch (userAuthIoEx: UserRecoverableAuthIOException) {
                startActivityForResult(userAuthIoEx.intent, REQUEST_AUTHENTICATE)
            } catch (illArgEx: IllegalArgumentException) {
                handler.post { toast(illArgEx.message!!) }
            }
        }.start()
    }
}
