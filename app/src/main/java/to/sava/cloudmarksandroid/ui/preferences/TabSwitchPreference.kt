package to.sava.cloudmarksandroid.ui.preferences

import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.launch
import to.sava.cloudmarksandroid.dataStore

@Composable
fun TabSwitchPreference(
    key: Preferences.Key<Int>,
    defaultValue: Int,
    tabs: List<Pair<Int, String>>,
    modifier: Modifier = Modifier,
    onChange: (value: Int) -> Unit = {},
    onChangeCancellable: (value: Int, cancel: () -> Unit) -> Unit = { _, _ -> },
    content: @Composable (selectedTabIndex: Int) -> Unit,
) {
    var selectedTabIndex by remember { mutableIntStateOf(defaultValue) }

    val dataStore = LocalContext.current.dataStore
    val prefs by remember { dataStore.data }.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    LaunchedEffect(prefs) {
        prefs?.get(key)?.also { storedValue ->
            // タブのインデックスが範囲内にあるか確認
            val tabIndices = tabs.map { it.first }
            selectedTabIndex = if (storedValue in tabIndices) {
                storedValue
            } else {
                // 存在しないインデックスの場合は最初のタブを使用
                tabIndices.firstOrNull() ?: defaultValue
            }
        }
    }

    TabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == selectedTabIndex }.let {
            if (it >= 0) it else 0
        },
        modifier = modifier,
    ) {
        tabs.forEach { (index, title) ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = {
                    var cancel = false
                    onChange(index)
                    onChangeCancellable(index) { cancel = true }
                    if (!cancel) {
                        selectedTabIndex = index
                        scope.launch {
                            dataStore.edit { settings ->
                                settings[key] = index
                            }
                        }
                    }
                },
                text = {
                    Text(title)
                }
            )
        }
    }

    content(selectedTabIndex)
}
