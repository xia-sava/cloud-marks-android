package to.sava.cloudmarksandroid.libs.di

import android.content.Context
import dagger.Module
import dagger.Provides
import io.realm.Realm
import to.sava.cloudmarksandroid.CloudMarksAndroidApplication
import to.sava.cloudmarksandroid.libs.Favicons
import to.sava.cloudmarksandroid.repositories.FaviconRepository
import to.sava.cloudmarksandroid.repositories.MarkNodeRepository

@Module
class RepositoryModule {

    @Provides
    fun provideApplicationContext(): Context = CloudMarksAndroidApplication.instance

    @Provides
    fun provideRealm() = Realm.getDefaultInstance()!!

    @Provides
    fun provideMarkNodeRepository() = MarkNodeRepository()

    @Provides
    fun provideFaviconRepository() = FaviconRepository()

    @Provides
    fun provideFavicons(context: Context, faviconRepository: FaviconRepository)
            = Favicons(context, faviconRepository)
}