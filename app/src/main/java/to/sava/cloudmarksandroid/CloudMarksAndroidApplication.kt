package to.sava.cloudmarksandroid

import android.app.Application
import androidx.room.Room
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import to.sava.cloudmarksandroid.databases.CloudMarksAndroidDatabase
import to.sava.cloudmarksandroid.libs.di.DaggerApplicationComponent
import javax.inject.Inject

class CloudMarksAndroidApplication : Application(),
    HasAndroidInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector() = dispatchingAndroidInjector

    companion object ApplicationInstance {
        lateinit var instance: CloudMarksAndroidApplication
        lateinit var database: CloudMarksAndroidDatabase
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

        database = Room.databaseBuilder(
            this,
            CloudMarksAndroidDatabase::class.java,
            "cma.db"
        )
            .allowMainThreadQueries()
            .build()

        DaggerApplicationComponent.factory()
                .create(this)
                .inject(this)
    }
}