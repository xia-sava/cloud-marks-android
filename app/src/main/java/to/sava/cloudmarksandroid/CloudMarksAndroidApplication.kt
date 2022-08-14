package to.sava.cloudmarksandroid

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.work.HiltWorkerFactory
import androidx.room.Room
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import to.sava.cloudmarksandroid.databases.CloudMarksAndroidDatabase
import javax.inject.Inject


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")


@HiltAndroidApp
class CloudMarksAndroidApplication : Application(), Configuration.Provider {

    companion object ApplicationInstance {
        lateinit var instance: CloudMarksAndroidApplication
        lateinit var database: CloudMarksAndroidDatabase
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        instance = this

        database = Room.databaseBuilder(
            this,
            CloudMarksAndroidDatabase::class.java,
            "cma.db"
        ).build()
    }

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}