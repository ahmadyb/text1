package com.example.ui.connect

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Peer
import com.example.ui.components.RadarView
import com.example.ui.theme.ColorErr
import com.example.ui.theme.ColorOk
import com.example.ui.theme.ColorWarn
import com.example.ui.theme.LocalMorsecodeColors
import com.example.viewmodel.MorsecodeViewModel
import com.example.viewmodel.ScreenState

@Composable
fun ConnectHubScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val scrollState = rememberScrollState()
    val isWebShareRunning = viewModel.webShareServer.isRunning
    val ip = viewModel.webShareServer.getLocalIpAddress()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Connect",
                color = colors.t1,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.navigateTo(ScreenState.HELP) }) {
                Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = colors.t2)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Center Radar
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            RadarView()
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "3 devices nearby",
                color = colors.t1,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Scanning the local network",
                color = colors.t3,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Send & Receive Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.navigateTo(ScreenState.DISCOVER) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.acc,
                    contentColor = colors.accInk
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("↑ Send", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Button(
                onClick = { viewModel.navigateTo(ScreenState.RECEIVE) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.raised,
                    contentColor = colors.t1
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(1.dp, colors.line, RoundedCornerShape(999.dp))
            ) {
                Text("↓ Receive", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Broadcast to several phones button
        Button(
            onClick = { viewModel.navigateTo(ScreenState.DISCOVER_MULTI) },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.acc.copy(alpha = 0.16f),
                contentColor = colors.acc
            ),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, colors.acc.copy(alpha = 0.40f), RoundedCornerShape(999.dp))
        ) {
            Text("⇶ Broadcast to several phones", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // WebShare PC Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                .clickable { viewModel.navigateTo(ScreenState.WEBSHARE) }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.raised),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Computer, contentDescription = "WebShare", tint = colors.t2)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("WebShare · PC", color = colors.t1, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("http://$ip:33455", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isWebShareRunning) ColorOk.copy(alpha = 0.2f) else colors.raised)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isWebShareRunning) "ON" else "OFF",
                    color = if (isWebShareRunning) ColorOk else colors.t3,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Recent Devices
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT DEVICES",
                color = colors.t3,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Clear",
                color = colors.acc,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { viewModel.showToast("Recent devices cleared") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val recents = listOf(
            Triple("Ravi's Redmi", "2 min ago · 144 MB video", "LAN"),
            Triple("Office Laptop", "Yesterday · WebShare", "WEB"),
            Triple("Pixel 7X", "3 days · 12 files", "NEARBY")
        )

        recents.forEach { (name, sub, badge) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (badge == "WEB") viewModel.navigateTo(ScreenState.WEBSHARE)
                        else viewModel.navigateTo(ScreenState.SENDING)
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            when (name.take(1)) {
                                "R" -> Color(0xFF8B5CF6)
                                "O" -> Color(0xFFF59E0B)
                                else -> Color(0xFF0EA5E9)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name.take(1), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(name, color = colors.t1, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(sub, color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Box(
                    modifier = Modifier
                        .border(1.dp, colors.acc.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(badge, color = colors.acc, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DiscoverScreen(
    viewModel: MorsecodeViewModel,
    isMultiSelect: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val discovered = remember {
        mutableStateListOf(
            Peer("r", "Ravi's Redmi", "R", "#8B5CF6", "192.168.1.42 · Phone · LAN", "LAN", "192.168.1.42"),
            Peer("p", "Pixel 7X", "P", "#0EA5E9", "Bluetooth 5.2 · Phone · Nearby", "NEARBY", "192.168.1.77", isNearby = true),
            Peer("s", "Samsung A14", "S", "#22C55E", "192.168.1.51 · Phone · LAN", "LAN", "192.168.1.51")
        )
    }
    val chosenPeers = remember { mutableStateListOf("r", "p") }

    Column(
        modifier = modifier
            .fillMaxSize()
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
                text = if (isMultiSelect) "Broadcast" else "Send files",
                color = colors.t1,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.showToast("Scanning options") }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = colors.t2)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            RadarView(sizeDp = 118.dp)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Searching for devices", color = colors.t1, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("Look for peers on the same Wi-Fi network", color = colors.t3, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Transport & Queue info cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("TRANSPORT", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("LAN or Nearby", color = colors.acc, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("QUEUE", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("3 files · 152 MB", color = colors.t1, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DISCOVERED", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text("Refresh", color = colors.acc, fontSize = 12.sp, modifier = Modifier.clickable { viewModel.showToast("Rescanning…") })
        }

        Spacer(modifier = Modifier.height(8.dp))

        discovered.forEach { peer ->
            val isSelected = chosenPeers.contains(peer.id)
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
                    Text(peer.sub, color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                if (isMultiSelect) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, if (isSelected) colors.acc else colors.t3, CircleShape)
                            .background(if (isSelected) colors.acc else Color.Transparent)
                            .clickable {
                                if (isSelected) chosenPeers.remove(peer.id) else chosenPeers.add(peer.id)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = colors.accInk, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.currentPeer.value = peer
                            viewModel.navigateTo(ScreenState.SENDING)
                            viewModel.showToast("Connected to ${peer.name}")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.acc.copy(alpha = 0.16f),
                            contentColor = colors.acc
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (isMultiSelect) {
            Button(
                onClick = {
                    val peersToBroadcast = discovered.filter { chosenPeers.contains(it.id) }
                    if (peersToBroadcast.size >= 2) {
                        viewModel.startBroadcast(peersToBroadcast)
                    } else {
                        viewModel.showToast("Pick at least two devices to broadcast")
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.acc,
                    contentColor = colors.accInk
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Connect & broadcast to (${chosenPeers.size})", fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = "Each phone gets its own session — pick at least two.",
                color = colors.t3,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun ConnectionDoctorScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val checks by viewModel.doctorChecks.collectAsState()

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
            Text(
                text = "Connection Doctor",
                color = colors.t1,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        checks.forEach { check ->
            val statusColor = when (check.status) {
                "ok" -> ColorOk
                "warn" -> ColorWarn
                else -> ColorErr
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (check.status) {
                            "ok" -> Icons.Default.Check
                            "warn" -> Icons.Default.Info
                            else -> Icons.Default.Close
                        },
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(check.key, color = colors.t1, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(check.value, color = colors.t3, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.requestBatteryExemption() },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.acc.copy(alpha = 0.16f),
                contentColor = colors.acc
            ),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Request battery exemption", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                viewModel.runDoctorDiagnostics()
                viewModel.showToast("Re-running 6 checks…")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.raised,
                contentColor = colors.t1
            ),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, colors.line, RoundedCornerShape(999.dp))
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Refresh checks", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun HelpScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val scrollState = rememberScrollState()
    val expandedIdx by viewModel.helpExpandedIndex.collectAsState()

    val faq = listOf(
        Pair("How does Morsecode work?", "Two phones on the same Wi-Fi connect directly. If there's no shared network, Nearby Connections uses Bluetooth or Wi-Fi Direct."),
        Pair("Do I need internet?", "No. Nothing leaves your local network — there is no cloud, no account and no analytics."),
        Pair("Can I send and receive at the same time?", "Yes. Once two phones are paired either side can send; the screen shows Sending and Receiving separately."),
        Pair("What is WebShare?", "A small web server on your phone. Any browser on the same network can browse, play, download and upload — after you accept it."),
        Pair("Where are my logs?", "Settings → Logs. Export writes one .txt with the version header and any crash reports. Nothing is ever uploaded.")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
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
            Text("Help", color = colors.t1, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        faq.forEachIndexed { index, (q, a) ->
            val isExpanded = expandedIdx == index
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.helpExpandedIndex.value = if (isExpanded) -1 else index }
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(q, color = colors.t1, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.t3
                    )
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(a, color = colors.t2, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "TROUBLESHOOTING",
            color = colors.t3,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        val troubleshooting = listOf(
            "Phone not found during discovery",
            "Transfer stalls partway through",
            "Browser can't connect to WebShare",
            "Received APK won't install",
            "Hotspot won't turn on",
            "What does Broadcast do?"
        )

        troubleshooting.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(ScreenState.DOCTOR) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item, color = colors.t1, fontSize = 13.sp)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.t3)
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        }
    }
}

@Composable
fun OnboardingScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val step by viewModel.onboardingStep.collectAsState()

    val slides = listOf(
        Triple("Send files without the internet", "Direct phone-to-phone transfer over Wi-Fi or Bluetooth. No cloud. No accounts. No ads.", "Continue"),
        Triple("Permissions we need", "Nearby devices to find peers. Storage to read and save files. Battery exemption to keep transfers alive when the screen is off.", "Grant permissions"),
        Triple("Or use a browser", "Start WebShare and any laptop on the same network can browse, download, and upload from your phone.", "Continue"),
        Triple("You're all set", "Tap Send to choose files or Receive to listen. Everything stays on your local network.", "Open Morsecode")
    )
    val cur = slides[step.coerceIn(0, 3)]

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    when (step) {
                        0 -> Color(0xFFF59E0B)
                        1 -> Color(0xFF22C55E)
                        2 -> Color(0xFF0EA5E9)
                        else -> Color(0xFF8B5CF6)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (step) {
                    0 -> Icons.Default.TrackChanges
                    1 -> Icons.Default.Lock
                    2 -> Icons.Default.Language
                    else -> Icons.Default.Check
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = cur.first,
            color = colors.t1,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = cur.second,
            color = colors.t2,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Dots
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0..3) {
                Box(
                    modifier = Modifier
                        .size(width = if (i == step) 18.dp else 7.dp, height = 7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (i == step) colors.acc else colors.pressed)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {
                if (step >= 3) {
                    viewModel.navigateTo(ScreenState.CONNECT)
                } else {
                    viewModel.navigateTo(ScreenState.ONBOARDING)
                    viewModel.onboardingStep.value = step + 1
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.acc,
                contentColor = colors.accInk
            ),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(cur.third, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (step >= 3) "Replay tour" else "Skip",
            color = colors.acc,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable {
                viewModel.navigateTo(ScreenState.CONNECT)
            }
        )
    }
}
