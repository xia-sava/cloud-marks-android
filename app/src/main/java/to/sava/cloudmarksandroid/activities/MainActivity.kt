package to.sava.cloudmarksandroid.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.fragments.MarksFragment
import to.sava.cloudmarksandroid.libs.Marks
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkType
import android.accounts.AccountManager
import to.sava.cloudmarksandroid.GoogleLoginSampleActivity
import to.sava.cloudmarksandroid.libs.Settings


class MainActivity : AppCompatActivity(),
        MarksFragment.OnListItemClickedListener, MarksFragment.OnListItemChangedListener {

    private lateinit var realm: Realm

    private val TAG = "AUTH_SAMPLE"
    private val ACCOUNT_TYPE_GOOGLE = "com.google"
    private val AUTH_SCOPE = "oauth2:profile email"
    private val REQUEST_SIGN_IN = 10000
    private var mAccountName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        Marks(Settings(this)).setupFixture()

        realm = Realm.getDefaultInstance()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.main_view_wrapper, MarksFragment.newInstance("root"))
                    .addToBackStack("root")
                    .commit()
        }

        val accountManager = AccountManager.get(this)
//        val accounts = accountManager.getAccountsByType(ACCOUNT_TYPE_GOOGLE)
//        mAccountName = accounts[0].name
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (supportFragmentManager.backStackEntryCount == 0) {
            finish()
        }
    }

    override fun onListItemChanged(mark: MarkNode) {
        toolbar.title = mark.title
        supportActionBar?.setDisplayHomeAsUpEnabled(supportFragmentManager.backStackEntryCount > 1)
    }

    override fun onListItemClicked(mark: MarkNode?) {
        when (mark?.type) {
            MarkType.Folder -> {
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.main_view_wrapper, MarksFragment.newInstance(mark.id))
                        .addToBackStack(mark.id)
                        .commit()
            }
            MarkType.Bookmark -> {
                toast(mark.url)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.main_menu_settings -> {
                startActivity<SettingsActivity>()
                true
            }
            R.id.main_menu_load -> {
                toast("読むぜ！")
                val marks = Marks(Settings(this))
                startActivity<GoogleLoginSampleActivity>()
//                getGoogleToken(mAccountName)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.main_menu_load)?.isEnabled = true
        return super.onPrepareOptionsMenu(menu)
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == Activity.RESULT_OK && RC_AUTHORIZE_CONTACTS == requestCode) {
//            getContacts()
//        }
//    }
//
//    private fun getGoogleToken(accountName: String) {
//        val SCOPE_CONTACTS_READ = Scope("https://www.googleapis.com/auth/contacts.readonly")
//        val SCOPE_EMAIL = Scope(Scopes.EMAIL)
//
//        if (!GoogleSignIn.hasPermissions(
//                        GoogleSignIn.getLastSignedInAccount(this),
//                        SCOPE_CONTACTS_READ,
//                        SCOPE_EMAIL)) {
//            GoogleSignIn.requestPermissions(
//                    this,
//                    RC_AUTHORIZE_CONTACTS,
//                    GoogleSignIn.getLastSignedInAccount(this),
//                    SCOPE_CONTACTS_READ,
//                    SCOPE_EMAIL)
//        } else {
//            getContacts()
//        }
//    }
}
