package to.sava.cloudmarksandroid.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import to.sava.cloudmarksandroid.CloudMarksAndroidApplication
import to.sava.cloudmarksandroid.dataStore
import to.sava.cloudmarksandroid.databases.CloudMarksAndroidDatabase
import to.sava.cloudmarksandroid.databases.dao.FaviconDao
import to.sava.cloudmarksandroid.databases.dao.MarkNodeDao
import to.sava.cloudmarksandroid.databases.repositories.FaviconRepository
import to.sava.cloudmarksandroid.databases.repositories.MarkNodeRepository
import to.sava.cloudmarksandroid.modules.Marks
import to.sava.cloudmarksandroid.modules.Settings

@Module
@InstallIn(SingletonComponent::class)
abstract class DiModuleBinding {
}

@Module
@InstallIn(SingletonComponent::class)
object DiModuleProvider {
    @Provides
    fun provideApplicationContext(): Context =
        CloudMarksAndroidApplication.instance

    @Provides
    fun provideCloudMarksAndroidDatabase(): CloudMarksAndroidDatabase =
        CloudMarksAndroidApplication.database

    @Provides
    fun provideDataStore(context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    fun provideMarkNodeDao(db: CloudMarksAndroidDatabase) =
        db.markNodeDao()

    @Provides
    fun provideFaviconDao(db: CloudMarksAndroidDatabase) =
        db.faviconDao()

    @Provides
    fun provideSettings(context: Context) =
        Settings(context, context.dataStore)

    @Provides
    fun provideMarkNodeRepository(markNodeDao: MarkNodeDao) =
        MarkNodeRepository(markNodeDao)

    @Provides
    fun provideFaviconRepository(context: Context, faviconDao: FaviconDao) =
        FaviconRepository(context, faviconDao)

    @Provides
    fun provideMarks(settings: Settings, markNodeRepository: MarkNodeRepository) =
        Marks(settings, markNodeRepository)
}
