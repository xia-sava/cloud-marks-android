package to.sava.cloudmarksandroid.ui.preferences

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.launch
import to.sava.cloudmarksandroid.ui.dataStore


@Composable
fun SwitchPreference(
    keyString: String,
    label: String,
    defaultValue: Boolean,
    modifier: Modifier = Modifier,
    onChange: (value: Boolean) -> Unit = {},
    onChangeCancellable: (value: Boolean, cancel: () -> Unit) -> Unit = { _, _ -> },
) {
    val key = stringPreferencesKey(keyString)
    val dataStore = LocalContext.current.dataStore
    val prefs by remember { dataStore.data }.collectAsState(initial = null)
    var value by remember { mutableStateOf(defaultValue) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(prefs) {
        prefs?.get(key)?.also {
            value = it.toBooleanStrictOrNull() ?: false
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
                                settings[key] = value.toString()
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
