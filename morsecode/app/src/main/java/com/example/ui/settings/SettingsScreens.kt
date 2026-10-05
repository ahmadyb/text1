package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.formatBytes
import com.example.ui.theme.AccentColor
import com.example.ui.theme.ColorErr
import com.example.ui.theme.ColorInfo
import com.example.ui.theme.ColorOk
import com.example.ui.theme.ColorWarn
import com.example.ui.theme.LocalMorsecodeColors
import com.example.viewmodel.MorsecodeViewModel
import com.example.viewmodel.ScreenState

@Composable
fun SettingsScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val scrollState = rememberScrollState()

    val isDark by viewModel.isDarkTheme.collectAsState()
    val accent by viewModel.currentAccent.collectAsState()
    val followSystem by viewModel.followSystemTheme.collectAsState()
    val sounds by viewModel.soundEffectsEnabled.collectAsState()
    val notif by viewModel.notificationsEnabled.collectAsState()
    val conflict by viewModel.conflictPolicy.collectAsState()
    val peersLimit by viewModel.broadcastPeersLimit.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("Settings", color = colors.t1, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(14.dp))

        // Device profile card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.acc.copy(alpha = 0.14f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.acc),
                contentAlignment = Alignment.Center
            ) {
                Text("M", color = colors.accInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("MYA-L10", color = colors.t1, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Tap to rename · Sunflower avatar", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text("ACCENT COLOUR", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AccentColor.entries.forEach { acc ->
                val isSelected = accent == acc
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(acc.primary)
                        .border(if (isSelected) 2.5.dp else 0.dp, colors.t1, CircleShape)
                        .clickable { viewModel.currentAccent.value = acc }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text("APPEARANCE", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        SettingSwitchRow("Dark mode", "Sunflower hue", isDark, onToggle = { viewModel.isDarkTheme.value = it })
        SettingSwitchRow("Follow system", "Match the phone setting", followSystem, onToggle = { viewModel.followSystemTheme.value = it })
        SettingSwitchRow("Sounds", "Connect · fail · success", sounds, onToggle = { viewModel.soundEffectsEnabled.value = it })

        Spacer(modifier = Modifier.height(18.dp))

        Text("TRANSFER", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        SettingClickableRow("Conflict policy", conflict, onClick = { viewModel.cycleConflictPolicy() })
        SettingSwitchRow("Notifications", "Transfer progress", notif, onToggle = { viewModel.notificationsEnabled.value = it })
        SettingClickableRow("Broadcast peers", "Maximum $peersLimit phones", onClick = { viewModel.cycleBroadcastLimit() })

        Spacer(modifier = Modifier.height(18.dp))

        Text("SYSTEM", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        SettingClickableRow("Storage access", "Download · SAF folders", onClick = { viewModel.showToast("Granted: Download, DCIM") })
        SettingClickableRow("Battery optimization", "Exemption state", onClick = { viewModel.navigateTo(ScreenState.DOCTOR) })
        SettingClickableRow("Logs", "View · export .txt · clear", onClick = { viewModel.navigateTo(ScreenState.LOGS) })
        SettingClickableRow("Crash reports", "3 local reports · export or clear", onClick = { viewModel.navigateTo(ScreenState.CRASHES) })

        Spacer(modifier = Modifier.height(18.dp))

        Text("DIAGNOSTICS & ABOUT", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        SettingClickableRow("Connection Doctor", "Wi-Fi · Multicast · Play Services", onClick = { viewModel.navigateTo(ScreenState.DOCTOR) })
        SettingClickableRow("Replay onboarding", "Show the tour again", onClick = {
            viewModel.onboardingStep.value = 0
            viewModel.navigateTo(ScreenState.ONBOARDING)
        })
        SettingClickableRow("Help & FAQ", "Questions and guides", onClick = { viewModel.navigateTo(ScreenState.HELP) })

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Morsecode 1.0.0 (1) · no accounts, no cloud, no ads",
            color = colors.t3,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }
}

@Composable
fun SettingSwitchRow(title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = LocalMorsecodeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.t1, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = colors.t3, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.accInk,
                checkedTrackColor = colors.acc,
                uncheckedThumbColor = colors.t3,
                uncheckedTrackColor = colors.pressed
            )
        )
    }
    HorizontalDivider(color = colors.line, thickness = 1.dp)
}

@Composable
fun SettingClickableRow(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = LocalMorsecodeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.t1, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.t3, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = colors.line, thickness = 1.dp)
}

@Composable
fun LogsScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val logs by viewModel.localLogs.collectAsState()
    val errorsOnly by viewModel.logsErrorOnly.collectAsState()

    val filtered = if (errorsOnly) logs.filter { it.level != "INFO" } else logs

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(ScreenState.SETTINGS) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.t1)
            }
            Text("Logs", color = colors.t1, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                viewModel.localLogs.value = emptyList()
                viewModel.showToast("Log cleared")
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = colors.t2)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    viewModel.shareSheetTitle.value = "morsecode-log.txt"
                    viewModel.shareSheetSubtitle.value = "12.4 KB · Morsecode 1.0.0 (1)"
                    viewModel.isShareSheetOpen.value = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.acc, contentColor = colors.accInk),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export .txt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { viewModel.logsErrorOnly.value = !errorsOnly },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (errorsOnly) colors.acc.copy(alpha = 0.2f) else colors.raised,
                    contentColor = if (errorsOnly) colors.acc else colors.t1
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Errors only", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            filtered.forEach { log ->
                val lvlColor = when (log.level) {
                    "INFO" -> ColorInfo
                    "WARN" -> ColorWarn
                    else -> ColorErr
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(log.timestamp, color = colors.t3, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(52.dp))
                    Text(log.level, color = lvlColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
                    Text(log.message, color = colors.t1, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                }
                HorizontalDivider(color = colors.line, thickness = 0.5.dp)
            }
        }

        Text(
            text = "Header of every export: Morsecode 1.0.0 (1)",
            color = colors.t3,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun CrashesScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val crashes by viewModel.localCrashes.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(ScreenState.SETTINGS) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.t1)
            }
            Text("Crash reports", color = colors.t1, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.acc.copy(alpha = 0.12f))
                .padding(14.dp)
        ) {
            Column {
                Text("Stored only on this device", color = colors.t1, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Reports are never uploaded automatically. Review or export them as a text file.", color = colors.t3, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    viewModel.shareSheetTitle.value = "morsecode-crash-reports.txt"
                    viewModel.shareSheetSubtitle.value = "${crashes.size} reports · local diagnostic data"
                    viewModel.isShareSheetOpen.value = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.acc, contentColor = colors.accInk),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export .txt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    viewModel.localCrashes.value = emptyList()
                    viewModel.showToast("Crash reports cleared")
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.raised, contentColor = colors.t1),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Clear", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            crashes.forEach { crash ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.card)
                        .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ColorErr.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("CRASH", color = ColorErr, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(crash.whereText, color = colors.t1, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(crash.whenText, color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(crash.error, color = ColorErr, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(crash.note, color = colors.t3, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun HistoryScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val currentTab by viewModel.historyTab.collectAsState()
    val received by viewModel.receivedHistory.collectAsState()
    val sent by viewModel.sentHistory.collectAsState()

    val currentItems = if (currentTab == "received") received else sent

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("History", color = colors.t1, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.showToast("Search history") }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = colors.t2)
            }
            IconButton(onClick = { viewModel.showToast("Clear history?") }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = colors.t2)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Segmented Control Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(colors.raised)
                .padding(4.dp)
        ) {
            Button(
                onClick = { viewModel.historyTab.value = "received" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentTab == "received") colors.acc else Color.Transparent,
                    contentColor = if (currentTab == "received") colors.accInk else colors.t2
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.weight(1f).height(38.dp)
            ) {
                Text("Received", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            Button(
                onClick = { viewModel.historyTab.value = "sent" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentTab == "sent") colors.acc else Color.Transparent,
                    contentColor = if (currentTab == "sent") colors.accInk else colors.t2
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.weight(1f).height(38.dp)
            ) {
                Text("Sent", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("YESTERDAY", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            currentItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.raised),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = if (item.kind == "video") Color(0xFF0EA5E9) else Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.fileName, color = colors.t1, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("${item.peerName} · ${formatBytes(item.sizeBytes)} · ${item.timeFormatted}", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    Icon(
                        imageVector = if (item.status == "ok") Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (item.status == "ok") ColorOk else ColorErr,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(onClick = { viewModel.showToast("Open · Share · Send again · Remove") }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = colors.t3, modifier = Modifier.size(18.dp))
                    }
                }
                HorizontalDivider(color = colors.line, thickness = 1.dp)
            }
        }
    }
}

@Composable
fun WebShareControlScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val isRunning = viewModel.webShareServer.isRunning
    val ip = viewModel.webShareServer.getLocalIpAddress()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(ScreenState.CONNECT) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.t1)
            }
            Text("WebShare", color = colors.t1, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.navigateTo(ScreenState.HELP) }) {
                Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = colors.t2)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.acc.copy(alpha = 0.12f))
                .border(1.dp, colors.acc.copy(alpha = 0.38f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isRunning) ColorOk.copy(alpha = 0.2f) else colors.raised)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isRunning) "● RUNNING" else "○ STOPPED",
                        color = if (isRunning) ColorOk else colors.t3,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.raised)
                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        .clickable { viewModel.showToast("Address copied to clipboard") }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$ip:33455", color = colors.acc, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = colors.t3, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Tap to copy · open it in any browser on the same network", color = colors.t3, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.toggleWebShare() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) colors.raised else colors.acc,
                        contentColor = if (isRunning) colors.t1 else colors.accInk
                    ),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(if (isRunning) "Stop WebShare" else "Start WebShare", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Stays on until you stop it — screen-off and idle never close it.", color = colors.t3, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("SESSIONS", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B)),
                contentAlignment = Alignment.Center
            ) {
                Text("O", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Chrome · Office Laptop", color = colors.t1, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("192.168.1.88 · accepted 4 min ago", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ColorOk.copy(alpha = 0.2f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text("LIVE", color = ColorOk, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("IF A BROWSER CAN'T CONNECT", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        listOf(
            "Both devices must be on the same Wi-Fi",
            "Turn off client / AP isolation on the router",
            "Try the phone's hotspot below",
            "WebShare never idles out — only Stop closes it"
        ).forEach { tip ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = ColorOk, modifier = Modifier.size(15.dp).padding(top = 2.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(tip, color = colors.t2, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("HOTSPOT", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.card)
                .padding(14.dp)
        ) {
            Text("Use the phone's hotspot", color = colors.t1, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("No shared Wi-Fi? Start the hotspot and let the laptop join it.", color = colors.t3, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { viewModel.showToast("Hotspot: SSID MorsecodeAP · key 8f2c1d90") },
                colors = ButtonDefaults.buttonColors(containerColor = colors.raised, contentColor = colors.t1),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text("Start hotspot", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    viewModel: MorsecodeViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalMorsecodeColors.current
    val sheetState = rememberModalBottomSheetState()
    val curKey by viewModel.sortKey.collectAsState()
    val isAsc by viewModel.sortAscending.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Sort by", color = colors.t1, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Date modified · newest first", color = colors.t3, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))

            listOf(
                Pair("date", "Date modified"),
                Pair("name", "Name"),
                Pair("size", "Size"),
                Pair("type", "Type")
            ).forEach { (k, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.sortKey.value = k }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = if (curKey == k) colors.acc else colors.t1, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    if (curKey == k) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = colors.acc)
                    }
                }
                HorizontalDivider(color = colors.line, thickness = 1.dp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.sortAscending.value = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isAsc) colors.acc else colors.raised,
                        contentColor = if (!isAsc) colors.accInk else colors.t1
                    ),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Descending", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.sortAscending.value = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAsc) colors.acc else colors.raised,
                        contentColor = if (isAsc) colors.accInk else colors.t1
                    ),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Ascending", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.raised, contentColor = colors.t1),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Done", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    viewModel: MorsecodeViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalMorsecodeColors.current
    val sheetState = rememberModalBottomSheetState()
    val title by viewModel.shareSheetTitle.collectAsState()
    val subtitle by viewModel.shareSheetSubtitle.collectAsState()

    val targets = listOf(
        Triple("Morsecode", Color(0xFFFACC15), Color.Black),
        Triple("Gmail", Color(0xFFEA4335), Color.White),
        Triple("Drive", Color(0xFF1A73E8), Color.White),
        Triple("WhatsApp", Color(0xFF25D366), Color.Black),
        Triple("Telegram", Color(0xFF29A9EB), Color.White),
        Triple("Bluetooth", Color(0xFF0EA5E9), Color.White),
        Triple("Files", Color(0xFF8B5CF6), Color.White),
        Triple("More", Color(0xFF3A3A3A), Color.White)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Share", color = colors.t1, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("$title · $subtitle", color = colors.t3, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

            val chunked = targets.chunked(4)
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    row.forEach { (name, bg, fg) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                viewModel.showToast("Shared $title → $name")
                                onDismiss()
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name.take(1), color = fg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(name, color = colors.t1, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModel.showToast("Copied to clipboard")
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.raised, contentColor = colors.t1),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Copy", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        viewModel.showToast("Shared → Nearby")
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.raised, contentColor = colors.t1),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Nearby", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.raised, contentColor = colors.t1),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        }
    }
}
