package to.sava.cloudmarksandroid.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Divider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.launch
import to.sava.cloudmarksandroid.dataStore


@Composable
fun SwitchPreference(
    key: Preferences.Key<Boolean>,
    label: String,
    defaultValue: Boolean,
    modifier: Modifier = Modifier,
    onChange: (value: Boolean) -> Unit = {},
    onChangeCancellable: (value: Boolean, cancel: () -> Unit) -> Unit = { _, _ -> },
) {
    val dataStore = LocalContext.current.dataStore
    val prefs by remember { dataStore.data }.collectAsState(initial = null)
    var value by remember { mutableStateOf(defaultValue) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(prefs) {
        prefs?.get(key)?.also {
            value = it
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                label,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )
            Switch(
                value,
                onCheckedChange = {
                    var cancel = false
                    onChange(it)
                    onChangeCancellable(it) { cancel = true }
                    if (!cancel) {
                        value = it
                        scope.launch {
                            dataStore.edit { settings ->
                                settings[key] = value
                            }
                        }
                    }
                },
            )
        }
        Divider()
        Spacer(modifier = Modifier.height(8.dp))
    }
}
