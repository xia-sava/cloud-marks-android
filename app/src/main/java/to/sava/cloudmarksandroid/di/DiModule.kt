package to.sava.cloudmarksandroid.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.viewModel
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module
import to.sava.cloudmarksandroid.CloudMarksAndroidApplication
import to.sava.cloudmarksandroid.dataStore
import to.sava.cloudmarksandroid.databases.CloudMarksAndroidDatabase
import to.sava.cloudmarksandroid.databases.dao.FaviconDao
import to.sava.cloudmarksandroid.databases.dao.MarkNodeDao
import to.sava.cloudmarksandroid.databases.repositories.FaviconRepository
import to.sava.cloudmarksandroid.databases.repositories.MarkNodeRepository
import to.sava.cloudmarksandroid.modules.MarkWorker
import to.sava.cloudmarksandroid.modules.Marks
import to.sava.cloudmarksandroid.modules.Settings
import to.sava.cloudmarksandroid.modules.createHttpClient
import to.sava.cloudmarksandroid.update.Updater
import to.sava.cloudmarksandroid.ui.MainPageViewModel
import to.sava.cloudmarksandroid.ui.MarksScreenViewModel
import to.sava.cloudmarksandroid.ui.SettingsViewModel


fun appModule() = module {
    single<Context> { CloudMarksAndroidApplication.instance }
    single<CloudMarksAndroidDatabase> { CloudMarksAndroidApplication.database }
    single<DataStore<Preferences>> { get<Context>().dataStore }
    single<MarkNodeDao> { get<CloudMarksAndroidDatabase>().markNodeDao() }
    single<FaviconDao> { get<CloudMarksAndroidDatabase>().faviconDao() }
    single<HttpClient> { createHttpClient() }
    single { Settings(get()) }
    single { MarkNodeRepository(get()) }
    single { FaviconRepository(get(), get()) }
    single { Marks(get(), get(), get()) }
    single { Updater(get(), get()) }

    viewModel { MainPageViewModel(get(), get()) }
    viewModel { MarksScreenViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }

    worker { MarkWorker(get(), get<Context>(), get()) }
}
