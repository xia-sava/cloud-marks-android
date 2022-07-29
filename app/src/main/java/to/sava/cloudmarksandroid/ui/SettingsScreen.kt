package to.sava.cloudmarksandroid.ui

import to.sava.cloudmarksandroid.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun Settings(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        SettingsItem(
            icon = Icons.Filled.Settings,
            label = "Application Settings",
            onClick = { /*TODO*/ }
        )
        SettingsItem(
            painter = painterResource(id = R.drawable.ic_google_drive),
            label = "Google Drive Settings",
            onClick = { /*TODO*/ }
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector? = null,
    painter: Painter? = null,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                )
            } else if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = label,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                )
            }

            Spacer(
                modifier = Modifier
                    .width(16.dp)
            )
            Text(
                text = label,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            )
        }
        Divider(
            color = Color.Gray,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}