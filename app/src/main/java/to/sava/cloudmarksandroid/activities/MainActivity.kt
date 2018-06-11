package to.sava.cloudmarksandroid.activities

import android.content.ClipData
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

    private fun onListItemClick(mark: MarkNode, choiceApp: Boolean) {
        when (mark.type) {
            MarkType.Folder -> {
                transitionMarksFragment(mark.id)
            }
            MarkType.Bookmark -> {
                var intent = Intent(Intent.ACTION_VIEW, Uri.parse(mark.url))
                if (choiceApp) {
                    intent = Intent.createChooser(intent, getString(R.string.mark_menu_share_to))
                }
                startActivity(intent)
            }
        }
    }

    override fun onListItemClick(mark: MarkNode) {
        return onListItemClick(mark, false)
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {
        val fragment = supportFragmentManager.fragments.last()
        if (fragment is MarksFragment) {
            item?.groupId?.let {
                fragment.adapter?.getItem(it)?.let { mark ->
                    when (item.itemId) {
                        R.id.mark_menu_open -> {
                            onListItemClick(mark)
                            return true
                        }
                        R.id.mark_menu_share_to -> {
                            onListItemClick(mark, true)
                            return true
                        }
                        R.id.mark_menu_copy_url -> {
                            clipboardManager.primaryClip = ClipData.newRawUri("", Uri.parse(mark.url))
                            toast(R.string.mark_toast_copy_url)
                            return true
                        }
                        R.id.mark_menu_copy_title -> {
                            clipboardManager.primaryClip = ClipData.newPlainText("", mark.title)
                            toast(R.string.mark_toast_copy_title)
                            return true
                        }
                        else -> Unit
                    }
                }
            }
        }
        return false
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
