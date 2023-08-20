package to.sava.cloudmarksandroid.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
fun SliderPreference(
    key: Preferences.Key<Int>,
    label: String,
    minValue: Int,
    maxValue: Int,
    defaultValue: Int,
    modifier: Modifier = Modifier,
    onChange: (value: Int) -> Unit = {},
    onChangeCancellable: (value: Int, cancel: () -> Unit) -> Unit = { _, _ -> },
) {
    assert(maxValue - minValue >= 1)

    val dataStore = LocalContext.current.dataStore
    val prefs by remember { dataStore.data }.collectAsState(initial = null)
    var sliderValue by remember { mutableFloatStateOf(defaultValue.toFloat()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(prefs) {
        prefs?.get(key)?.also {
            sliderValue = it.toFloat()
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
            Text(
                sliderValue.toInt().toString(),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Slider(
                value = sliderValue,
                steps = maxValue - minValue - 1,
                valueRange = minValue.toFloat()..maxValue.toFloat(),
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    val value = sliderValue.toInt()
                    var cancel = false
                    onChange(value)
                    onChangeCancellable(value) { cancel = true }
                    if (!cancel) {
                        scope.launch {
                            dataStore.edit { settings ->
                                settings[key] = value
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        Divider()
        Spacer(modifier = Modifier.height(8.dp))
    }
}
