package to.sava.cloudmarksandroid.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_debug.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import to.sava.cloudmarksandroid.R

class DebugActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        debug_clear_marks.setOnClickListener {
            Realm.getDefaultInstance().use {
                it.executeTransaction {
                    it.deleteAll()
                }
            }
            toast("Realmをtruncateしました")
        }
        debug_return.setOnClickListener {
            startActivity<MainActivity>()
        }
    }
}
