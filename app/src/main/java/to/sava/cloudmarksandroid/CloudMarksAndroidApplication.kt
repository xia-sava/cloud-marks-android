package to.sava.cloudmarksandroid

import android.app.Activity
import android.app.Application
import androidx.fragment.app.Fragment
import com.crashlytics.android.Crashlytics
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.support.HasSupportFragmentInjector
import io.fabric.sdk.android.Fabric
import io.realm.Realm
import io.realm.RealmConfiguration
import to.sava.cloudmarksandroid.libs.di.DaggerApplicationComponent
import javax.inject.Inject


class CloudMarksAndroidApplication : Application(),
        HasActivityInjector, HasSupportFragmentInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var dispatchingFragmentInjector: DispatchingAndroidInjector<Fragment>

    override fun activityInjector() = dispatchingAndroidInjector
    override fun supportFragmentInjector() = dispatchingFragmentInjector

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

        instance = this

        DaggerApplicationComponent.builder()
                .create(this)
                .inject(this)

        Realm.init(this)
        val config = RealmConfiguration.Builder()
                .directory(this.cacheDir)
                .deleteRealmIfMigrationNeeded()
                .build()
        Realm.setDefaultConfiguration(config)

        Fabric.with(this, Crashlytics())
    }
}
