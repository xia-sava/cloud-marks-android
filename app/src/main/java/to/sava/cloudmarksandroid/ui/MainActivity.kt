package to.sava.cloudmarksandroid.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
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
import kotlinx.coroutines.launch
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.modules.MarkWorker
import to.sava.cloudmarksandroid.modules.Settings
import to.sava.cloudmarksandroid.modules.enqueueMarkLoader
import to.sava.cloudmarksandroid.ui.theme.CloudMarksAndroidTheme
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

    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()
    val navBackStack by navController.currentBackStackEntryAsState()
    var runMarksLoader by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val lastOpenedMarkId by viewModel.lastOpenedId.collectAsState(initial = null)

    if (runMarksLoader) {
        LaunchedEffect(MarkNode.ROOT_ID) {
            enqueueMarkLoader(MarkWorker.Action.LOAD, context, lifecycleOwner) {
                runMarksLoader = false
            }
        }
    }

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
                disableLoadMenu = runMarksLoader,
                onClickLoad = { runMarksLoader = true },
                onClickBack = { navController.popBackStack() },
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
                    hiltViewModel(),
                    backStackEntry.arguments?.getLong("markId") ?: markId,
                    onSelectFolder = { selectedId ->
                        viewModel.setLastOpenedId(selectedId)
                        navController.navigate("marks/$selectedId")
                    }
                )
            }
            composable("settings") {
                Settings()
            }
        }
    }
}

@Composable
private fun CloudMarksTopAppBar(
    showBackButton: Boolean = false,
    disableSettingsMenu: Boolean = false,
    onClickSettings: () -> Unit = {},
    disableLoadMenu: Boolean = false,
    onClickLoad: () -> Unit = {},
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
            }
        }
    )
}


@HiltViewModel
private class MainPageViewModel @Inject constructor(
    private val settings: Settings
) : ViewModel() {
    val lastOpenedId = settings.getLastOpenedMarkId()

    fun setLastOpenedId(markId: Long) {
        viewModelScope.launch {
            settings.setLastOpenedMarkId(markId)
        }
    }
}
