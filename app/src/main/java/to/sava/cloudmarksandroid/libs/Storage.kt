package to.sava.cloudmarksandroid.libs

import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import java.util.*


abstract class Storage(val settings: Settings) {
    abstract fun checkAccessibility(): Boolean
}


class GoogleDriveStorage(settings: Settings): Storage(settings) {

    val credential: GoogleAccountCredential by lazy {
        GoogleAccountCredential.usingOAuth2(settings.context, SCOPES)
    }

    private val api: Drive by lazy {
        Drive.Builder(AndroidHttp.newCompatibleTransport(), GsonFactory(), credential).build()
    }

    companion object {
        val SCOPES: List<String> = Arrays.asList("https://www.googleapis.com/auth/drive")
        val SCOPES_STR: String
            get() = "oauth2${SCOPES.joinToString(" ")}"
    }

    /**
     * 試しに今の認証情報でアクセスしてみる．
     * エラーの Exception は全部素通し．
     */
    override fun checkAccessibility(): Boolean {
        api.files().get("root").execute()
        return true
    }
}