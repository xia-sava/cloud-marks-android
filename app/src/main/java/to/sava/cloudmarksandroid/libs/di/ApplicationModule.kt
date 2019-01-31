package to.sava.cloudmarksandroid.libs.di

import dagger.Module
import dagger.Provides
import to.sava.cloudmarksandroid.libs.Marks
import to.sava.cloudmarksandroid.repositories.MarkNodeRepository

@Module
class ApplicationModule {

    @Provides
    fun provideMarks(repo: MarkNodeRepository) = Marks(repo)
}