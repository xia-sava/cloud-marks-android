package to.sava.cloudmarksandroid

import android.app.Application
import android.util.Log
import io.realm.Realm
import io.realm.RealmConfiguration


class CloudMarksAndroidApplication : Application() {

    companion object ApplicationInstance {
        lateinit var instance: CloudMarksAndroidApplication
    }

    var loading: Boolean = false
        set(value) {
            Log.i("cma", "loading: set $value")
            field = value
        }


    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        val config = RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build()
        Realm.setDefaultConfiguration(config)

        instance = this
    }
}

