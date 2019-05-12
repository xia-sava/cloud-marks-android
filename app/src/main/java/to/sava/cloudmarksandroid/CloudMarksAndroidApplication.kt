package to.sava.cloudmarksandroid

import android.app.Activity
import android.app.Application
import android.app.Service
import androidx.fragment.app.Fragment
import com.crashlytics.android.Crashlytics
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import dagger.android.support.HasSupportFragmentInjector
import io.fabric.sdk.android.Fabric
import io.realm.Realm
import io.realm.RealmConfiguration
import to.sava.cloudmarksandroid.libs.di.DaggerApplicationComponent
import javax.inject.Inject
import com.crashlytics.android.core.CrashlyticsCore




class CloudMarksAndroidApplication : Application(),
        HasActivityInjector, HasSupportFragmentInjector, HasServiceInjector {

    @Inject
    lateinit var dispatchingActivityInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var dispatchingFragmentInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var dispatchingServiceInjector: DispatchingAndroidInjector<Service>

    override fun activityInjector() = dispatchingActivityInjector
    override fun supportFragmentInjector() = dispatchingFragmentInjector
    override fun serviceInjector() = dispatchingServiceInjector

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

        DaggerApplicationComponent.factory()
                .create(this)
                .inject(this)

        Realm.init(this)
        val config = RealmConfiguration.Builder()
                .directory(this.cacheDir)
                .deleteRealmIfMigrationNeeded()
                .build()
        Realm.setDefaultConfiguration(config)

        Fabric.with(this, Crashlytics.Builder().core(
                CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build())
    }
}
