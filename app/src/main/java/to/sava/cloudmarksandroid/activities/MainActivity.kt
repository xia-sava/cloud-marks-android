package to.sava.cloudmarksandroid.activities

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*
import to.sava.cloudmarksandroid.CloudMarksAndroidApplication
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.fragments.MarksFragment
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkType
import to.sava.cloudmarksandroid.libs.Settings
import to.sava.cloudmarksandroid.services.MarksIntentService


class MainActivity : AppCompatActivity(),
        MarksFragment.OnListItemClickListener,
        MarksFragment.OnListItemLongClickListener,
        MarksFragment.OnListItemChangListener {

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

    override fun onListItemChange(mark: MarkNode?) {
        val backCount = supportFragmentManager.backStackEntryCount
        toolbar.title =
                if (backCount == 1) getString(R.string.app_name)
                else mark?.title ?: "ブックマークが見つかりません"
        supportActionBar?.setDisplayHomeAsUpEnabled(backCount > 1)
    }

    override fun onListItemClick(mark: MarkNode) {
        when (mark.type) {
            MarkType.Folder -> {
                transitionMarksFragment(mark.id)
            }
            MarkType.Bookmark -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mark.url)))
            }
        }
    }

    override fun onListItemLongClick(mark: MarkNode): Boolean {
        toast(mark.title)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val marksMenuEnabled = (
                Settings().googleConnected &&
                        !CloudMarksAndroidApplication.instance.loading
                )
        menu?.findItem(R.id.main_menu_load)?.isEnabled = marksMenuEnabled
        return super.onPrepareOptionsMenu(menu)
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
                MarksIntentService.startActionLoad(this)
            }
            R.id.main_menu_debug -> {
                startActivity<DebugActivity>()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun transitionMarksFragment(markId: String) {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_view_wrapper, MarksFragment.newInstance(markId))
                .addToBackStack(markId)
                .commit()
    }
}
