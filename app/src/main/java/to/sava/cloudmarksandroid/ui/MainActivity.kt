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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.ui.theme.CloudMarksAndroidTheme

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
    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()
    val navBackStack by navController.currentBackStackEntryAsState()
    Scaffold(
        topBar = {
            CloudMarksTopAppBar(
                showBackButton = navBackStack?.destination?.route != "marks",
                disableSettingsMenu = navBackStack?.destination?.route == "settings",
                onClickSettings = { navController.navigate("settings") },
                disableLoadMenu = false,
                onClickLoad = { /* Load() */ },
                onClickBack = { navController.popBackStack() },
            )
        },
        modifier = modifier,
        scaffoldState = scaffoldState,
    ) {
        NavHost(navController, startDestination = "marks") {
            composable("marks") {
                MarksScreen()
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

