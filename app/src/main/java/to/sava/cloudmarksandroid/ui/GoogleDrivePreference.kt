package to.sava.cloudmarksandroid.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch
import to.sava.cloudmarksandroid.R

private enum class LoadingStatus {
    NORMAL, ERROR, LOADING
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GoogleDrivePreference(
    keyString: String,
    label: String,
    defaultValue: String,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val key = stringPreferencesKey(keyString)
    val context = LocalContext.current
    val dataStore = context.dataStore

    val prefs by remember { dataStore.data }.collectAsState(initial = null)
    val permissionState = rememberPermissionState(Manifest.permission.GET_ACCOUNTS)
    var runConnectionProcess by rememberSaveable { mutableStateOf(false) }
    var loadingState by rememberSaveable { mutableStateOf(LoadingStatus.NORMAL) }
    var account by rememberSaveable { mutableStateOf(defaultValue) }

    LaunchedEffect(prefs) {
        prefs?.get(key)?.let { account = it }
    }

    val authResultLauncher = rememberLauncherForActivityResult(
        contract = GoogleSignInContract,
    ) { task ->
        loadingState = try {
            task?.getResult(ApiException::class.java)?.email?.let { email ->
                account = email
                scope.launch {
                    dataStore.edit { it[key] = email }
                }
                LoadingStatus.NORMAL
            } ?: run {
                Toast.makeText(context, "Sign in error!", Toast.LENGTH_LONG).show()
                LoadingStatus.ERROR
            }
        } catch (e: ApiException) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()
            LoadingStatus.ERROR
        }
    }

    if (runConnectionProcess) {
        if (account.isEmpty()) {
            when {
                permissionState.hasPermission -> {
                    authResultLauncher.launch(1)
                    runConnectionProcess = false
                }
                else -> {
                    loadingState = LoadingStatus.LOADING
                    SideEffect {
                        permissionState.launchPermissionRequest()
                    }
                }
            }
        } else {
            account = ""
            SideEffect {
                scope.launch {
                    dataStore.edit { it[key] = "" }
                }
            }
            runConnectionProcess = false
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                label,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )
            OutlinedButton(
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = MaterialTheme.colors.background,
                    contentColor = MaterialTheme.colors.onBackground,
                ),
                onClick = { runConnectionProcess = true }
            ) {
                if (account.isEmpty()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_icons8_google_48),
                        contentDescription = "",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Connect")
                } else {
                    Icon(
                        imageVector = Icons.Filled.RemoveCircleOutline,
                        contentDescription = "",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Disconnect")

                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            if (loadingState == LoadingStatus.LOADING) {
                CircularProgressIndicator(
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                account.ifEmpty { "(not connected)" },
                color = MaterialTheme.colors.onSecondary,
                fontSize = 10.sp,
            )
        }
        Divider()
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private val GoogleSignInContract =
    object : ActivityResultContract<Int, Task<GoogleSignInAccount>?>() {
        override fun createIntent(context: Context, input: Int): Intent {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE))
                .build()
            return GoogleSignIn.getClient(context, gso).signInIntent
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Task<GoogleSignInAccount>? {
            return when (resultCode) {
                Activity.RESULT_OK -> {
                    GoogleSignIn.getSignedInAccountFromIntent(intent)
                }
                else -> null
            }
        }
    }
