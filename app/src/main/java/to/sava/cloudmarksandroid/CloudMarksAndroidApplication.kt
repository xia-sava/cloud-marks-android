package to.sava.cloudmarksandroid

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration


class CloudMarksAndroidApplication : Application() {

    companion object ApplicationInstance {
        lateinit var instance: CloudMarksAndroidApplication
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

