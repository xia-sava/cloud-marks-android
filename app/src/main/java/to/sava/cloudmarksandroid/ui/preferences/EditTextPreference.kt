package to.sava.cloudmarksandroid.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.launch
import to.sava.cloudmarksandroid.dataStore


@Composable
fun EditTextPreference(
    key: Preferences.Key<String>,
    label: String,
    defaultValue: String,
    modifier: Modifier = Modifier,
    onChange: (value: String) -> Unit = {},
    onChangeCancellable: (value: String, cancel: () -> Unit) -> Unit = {_, _ -> },
) {
    val dataStore = LocalContext.current.dataStore
    val prefs by remember { dataStore.data }.collectAsState(initial = null)
    var value by remember { mutableStateOf(defaultValue) }
    var editMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(prefs) {
        prefs?.get(key)?.also {
            value = it
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { editMode = true }
    ) {
        Text(label)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Text(
                value,
                color = Color.DarkGray,
                fontSize = 10.sp,
            )
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            if (editMode) {
                var inputValue by remember { mutableStateOf(value) }
                AlertDialog(
                    onDismissRequest = { editMode = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                editMode = false
                                var cancel = false
                                onChange(inputValue)
                                onChangeCancellable(inputValue) { cancel = true }
                                if (!cancel) {
                                    value = inputValue
                                    scope.launch {
                                        dataStore.edit { settings ->
                                            settings[key] = value
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                editMode = false
                            }
                        ) {
                            Text("CANCEL")
                        }
                    },
                    title = {
                        Text(label)
                        Spacer(Modifier.height(16.dp))
                    },
                    text = {
                        TextField(
                            value = inputValue,
                            onValueChange = { inputValue = it.trim() },
                            singleLine = true,
                        )
                    },
                )
            }
        }
    }
}
