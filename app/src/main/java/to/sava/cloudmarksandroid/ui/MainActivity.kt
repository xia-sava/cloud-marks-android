package to.sava.cloudmarksandroid.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import to.sava.cloudmarksandroid.BuildConfig
import to.sava.cloudmarksandroid.CloudMarksAndroidApplication
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.modules.*
import to.sava.cloudmarksandroid.ui.theme.CloudMarksAndroidTheme
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CloudMarksAndroidTheme {
                MainPage()
            }
        }
    }
}

@Composable
fun MainPage(modifier: Modifier = Modifier) {
    val viewModel: MainPageViewModel = hiltViewModel()

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()
    val navBackStack by navController.currentBackStackEntryAsState()

    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    val marksWorkerRunning by viewModel.marksWorkerRunning.collectAsState(false)
    val lastOpenedMarkId by viewModel.lastOpenedId.collectAsState(null)
    val lastOpenedTime by viewModel.lastOpenedTime.collectAsState("")
    val isGoogleConnected by viewModel.isGoogleConnected.collectAsState(false)

    val markId = lastOpenedMarkId ?: return

    Scaffold(
        topBar = {
            CloudMarksTopAppBar(
                showBackButton = (
                        navBackStack?.destination?.route != "marks/{markId}"
                                || markId != MarkNode.ROOT_ID
                        ),
                disableSettingsMenu = navBackStack?.destination?.route == "settings",
                onClickSettings = { navController.navigate("settings") },
                disableLoadMenu = marksWorkerRunning || !isGoogleConnected,
                onClickLoad = { viewModel.loadMarks(lifecycleOwner) },
                onClickAbout = { showAboutDialog = true },
                onClickBack = { viewModel.back() },
            )
        },
        modifier = modifier,
        scaffoldState = scaffoldState,
    ) {
        NavHost(navController, startDestination = "marks/{markId}") {
            composable(
                "marks/{markId}",
                arguments = listOf(
                    navArgument("markId") {
                        type = NavType.LongType
                        defaultValue = markId
                    },
                )
            ) { backStackEntry ->
                MarksScreen(
                    hiltViewModel<MarksScreenViewModel>().apply {
                        showMessage = { message ->
                            viewModel.showMessage(message)
                        }
                        onSelectFolder = { selectedId ->
                            viewModel.setLastOpenedId(selectedId)
                            navController.navigate("marks/$selectedId")
                        }
                        onCopyToClipboard = { copyText, typeText ->
                            clipboardManager.setText(AnnotatedString(copyText))
                            showMessage("${typeText}をクリップボードにコピーしました。")
                        }
                        openMark = { url ->
                            uriHandler.openUri(url)
                        }
                        shareMark = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.packageManager.queryIntentActivities(
                                intent,
                                PackageManager.MATCH_ALL,
                            ).map {
                                Intent(intent).apply {
                                    setPackage(it.activityInfo.packageName)
                                }
                            }.let {
                                Intent.createChooser(Intent(), "Share to ...").apply {
                                    putExtra(Intent.EXTRA_INITIAL_INTENTS, it.toTypedArray())
                                }.let {
                                    context.startActivity(it)
                                }
                            }
                        }
                        fetchFavicon = { domains ->
                            viewModel.fetchFavicon(domains)
                        }
                        backButton = {
                            viewModel.back()
                        }
                    },
                    backStackEntry.arguments?.getLong("markId") ?: markId,
                    lastOpenedTime,
                )
            }
            composable("settings") {
                Settings()
            }
        }
    }
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text("Cloud Marks Android")
            },
            text = {
                Text("Version: ${BuildConfig.VERSION_NAME}")
            },
            confirmButton = {
                TextButton(
                    onClick = { showAboutDialog = false }
                ) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
private fun CloudMarksTopAppBar(
    showBackButton: Boolean = false,
    disableSettingsMenu: Boolean = false,
    onClickSettings: () -> Unit = {},
    disableLoadMenu: Boolean = false,
    onClickLoad: () -> Unit = {},
    onClickAbout: () -> Unit = {},
    onClickBack: () -> Unit = {},
) {
    var showMenu by mutableStateOf(false)

    TopAppBar(
        title = { Text("Cloud Marks Android") },
        navigationIcon = if (showBackButton) {
            {
                IconButton(onClick = onClickBack) {
                    Icon(Icons.Filled.ArrowBack, "Back")
                }
            }
        } else {
            {
                Image(
                    painterResource(R.drawable.ic_launcher),
                    "Cloud Marks Android",
                )
            }
        },
        actions = {
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(Icons.Filled.Menu, "Menu")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    onClick = {
                        showMenu = false
                        onClickSettings()
                    },
                    enabled = !disableSettingsMenu,
                ) {
                    Text("Settings")
                }
                DropdownMenuItem(
                    onClick = {
                        showMenu = false
                        onClickLoad()
                    },
                    enabled = !disableLoadMenu,
                ) {
                    Text("Load")
                }
                DropdownMenuItem(
                    onClick = {
                        showMenu = false
                        onClickAbout()
                    },
                    enabled = true,
                ) {
                    Text("About ...")
                }
            }
        }
    )
}


@HiltViewModel
private class MainPageViewModel @Inject constructor(
    private val settings: Settings,
    private val marks: Marks,
) : ViewModel() {
    val lastOpenedId = settings.getLastOpenedMarkId()
    val isGoogleConnected = settings.isGoogleConnected()

    private var _marksWorkerRunning = MutableStateFlow(false)
    val marksWorkerRunning get() = _marksWorkerRunning.asStateFlow()

    private var _lastOpenedTime = MutableStateFlow(LocalTime.now().toString())
    val lastOpenedTime get() = _lastOpenedTime.asStateFlow()

    fun showMessage(message: String) {
        CloudMarksAndroidApplication.instance.toast(message)
    }

    fun back() {
        viewModelScope.launch {
            settings.getLastOpenedMarkIdValue()
                .let { marks.getMark(it) }
                ?.let { marks.getMarkPath(it) }
                ?.takeIf { it.size > 1 }
                ?.takeLast(2)
                ?.first()
                ?.let {
                    setLastOpenedId(it.id)
                }
        }
    }

    fun loadMarks(lifecycleOwner: LifecycleOwner) {
        _marksWorkerRunning.value = true
        enqueueMarkLoader(MarkWorker.Action.LOAD, lifecycleOwner) {
            _marksWorkerRunning.value = false
            refresh()
        }
    }

    fun fetchFavicon(domains: List<String>) {
        viewModelScope.launch {
            marks.fetchFavicons(domains, onFetched = { refresh() })
        }
    }

    fun setLastOpenedId(markId: Long) {
        viewModelScope.launch {
            settings.setLastOpenedMarkId(markId)
        }
    }

    private fun refresh() {
        _lastOpenedTime.value = LocalTime.now().toString()
    }
}
