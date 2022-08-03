package to.sava.cloudmarksandroid.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.common.api.ApiException
import to.sava.cloudmarksandroid.module.GoogleApiContract
import to.sava.cloudmarksandroid.module.SignInGoogleViewModel
import to.sava.cloudmarksandroid.module.SignInGoogleViewModelFactory

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GoogleDriveCheckIn() {
    val context = LocalContext.current

    val permissionState = rememberPermissionState(Manifest.permission.GET_ACCOUNTS)

    var startProcess by rememberSaveable { mutableStateOf(false) }

    val signInViewModel: SignInGoogleViewModel = viewModel(
        factory = SignInGoogleViewModelFactory(context.applicationContext as Application)
    )
    var isError by rememberSaveable { mutableStateOf(false) }
    val state = signInViewModel.googleUser.observeAsState()

    val authResultLauncher = rememberLauncherForActivityResult(
        contract = GoogleApiContract(),
    ) { task ->
        try {
            val gsa = task?.getResult(ApiException::class.java)
            if (gsa != null) {
                signInViewModel.fetchSignInUser(gsa.email, gsa.displayName)
            } else {
                isError = true
            }
        } catch (e: ApiException) {
            throw e
        }
    }

    val granted = (ContextCompat.checkSelfPermission(
        LocalContext.current,
        Manifest.permission.GET_ACCOUNTS
    ) == PackageManager.PERMISSION_GRANTED)
    val label = if (isError) {
        "Error!" + if (!granted) ": not granted" else "granted"
    } else {
        state.value?.let {
            "Google Account: ${it.name} / ${it.email}"
        } ?: if (granted) "Granted" else "not granted"
    }

    if (startProcess) {
        when {
            permissionState.hasPermission -> {
                authResultLauncher.launch(1)
                startProcess = false
            }
            else -> {
                SideEffect {
                    permissionState.launchPermissionRequest()
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            permissionState.hasPermission -> Text("権限は付与済みです")
            permissionState.shouldShowRationale -> Text("権限を許可してください")
            permissionState.permissionRequested -> Text("権限が必要なのです")
        }
        Button(
            onClick = { startProcess = true }
        ) {
            Text("Google Drive")
        }
        Text(label)
    }
}
