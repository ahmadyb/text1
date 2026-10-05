package com.example.ui.transfer

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Peer
import com.example.core.model.TransferState
import com.example.ui.components.PeerHeaderCard
import com.example.ui.components.StatusChip
import com.example.ui.components.TransferActionBar
import com.example.ui.components.TransferRow
import com.example.ui.components.formatBytes
import com.example.ui.components.formatSpeed
import com.example.ui.theme.ColorOk
import com.example.ui.theme.LocalMorsecodeColors
import com.example.viewmodel.MorsecodeViewModel
import com.example.viewmodel.ScreenState

@Composable
fun SendingScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val scrollState = rememberScrollState()
    val sendList by viewModel.sendingList.collectAsState()
    val recvList by viewModel.receivingList.collectAsState()
    val peer by viewModel.currentPeer.collectAsState()

    val isAnyPaused = sendList.any { it.state == TransferState.PAUSED } || recvList.any { it.state == TransferState.PAUSED }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(ScreenState.CONNECT) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.t1)
                }
                Text(
                    text = "Sending + receiving",
                    color = colors.t1,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.showToast("Transfer options") }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = colors.t2)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PeerHeaderCard(peer = peer, statusText = "Connected · Phone · LAN")

            Spacer(modifier = Modifier.height(14.dp))

            // Sending Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SENDING · ${sendList.size} FILES",
                    color = colors.t3,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.clearCompleted(false) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear completed", tint = colors.acc, modifier = Modifier.size(18.dp))
                }
            }

            sendList.forEach { item ->
                TransferRow(
                    item = item,
                    onPause = { viewModel.toggleItemPause(item) },
                    onResume = { viewModel.toggleItemPause(item) },
                    onCancel = { viewModel.cancelItem(item) },
                    onRetry = { viewModel.retryItem(item) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Batch Summary Card
            val allSendDone = sendList.isNotEmpty() && sendList.all { it.state == TransferState.DONE }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.acc.copy(alpha = 0.12f))
                    .border(1.dp, colors.acc.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = if (allSendDone) "✓ Batch complete" else "Batch in progress",
                        color = if (allSendDone) ColorOk else colors.t1,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val activeCount = sendList.count { it.state == TransferState.SENDING }
                    val queuedCount = sendList.count { it.state == TransferState.QUEUED }
                    val pausedCount = sendList.count { it.state == TransferState.PAUSED }
                    Text(
                        text = if (allSendDone) "${sendList.size} sent · 0 failed · 0 skipped — avg 6.2 MB/s"
                        else "$activeCount sending · $queuedCount queued · $pausedCount paused — avg 6.2 MB/s",
                        color = colors.t3,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Receiving Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECEIVING · ${recvList.size} FILE${if (recvList.size == 1) "" else "S"}",
                    color = colors.t3,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.clearCompleted(true) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear completed", tint = colors.acc, modifier = Modifier.size(18.dp))
                }
            }

            if (recvList.isEmpty()) {
                Text(
                    text = "Nothing incoming — the peer can send back at any time without re-pairing.",
                    color = colors.t3,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                recvList.forEach { item ->
                    TransferRow(
                        item = item,
                        onPause = { viewModel.toggleItemPause(item) },
                        onResume = { viewModel.toggleItemPause(item) },
                        onCancel = { viewModel.cancelItem(item) },
                        onRetry = { viewModel.retryItem(item) }
                    )
                }
            }
        }

        // Bottom Transfer Action Bar
        TransferActionBar(
            isPaused = isAnyPaused,
            onAddFiles = { viewModel.navigateTo(ScreenState.FILES) },
            onTogglePause = { viewModel.togglePauseAll() },
            onBackground = { viewModel.backgroundSession() },
            onEndSession = { viewModel.endTransferSession() }
        )
    }
}

