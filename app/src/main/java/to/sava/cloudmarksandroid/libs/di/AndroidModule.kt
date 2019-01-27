package to.sava.cloudmarksandroid.libs.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import to.sava.cloudmarksandroid.activities.MainActivity
import to.sava.cloudmarksandroid.activities.SettingsActivity

@Module
abstract class AndroidModule {

    @ContributesAndroidInjector
    abstract fun contributeMainActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun contributeSettingsActivity(): SettingsActivity
}