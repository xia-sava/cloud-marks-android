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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            transitionMarksFragment(MarkNode.ROOT_ID)
        }

        realm = Realm.getDefaultInstance()
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

    override fun onListItemChanged(mark: MarkNode?) {
        val backCount = supportFragmentManager.backStackEntryCount
        toolbar.title =
                if (backCount == 1) getString(R.string.app_name)
                else mark?.title ?: "ブックマークが見つかりません"
        supportActionBar?.setDisplayHomeAsUpEnabled(backCount > 1)
    }

    override fun onListItemClicked(mark: MarkNode?) {
        when (mark?.type) {
            MarkType.Folder -> {
                transitionMarksFragment(mark.id)
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
                doAsync {
                    Realm.getDefaultInstance().use {
                        val marks = Marks(it)
                        try {
                            marks.load()
                            uiThread {
                                toast("読んだ")
                                startActivity<MainActivity>()
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
                                    yesButton {
                                        startActivity<MainActivity>()
                                    }
                                }.show()
                            }
                        }
                    }
                }
            }
            R.id.main_menu_debug -> {
                startActivity<DebugActivity>()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val marksMenuEnabled = Settings().googleConnected
        menu?.findItem(R.id.main_menu_load)?.isEnabled = marksMenuEnabled
        return super.onPrepareOptionsMenu(menu)
    }

    private fun transitionMarksFragment(markId: String) {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_view_wrapper, MarksFragment.newInstance(markId))
                .addToBackStack(markId)
                .commit()
    }
}