@Composable
fun ReceiveScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(ScreenState.CONNECT) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.t1)
                }
                Text("Receive", color = colors.t1, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.showToast("Receive settings") }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = colors.t2)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(colors.acc.copy(alpha = 0.22f))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = colors.acc,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Listening for senders",
                color = colors.t1,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Keep this screen open. Anyone on your network can send files — you can queue files to send back too.",
                color = colors.t2,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(26.dp))

            val items = listOf(
                Pair("BROADCASTING AS", "MYA-L10"),
                Pair("TRANSPORT", "Wi-Fi LAN · BT"),
                Pair("PORT", "33456")
            )
            items.forEach { (k, v) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(k, color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(v, color = colors.t1, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { viewModel.navigateTo(ScreenState.CONSENT_PEER) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.acc.copy(alpha = 0.16f),
                    contentColor = colors.acc
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, colors.acc.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            ) {
                Text("Simulate incoming request", fontWeight = FontWeight.SemiBold)
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

@Composable
fun ReceivingScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val scrollState = rememberScrollState()
    val recvList by viewModel.receivingList.collectAsState()
    val sendBackList by viewModel.sendingList.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
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
                IconButton(onClick = { viewModel.navigateTo(ScreenState.CONNECT) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.t1)
                }
                Text("Receiving + sending back", color = colors.t1, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.showToast("Options") }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = colors.t2)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PeerHeaderCard(
                peer = Peer("p", "Pixel 7X", "P", "#0EA5E9", "Connected · Phone · Nearby", "Nearby"),
                statusText = "Connected · Phone · Nearby"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Receiving Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECEIVING · ${recvList.size} FILE${if (recvList.size == 1) "" else "S"}",
                    color = colors.t3,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.clearCompleted(true) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear completed", tint = colors.acc, modifier = Modifier.size(18.dp))
                }
            }

            recvList.forEach { item ->
                TransferRow(
                    item = item,
                    onPause = { viewModel.toggleItemPause(item) },
                    onResume = { viewModel.toggleItemPause(item) },
                    onCancel = { viewModel.cancelItem(item) },
                    onRetry = { viewModel.retryItem(item) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sending Back Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SENDING BACK · ${sendBackList.size} FILE${if (sendBackList.size == 1) "" else "S"}",
                    color = colors.t3,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.clearCompleted(false) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear completed", tint = colors.acc, modifier = Modifier.size(18.dp))
                }
            }

            sendBackList.forEach { item ->
                TransferRow(
                    item = item,
                    onPause = { viewModel.toggleItemPause(item) },
                    onResume = { viewModel.toggleItemPause(item) },
                    onCancel = { viewModel.cancelItem(item) },
                    onRetry = { viewModel.retryItem(item) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.acc.copy(alpha = 0.12f))
                    .border(1.dp, colors.acc.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("✓ Batch complete", color = ColorOk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "In: ${recvList.size} received · 0 failed · 0 skipped — avg 8.4 MB/s\nOut: ${sendBackList.size} sent · 0 failed · 0 skipped — avg 5.7 MB/s",
                        color = colors.t3,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
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

@Composable
fun BroadcastSenderScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val scrollState = rememberScrollState()
    val peers by viewModel.broadcastPeers.collectAsState()
    val items by viewModel.broadcastItems.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
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
                IconButton(onClick = { viewModel.navigateTo(ScreenState.CONNECT) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.t1)
                }
                Text("Broadcasting", color = colors.t1, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.showToast("Broadcast options") }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = colors.t2)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, color = colors.t1, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${formatBytes(item.size)} · ${item.state.raw}", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        StatusChip(state = item.state)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    peers.forEach { peer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(peer.colorHex))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(peer.letter, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors.pressed)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.68f)
                                        .height(4.dp)
                                        .background(colors.acc)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text("68%", color = colors.t3, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.acc.copy(alpha = 0.12f))
                    .border(1.dp, colors.acc.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("⇶ Broadcasting to ${peers.size} phones", color = colors.t1, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1 broadcasting · 2 queued — combined throughput 14.2 MB/s", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair("${peers.size}", "PEERS"),
                            Pair("212", "BATCH MB"),
                            Pair("636", "TO SEND MB")
                        ).forEach { (v, l) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.raised)
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(v, color = colors.acc, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                                Text(l, color = colors.t3, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.navigateTo(ScreenState.BROADCAST_COMPLETE) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.acc,
                    contentColor = colors.accInk
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("See completion screens", fontWeight = FontWeight.SemiBold)
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

@Composable
fun BroadcastCompleteScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val peers by viewModel.broadcastPeers.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(ScreenState.CONNECT) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.t1)
            }
            Text("Complete", color = colors.t1, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.showToast("More") }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = colors.t2)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.acc.copy(alpha = 0.12f))
                .border(1.dp, colors.acc.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.acc),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.accInk)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Broadcast complete", color = colors.t1, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("All ${peers.size.coerceAtLeast(2)} phones verified", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.acc.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("×${peers.size.coerceAtLeast(2)}", color = colors.acc, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text("DELIVERED TO", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        val delivered = if (peers.isEmpty()) {
            listOf(
                Peer("r", "Ravi's Redmi", "R", "#8B5CF6"),
                Peer("p", "Pixel 7X", "P", "#0EA5E9"),
                Peer("s", "Samsung A14", "S", "#22C55E")
            )
        } else peers

        delivered.forEach { peer ->
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
                        .background(Color(android.graphics.Color.parseColor(peer.colorHex))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(peer.letter, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(peer.name, color = colors.t1, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ColorOk)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Icon(Icons.Default.Check, contentDescription = "Done", tint = ColorOk, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.navigateTo(ScreenState.CONNECT) },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.acc,
                contentColor = colors.accInk
            ),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Done", fontWeight = FontWeight.Bold)
        }
    }
}
