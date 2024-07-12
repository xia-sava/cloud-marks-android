package to.sava.cloudmarksandroid.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.androidx.compose.koinViewModel
import to.sava.cloudmarksandroid.modules.PreferenceKeys
import to.sava.cloudmarksandroid.modules.Services
import to.sava.cloudmarksandroid.modules.Settings
import to.sava.cloudmarksandroid.ui.preferences.EditTextPreference
import to.sava.cloudmarksandroid.ui.preferences.PreferenceGroup
import to.sava.cloudmarksandroid.ui.preferences.SliderPreference
import to.sava.cloudmarksandroid.ui.preferences.TabSwitchPreference


@Composable
fun Settings(
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<SettingsViewModel>()

    val settings = viewModel.settingsInstance.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        ApplicationSettings()

        TabSwitchPreference(
            key = PreferenceKeys.CURRENT_SERVICE,
            defaultValue = Services.GoogleDrive.ordinal,
            tabs = listOf(
                Services.GoogleDrive.ordinal to "Google Drive",
                Services.AwsS3.ordinal to "AWS S3",
            ),
            modifier = Modifier.fillMaxWidth()
        ) { index ->
            when (index) {
                0 -> GoogleDriveSettings()
                1 -> AWSS3Settings(settings.value)
            }
        }
    }
}

@Composable
fun ApplicationSettings() {
    PreferenceGroup(name = "Application Settings") {
        SliderPreference(
            key = PreferenceKeys.FOLDER_COLUMNS,
            label = "Folder Columns",
            minValue = 1,
            maxValue = 5,
            defaultValue = 1,
        )
    }
}

@Composable
fun GoogleDriveSettings() {
    PreferenceGroup(name = "Google Drive Settings") {
        GoogleDriveConnectionPreference(
            key = PreferenceKeys.GOOGLE_DRIVE_ACCOUNT,
            label = "Google Drive Connection",
            defaultValue = "",
        )
        EditTextPreference(
            key = PreferenceKeys.GOOGLE_DRIVE_FOLDER_NAME,
            label = "Folder name on Google Drive",
            defaultValue = "cloud_marks",
        )
    }
}

@Composable
fun AWSS3Settings(
    settings: Settings,
) {
    PreferenceGroup(name = "AWS S3 Settings") {
        EditTextPreference(
            key = PreferenceKeys.AWS_S3_ACCESS_KEY_ID,
            label = "Access Key ID on AWS S3",
            defaultValue = "",
        )
        EditTextPreference(
            key = PreferenceKeys.AWS_S3_SECRET_ACCESS_KEY,
            label = "Secret Access Key on AWS S3",
            defaultValue = "",
        )
        EditTextPreference(
            key = PreferenceKeys.AWS_S3_REGION,
            label = "Region on AWS S3",
            defaultValue = "",
        )
        EditTextPreference(
            key = PreferenceKeys.AWS_S3_BUCKET_NAME,
            label = "Bucket name on AWS S3",
            defaultValue = "",
        )
        EditTextPreference(
            key = PreferenceKeys.AWS_S3_FOLDER_NAME,
            label = "Folder name on AWS S3",
            defaultValue = "cloud_marks",
        )
        AwsS3ConnectionPreference(
            key = PreferenceKeys.AWS_S3_CONNECTED,
            label = "AWS S3 Connection",
            defaultValue = false,
            settings = settings,
        )
    }
}


class SettingsViewModel(
    settings: Settings,
) : ViewModel() {
    private val _settingsInstance = MutableStateFlow(settings)
    val settingsInstance get() = _settingsInstance.asStateFlow()
}
