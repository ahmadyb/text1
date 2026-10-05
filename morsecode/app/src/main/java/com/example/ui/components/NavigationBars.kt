package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ColorErr
import com.example.ui.theme.LocalMorsecodeColors

enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    CONNECT("connect", "Connect", Icons.Default.TrackChanges),
    FILES("files", "Files", Icons.Default.Folder),
    HISTORY("history", "History", Icons.Default.History),
    SETTINGS("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MorsecodeBottomNav(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = colors.line, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(colors.card)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppDestination.entries.forEach { dest ->
                val isSelected = dest == currentDestination
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(dest) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colors.acc.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = dest.label,
                            tint = if (isSelected) colors.acc else colors.t3,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = dest.label,
                        color = if (isSelected) colors.acc else colors.t3,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun TransferActionBar(
    isPaused: Boolean,
    onAddFiles: () -> Unit,
    onTogglePause: () -> Unit,
    onBackground: () -> Unit,
    onEndSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = colors.line, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(colors.raised)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add files
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAddFiles() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.acc.copy(alpha = 0.16f))
                        .padding(horizontal = 14.dp, vertical = 3.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add files", tint = colors.acc, modifier = Modifier.size(18.dp))
                }
                Text("Add files", color = colors.acc, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }

            // Pause all / Resume all
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTogglePause() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume all" else "Pause all",
                        tint = colors.t2,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(if (isPaused) "Resume all" else "Pause all", color = colors.t2, fontSize = 10.sp)
            }

            // Background
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onBackground() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 3.dp)
                ) {
                    Icon(Icons.Default.Minimize, contentDescription = "Background", tint = colors.t2, modifier = Modifier.size(18.dp))
                }
                Text("Background", color = colors.t2, fontSize = 10.sp)
            }

            // End
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEndSession() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 3.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "End session", tint = ColorErr, modifier = Modifier.size(18.dp))
                }
                Text("End", color = ColorErr, fontSize = 10.sp)
            }
        }
    }
}
