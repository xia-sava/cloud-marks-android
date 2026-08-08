package to.sava.cloudmarksandroid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import to.sava.cloudmarksandroid.update.UpdateInstallState
import to.sava.cloudmarksandroid.update.UpdateStatus
import to.sava.cloudmarksandroid.update.isRunning

@Composable
fun UpdatePreference(
    currentVersionName: String,
    updatable: Boolean,
    status: UpdateStatus?,
    checking: Boolean,
    installState: UpdateInstallState?,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Current Version",
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )
            Text(
                currentVersionName,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Check for Update",
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )
            OutlinedButton(
                enabled = updatable && !checking && !installState.isRunning,
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = MaterialTheme.colors.background,
                    contentColor = MaterialTheme.colors.onBackground,
                ),
                onClick = onCheck,
            ) {
                if (checking) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Check")
            }
        }
        UpdateStatusRow(updatable, status, onInstall, installState)
        InstallStateRow(installState)
        Divider()
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** 更新確認の結果と、更新があるときの適用ボタンを出す． */
@Composable
private fun UpdateStatusRow(
    updatable: Boolean,
    status: UpdateStatus?,
    onInstall: () -> Unit,
    installState: UpdateInstallState?,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        if (!updatable) {
            Note("配布版のみ更新できます")
            return@Row
        }
        when (status) {
            null -> Unit

            UpdateStatus.UpToDate -> Note("最新です")

            is UpdateStatus.Available -> {
                Note("新しいバージョンがあります: ${status.versionName}")
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    enabled = !installState.isRunning,
                    colors = ButtonDefaults.textButtonColors(
                        backgroundColor = MaterialTheme.colors.background,
                        contentColor = MaterialTheme.colors.onBackground,
                    ),
                    onClick = onInstall,
                ) {
                    Text("Update")
                }
            }

            is UpdateStatus.Failed -> Note(status.reason, MaterialTheme.colors.error)
        }
    }
}

/** 適用の進み具合を出す． */
@Composable
private fun InstallStateRow(installState: UpdateInstallState?) {
    if (installState == null) {
        return
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            when (installState) {
                is UpdateInstallState.Downloading -> Note(downloadingLabel(installState))
                UpdateInstallState.Verifying -> Note("照合しています")
                UpdateInstallState.Launching -> Note("インストーラを起動しました")
                is UpdateInstallState.Failed ->
                    Note(installState.reason, MaterialTheme.colors.error)
            }
        }
        if (installState is UpdateInstallState.Downloading) {
            val fraction = installState.fraction
            if (fraction == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(
                    progress = fraction,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Note(text: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colors.onSecondary) {
    Text(text, color = color, fontSize = 10.sp)
}

/** 受信量を「1.2 MB / 3.4 MB」の形にする．全体長が判らなければ受信量だけを出す． */
private fun downloadingLabel(state: UpdateInstallState.Downloading): String =
    if (state.totalBytes > 0) {
        "${megaBytes(state.receivedBytes)} / ${megaBytes(state.totalBytes)}"
    } else {
        megaBytes(state.receivedBytes)
    }

private fun megaBytes(bytes: Long): String = "%.1f MB".format(bytes / 1024.0 / 1024.0)
