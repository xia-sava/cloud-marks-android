package to.sava.cloudmarksandroid.libs.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import to.sava.cloudmarksandroid.CloudMarksAndroidApplication
import to.sava.cloudmarksandroid.databases.CloudMarksAndroidDatabase
import to.sava.cloudmarksandroid.databases.dao.FaviconDao
import to.sava.cloudmarksandroid.databases.dao.MarkNodeDao
import to.sava.cloudmarksandroid.databases.repositories.FaviconRepository
import to.sava.cloudmarksandroid.libs.Marks
import to.sava.cloudmarksandroid.databases.repositories.MarkNodeRepository
import to.sava.cloudmarksandroid.libs.Settings
import to.sava.cloudmarksandroid.services.FaviconService
import to.sava.cloudmarksandroid.services.MarksService
import to.sava.cloudmarksandroid.ui.activities.MainActivity
import to.sava.cloudmarksandroid.ui.activities.SettingsActivity
import to.sava.cloudmarksandroid.ui.fragments.ApplicationPreferenceFragment
import to.sava.cloudmarksandroid.ui.fragments.GoogleDrivePreferenceFragment
import to.sava.cloudmarksandroid.ui.fragments.MarksFragment
import to.sava.cloudmarksandroid.ui.fragments.SettingsFragment

@Module
abstract class AndroidModule {

    @ContributesAndroidInjector
    abstract fun contributeMainActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun contributeSettingsActivity(): SettingsActivity

    @ContributesAndroidInjector
    abstract fun contributeMarksFragment(): MarksFragment

    @ContributesAndroidInjector
    abstract fun contributeSettingsFragment(): SettingsFragment

    @ContributesAndroidInjector
    abstract fun contributeApplicationPreferenceFragment(): ApplicationPreferenceFragment

    @ContributesAndroidInjector
    abstract fun contributeMarksGoogleDrivePreferenceFragment(): GoogleDrivePreferenceFragment

    @ContributesAndroidInjector
    abstract fun contributeMarksService(): MarksService

    @ContributesAndroidInjector
    abstract fun contributeFaviconService(): FaviconService
}

@Module
class ApplicationModule {
    @Provides
    fun provideApplicationContext(): Context = CloudMarksAndroidApplication.instance

    @Provides
    fun provideCloudMarksAndroidDatabase(): CloudMarksAndroidDatabase =
        CloudMarksAndroidApplication.database

    @Provides
    fun provideMarkNodeDao(db: CloudMarksAndroidDatabase) = db.markNodeDao()

    @Provides
    fun provideFaviconDao(db: CloudMarksAndroidDatabase) = db.faviconDao()

    @Provides
    fun provideSettings(context: Context) = Settings(context)

    @Provides
    fun provideMarkNodeRepository(markNodeDao: MarkNodeDao) = MarkNodeRepository(markNodeDao)

    @Provides
    fun provideFaviconRepository(context: Context, faviconDao: FaviconDao) =
        FaviconRepository(context, faviconDao)

    @Provides
    fun provideMarks(settings: Settings, markNodeRepository: MarkNodeRepository) =
        Marks(settings, markNodeRepository)
}