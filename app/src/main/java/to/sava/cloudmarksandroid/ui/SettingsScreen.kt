package to.sava.cloudmarksandroid.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import to.sava.cloudmarksandroid.ui.preferences.EditTextPreference
import to.sava.cloudmarksandroid.ui.preferences.PreferenceGroup


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

const val FOLDER_NAME = "folder_name"
const val GOOGLE_DRIVE_ACCOUNT = "google_drive_account"


@Composable
fun Settings(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        PreferenceGroup(name = "Application Settings") {
            EditTextPreference(
                keyString = FOLDER_NAME,
                label = "Folder name on storage service",
                defaultValue = "cloud_marks-",
            )
        }
        PreferenceGroup(name = "Google Drive Settings") {
            GoogleDrivePreference(
                keyString = GOOGLE_DRIVE_ACCOUNT,
                label = "Google Drive Connection",
                defaultValue = "",
            )
        }
    }
}
