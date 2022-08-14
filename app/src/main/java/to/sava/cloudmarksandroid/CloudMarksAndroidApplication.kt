package to.sava.cloudmarksandroid

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dagger.hilt.android.HiltAndroidApp
import to.sava.cloudmarksandroid.databases.CloudMarksAndroidDatabase


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")


@HiltAndroidApp
class CloudMarksAndroidApplication : Application() {

    companion object ApplicationInstance {
        lateinit var instance: CloudMarksAndroidApplication
        lateinit var database: CloudMarksAndroidDatabase
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        database = Room.databaseBuilder(
            this,
            CloudMarksAndroidDatabase::class.java,
            "cma.db"
        ).build()
    }
}