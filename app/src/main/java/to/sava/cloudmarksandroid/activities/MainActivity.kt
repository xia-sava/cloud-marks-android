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

class MainActivity : AppCompatActivity(),
        MarksFragment.OnListItemClickedListener, MarksFragment.OnListItemChangedListener {

    private lateinit var realm: Realm

    init {
        Marks().load()
    }

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
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.main_menu_load)?.isEnabled = false
        return super.onPrepareOptionsMenu(menu)
    }
}
