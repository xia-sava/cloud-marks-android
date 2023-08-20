package to.sava.cloudmarksandroid.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import to.sava.cloudmarksandroid.BuildConfig
import to.sava.cloudmarksandroid.CloudMarksAndroidApplication
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.modules.MarkWorker
import to.sava.cloudmarksandroid.modules.Marks
import to.sava.cloudmarksandroid.modules.Settings
import to.sava.cloudmarksandroid.modules.enqueueMarkLoader
import to.sava.cloudmarksandroid.modules.toast
import to.sava.cloudmarksandroid.ui.theme.CloudMarksAndroidTheme
import java.time.LocalTime

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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainPage(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<MainPageViewModel>()

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
    val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) else null

    val markId = lastOpenedMarkId ?: return
    var showPermissionDialog by remember { mutableStateOf(false) }

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
                onClickLoad = {
                    if (permissionState == null || permissionState.hasPermission) {
                        viewModel.loadMarks(lifecycleOwner)
                    } else {
                        showPermissionDialog = true
                    }
                },
                onClickAbout = { showAboutDialog = true },
                onClickBack = {
                    if (navBackStack?.destination?.route == "settings") {
                        navController.popBackStack()
                    } else {
                        viewModel.back()
                    }
                },
            )
        },
        modifier = modifier,
        scaffoldState = scaffoldState,
    ) { innerPadding ->
        NavHost(
            navController,
            modifier = Modifier.padding(innerPadding),
            startDestination = "marks/{markId}",
        ) {
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
                    markId = backStackEntry.arguments?.getLong("markId") ?: markId,
                    lastOpenedTime = lastOpenedTime,
                    viewModelConfigurator = {
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
                        onMarkRead = { mark ->
                            viewModel.setMarkReadToHere(mark.id)
                            showMessage("${mark.title}にここまで読んだマークを付けました。")
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
                    }
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
    if (showPermissionDialog && permissionState != null) {
        AlertDialog(
            onDismissRequest = {
                showPermissionDialog = false
            },
            title = {
                Text("進捗通知のための権限付与")
            },
            text = {
                Text("進捗通知を表示するために通知権限を許可してください")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        permissionState.launchPermissionRequest()
                    }
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
    var showMenu by remember { mutableStateOf(false) }

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
                    painterResource(R.drawable.ic_cloud_marks),
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


class MainPageViewModel(
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
                ?: setLastOpenedId(MarkNode.ROOT_ID)
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

    fun setMarkReadToHere(markId: Long) {
        viewModelScope.launch {
            settings.setMarkReadToHere(markId)
        }
    }

    private fun refresh() {
        _lastOpenedTime.value = LocalTime.now().toString()
    }
}
