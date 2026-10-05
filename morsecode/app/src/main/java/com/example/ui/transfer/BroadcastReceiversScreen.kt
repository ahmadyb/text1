package com.example.ui.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Peer
import com.example.ui.components.PeerHeaderCard
import com.example.ui.components.TransferActionBar
import com.example.ui.theme.ColorOk
import com.example.ui.theme.ColorRecv
import com.example.ui.theme.ColorRecvBg
import com.example.ui.theme.LocalMorsecodeColors
import com.example.viewmodel.MorsecodeViewModel
import com.example.viewmodel.ScreenState

@Composable
fun BroadcastReceiversScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val scrollState = rememberScrollState()
    val peers by viewModel.broadcastPeers.collectAsState()

    Column(modifier = modifier.fillMaxSize().testTag("broadcast_receivers_screen")) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(ScreenState.BROADCAST_SENDER) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.t1)
                }
                Text("Broadcast receivers", color = colors.t1, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.showToast("Options") }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = colors.t2)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PeerHeaderCard(
                peer = Peer("m", "MYA-L10", "M", "#F59E0B", "Broadcast · Phone · LAN", "FROM"),
                statusText = "Broadcast · Phone · LAN"
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("RECEIVING INDEPENDENT SESSIONS", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            val displayPeers = if (peers.isEmpty()) {
                listOf(
                    Peer("r", "Ravi's Redmi", "R", "#8B5CF6", "62% · 4.8 MB/s", "LAN"),
                    Peer("p", "Pixel 7X", "P", "#0EA5E9", "88% · 5.4 MB/s", "LAN"),
                    Peer("s", "Samsung A14", "S", "#22C55E", "35% · 4.0 MB/s", "LAN")
                )
            } else peers

            displayPeers.forEach { peer ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.card)
                        .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(peer.colorHex))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(peer.letter, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(peer.name, color = colors.t1, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(peer.sub, color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ColorRecvBg)
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text("RECV", color = ColorRecv, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.pressed)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (peer.id == "r") 0.62f else if (peer.id == "p") 0.88f else 0.35f)
                                .height(4.dp)
                                .background(colors.acc)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.acc.copy(alpha = 0.12f))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Each receiver has an isolated session. One slow phone never stalls the others.",
                    color = colors.t2,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        TransferActionBar(
            isPaused = false,
            onAddFiles = { viewModel.navigateTo(ScreenState.FILES) },
            onTogglePause = { viewModel.togglePauseAll() },
            onBackground = { viewModel.backgroundSession() },
            onEndSession = { viewModel.endTransferSession() }
        )
    }
}
