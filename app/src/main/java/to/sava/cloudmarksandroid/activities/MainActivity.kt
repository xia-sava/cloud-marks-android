package to.sava.cloudmarksandroid.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.fragments.MarksFragment
import to.sava.cloudmarksandroid.libs.Marks
import to.sava.cloudmarksandroid.libs.ServiceAuthenticationException
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkType
import to.sava.cloudmarksandroid.libs.Settings


class MainActivity : AppCompatActivity(),
        MarksFragment.OnListItemClickedListener, MarksFragment.OnListItemChangedListener {

    private lateinit var realm: Realm

    override fun onCreate(savedInstanceState: Bundle?) {
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
        when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.main_menu_settings -> {
                startActivity<SettingsActivity>()
            }
            R.id.main_menu_load -> {
                toast("クラウドから読込みます")
                val marks = Marks(this)
                doAsync {
                    try {
                        marks.load()
                        uiThread {
                            toast("読んだ")
                        }
                    } catch (ex: ServiceAuthenticationException) {
                        uiThread {
                            alert("認証エラーが発生しました。クラウド接続設定をご確認ください。") {
                                yesButton {
                                    startActivity<SettingsActivity>()
                                }
                            }.show()
                        }
                    } catch (ex: Exception) {
                        uiThread {
                            alert("エラーが発生しました： ${ex.message}") {
                                yesButton { }
                            }.show()
                        }
                    }
                }
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val marksMenuEnabled = Settings(this).googleConnected
        menu?.findItem(R.id.main_menu_load)?.isEnabled = marksMenuEnabled
        return super.onPrepareOptionsMenu(menu)
    }
}
