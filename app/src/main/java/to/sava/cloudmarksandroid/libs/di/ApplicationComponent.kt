package to.sava.cloudmarksandroid.libs.di

import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import to.sava.cloudmarksandroid.CloudMarksAndroidApplication

@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        AndroidModule::class,
        ApplicationModule::class
    ]
)
interface ApplicationComponent : AndroidInjector<CloudMarksAndroidApplication> {
    @Component.Factory
    interface Factory : AndroidInjector.Factory<CloudMarksAndroidApplication>
}