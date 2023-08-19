package to.sava.cloudmarksandroid

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import to.sava.cloudmarksandroid.databases.CloudMarksAndroidDatabase
import to.sava.cloudmarksandroid.di.appModule

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class CloudMarksAndroidApplication : Application(), KoinComponent {
    override fun onCreate() {
        super.onCreate()

        instance = this

        database = Room.databaseBuilder(
            this,
            CloudMarksAndroidDatabase::class.java,
            "cma.db"
        ).build()

        startKoin {
            androidContext(this@CloudMarksAndroidApplication)
            workManagerFactory()
            modules(appModule())
        }
    }

    companion object ApplicationInstance {
        lateinit var instance: CloudMarksAndroidApplication
        lateinit var database: CloudMarksAndroidDatabase
    }
}
