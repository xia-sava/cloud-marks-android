package to.sava.cloudmarksandroid.libs.di

import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import to.sava.cloudmarksandroid.CloudMarksAndroidApplication

@Component(modules = [
    AndroidSupportInjectionModule::class,
    RepositoryModule::class,
    AndroidModule::class
])
interface ApplicationComponent: AndroidInjector<CloudMarksAndroidApplication> {
    @Component.Builder
    abstract class Builder: AndroidInjector.Builder<CloudMarksAndroidApplication>()
}