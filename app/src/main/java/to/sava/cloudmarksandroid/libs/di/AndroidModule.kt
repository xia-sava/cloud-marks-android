package to.sava.cloudmarksandroid.libs.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import to.sava.cloudmarksandroid.activities.MainActivity
import to.sava.cloudmarksandroid.activities.SettingsActivity
import to.sava.cloudmarksandroid.fragments.MarksFragment
import to.sava.cloudmarksandroid.services.MarksService

@Module
abstract class AndroidModule {

    @ContributesAndroidInjector
    abstract fun contributeMainActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun contributeSettingsActivity(): SettingsActivity

    @ContributesAndroidInjector
    abstract fun contributeMarksFragment(): MarksFragment

    @ContributesAndroidInjector
    abstract fun contributeMarksService(): MarksService
}