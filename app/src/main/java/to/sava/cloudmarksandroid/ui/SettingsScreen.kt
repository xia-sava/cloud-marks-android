package to.sava.cloudmarksandroid.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import to.sava.cloudmarksandroid.modules.PreferenceDefaults
import to.sava.cloudmarksandroid.modules.PreferenceKeys
import to.sava.cloudmarksandroid.modules.Settings
import to.sava.cloudmarksandroid.ui.preferences.EditTextPreference
import to.sava.cloudmarksandroid.ui.preferences.PreferenceGroup
import to.sava.cloudmarksandroid.ui.preferences.SliderPreference
import to.sava.cloudmarksandroid.update.UpdateInstallState
import to.sava.cloudmarksandroid.update.UpdateStatus
import to.sava.cloudmarksandroid.update.Updater
import to.sava.cloudmarksandroid.update.isRunning


@Composable
fun Settings(
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<SettingsViewModel>()

    val settings = viewModel.settingsInstance.collectAsState()
    val updateStatus = viewModel.updateStatus.collectAsState()
    val checking = viewModel.checking.collectAsState()
    val installState = viewModel.installState.collectAsState()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        ApplicationSettings()

        PreferenceGroup(name = "Application Update") {
            UpdatePreference(
                currentVersionName = viewModel.currentVersionName,
                updatable = viewModel.updatable,
                status = updateStatus.value,
                checking = checking.value,
                installState = installState.value,
                onCheck = viewModel::checkForUpdate,
                onInstall = viewModel::install,
            )
        }

        AWSS3Settings(settings.value)
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
            defaultValue = PreferenceDefaults.FOLDER_COLUMNS,
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
            defaultValue = PreferenceDefaults.AWS_S3_ACCESS_KEY_ID,
        )
        EditTextPreference(
            key = PreferenceKeys.AWS_S3_SECRET_ACCESS_KEY,
            label = "Secret Access Key on AWS S3",
            defaultValue = PreferenceDefaults.AWS_S3_SECRET_ACCESS_KEY,
        )
        EditTextPreference(
            key = PreferenceKeys.AWS_S3_REGION,
            label = "Region on AWS S3",
            defaultValue = PreferenceDefaults.AWS_S3_REGION,
        )
        EditTextPreference(
            key = PreferenceKeys.AWS_S3_BUCKET_NAME,
            label = "Bucket name on AWS S3",
            defaultValue = PreferenceDefaults.AWS_S3_BUCKET_NAME,
        )
        EditTextPreference(
            key = PreferenceKeys.AWS_S3_FOLDER_NAME,
            label = "Folder name on AWS S3",
            defaultValue = PreferenceDefaults.AWS_S3_FOLDER_NAME,
        )
        AwsS3ConnectionPreference(
            key = PreferenceKeys.AWS_S3_CONNECTED,
            label = "AWS S3 Connection",
            defaultValue = PreferenceDefaults.AWS_S3_CONNECTED,
            settings = settings,
        )
    }
}


class SettingsViewModel(
    settings: Settings,
    private val updater: Updater,
) : ViewModel() {
    private val _settingsInstance = MutableStateFlow(settings)
    val settingsInstance get() = _settingsInstance.asStateFlow()

    /** 実行中の版の表示名． */
    val currentVersionName: String get() = updater.currentVersionName

    /** 配布物として動いていなければ更新の操作をさせない． */
    val updatable: Boolean get() = updater.updatable

    private val _updateStatus = MutableStateFlow<UpdateStatus?>(null)
    val updateStatus get() = _updateStatus.asStateFlow()

    private val _checking = MutableStateFlow(false)
    val checking get() = _checking.asStateFlow()

    private val _installState = MutableStateFlow<UpdateInstallState?>(null)
    val installState get() = _installState.asStateFlow()

    /** 更新確認を実行する．実行中なら多重に走らせない． */
    fun checkForUpdate() {
        if (_checking.value || _installState.value.isRunning) {
            return
        }
        _checking.value = true
        _installState.value = null
        viewModelScope.launch {
            try {
                _updateStatus.value = updater.check()
            } finally {
                _checking.value = false
            }
        }
    }

    /** 確認済みの配布物を適用する．実行中なら多重に走らせない． */
    fun install() {
        val available = _updateStatus.value as? UpdateStatus.Available ?: return
        if (_installState.value.isRunning) {
            return
        }
        viewModelScope.launch {
            updater.install(available) { state -> _installState.value = state }
        }
    }
}
