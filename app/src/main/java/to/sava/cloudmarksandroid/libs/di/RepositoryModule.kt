package to.sava.cloudmarksandroid.libs.di

import dagger.Module
import dagger.Provides
import to.sava.cloudmarksandroid.repositories.FaviconRepository
import to.sava.cloudmarksandroid.repositories.MarkNodeRepository

@Module
class RepositoryModule {

    @Provides
    fun provideMarkNodeRepository() = MarkNodeRepository()

    @Provides
    fun provideFaviconRepository() = FaviconRepository()
}