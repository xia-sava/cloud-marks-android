package to.sava.cloudmarksandroid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.dataStore
import to.sava.cloudmarksandroid.modules.PreferenceKeys
import to.sava.cloudmarksandroid.modules.Settings
import to.sava.cloudmarksandroid.modules.storageFactory

private enum class AwsS3LoadingStatus {
    NORMAL, ERROR, LOADING
}

@Composable
fun AwsS3ConnectionPreference(
    key: Preferences.Key<Boolean>,
    label: String,
    defaultValue: Boolean,
    settings: Settings,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dataStore = context.dataStore

    val prefs by remember { dataStore.data }.collectAsState(initial = null)
    var runConnectionProcess by rememberSaveable { mutableStateOf(false) }
    var loadingState by rememberSaveable { mutableStateOf(AwsS3LoadingStatus.NORMAL) }
    var connected by rememberSaveable { mutableStateOf(defaultValue) }
    var accessKeyId by rememberSaveable { mutableStateOf("") }
    var secretAccessKey by rememberSaveable { mutableStateOf("") }
    var region by rememberSaveable { mutableStateOf("") }
    var bucketName by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(prefs) {
        prefs?.get(PreferenceKeys.AWS_S3_ACCESS_KEY_ID)?.let { accessKeyId = it }
        prefs?.get(PreferenceKeys.AWS_S3_SECRET_ACCESS_KEY)?.let { secretAccessKey = it }
        prefs?.get(PreferenceKeys.AWS_S3_REGION)?.let { region = it }
        prefs?.get(PreferenceKeys.AWS_S3_BUCKET_NAME)?.let { bucketName = it }
    }

    if (runConnectionProcess) {
        LaunchedEffect(connected) {
            if (!connected) {
                loadingState = AwsS3LoadingStatus.LOADING
                val storage = storageFactory(settings)
                try {
                    storage.checkAccessibility()
                    connected = true
                    loadingState = AwsS3LoadingStatus.NORMAL
                } catch (e: Exception) {
                    loadingState = AwsS3LoadingStatus.ERROR
                }
                runConnectionProcess = false

            } else {
                connected = false
                dataStore.edit { it[key] = false }
                runConnectionProcess = false
            }
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
                if (!connected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_icons8_aws_48),
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
            if (loadingState == AwsS3LoadingStatus.LOADING) {
                CircularProgressIndicator(
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                if (connected) "(not connected)" else "",
                color = MaterialTheme.colors.onSecondary,
                fontSize = 10.sp,
            )
            if (loadingState == AwsS3LoadingStatus.ERROR) {
                Text(
                    "Connection failed",
                    color = MaterialTheme.colors.error,
                    fontSize = 10.sp,
                )
            }
        }
        Divider()
        Spacer(modifier = Modifier.height(8.dp))
    }
}
