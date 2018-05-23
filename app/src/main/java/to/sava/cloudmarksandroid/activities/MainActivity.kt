package to.sava.cloudmarksandroid.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import to.sava.cloudmarksandroid.BlankFragment
import to.sava.cloudmarksandroid.R

class MainActivity : AppCompatActivity(), BlankFragment.OnFragmentButtonClickedListener {
    override fun onFragmentButtonClicked(current: String, next: String) {
        supportFragmentManager
                .beginTransaction()
                .add(R.id.main_view_wrapper, BlankFragment.newInstance("$current/$next"))
                .addToBackStack(null)
                .commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        setSupportActionBar(toolbar)
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.main_view_wrapper, BlankFragment.newInstance("root"))
                    .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
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
        menu?.findItem(R.id.main_menu_load)?.setEnabled(false)
        return super.onPrepareOptionsMenu(menu)
    }
}
