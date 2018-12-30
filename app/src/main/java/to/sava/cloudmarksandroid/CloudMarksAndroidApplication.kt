package to.sava.cloudmarksandroid

import android.app.Application
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import io.realm.Realm
import io.realm.RealmConfiguration


class CloudMarksAndroidApplication : Application() {

    companion object ApplicationInstance {
        lateinit var instance: CloudMarksAndroidApplication
    }

    var loading: Boolean = false
//    var saving: Boolean = false
//    var merging: Boolean = false

    var processing: Boolean
        get() = loading // || saving || merging
        set(value) {
            loading = value
//            saving = value
//            merging = value
        }

    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        val config = RealmConfiguration.Builder()
                .directory(this.cacheDir)
                .deleteRealmIfMigrationNeeded()
                .build()
        Realm.setDefaultConfiguration(config)

        Fabric.with(this, Crashlytics())

        instance = this
    }
}
