package to.sava.cloudmarksandroid.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import to.sava.cloudmarksandroid.modules.PreferenceKeys
import to.sava.cloudmarksandroid.ui.preferences.EditTextPreference
import to.sava.cloudmarksandroid.ui.preferences.PreferenceGroup


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
                key = PreferenceKeys.FOLDER_NAME,
                label = "Folder name on storage service",
                defaultValue = "cloud_marks-",
            )
        }
        PreferenceGroup(name = "Google Drive Settings") {
            GoogleDrivePreference(
                key = PreferenceKeys.GOOGLE_DRIVE_ACCOUNT,
                label = "Google Drive Connection",
                defaultValue = "",
            )
        }
    }
}
