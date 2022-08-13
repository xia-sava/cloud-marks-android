package to.sava.cloudmarksandroid

import android.app.Application
import androidx.room.Room
import dagger.hilt.android.HiltAndroidApp
import to.sava.cloudmarksandroid.databases.CloudMarksAndroidDatabase

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