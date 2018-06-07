package to.sava.cloudmarksandroid.activities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_debug.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.services.MarksIntentService

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

        debug_start_service.setOnClickListener {
            MarksIntentService.startActionLoad(this)
        }

        debug_return.setOnClickListener {
            val intent = intentFor<MainActivity>()
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }
}
