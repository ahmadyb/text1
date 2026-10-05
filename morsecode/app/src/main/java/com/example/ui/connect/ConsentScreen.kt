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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TransferActionBar
import com.example.ui.theme.ColorOk
import com.example.ui.theme.LocalMorsecodeColors
import com.example.ui.transfer.ReceiveScreen
import com.example.viewmodel.MorsecodeViewModel
import com.example.viewmodel.ScreenState

@Composable
fun ConsentScreen(
    viewModel: MorsecodeViewModel,
    isBrowserConsent: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current

    Box(modifier = modifier.fillMaxSize()) {
        // Underlying receive screen behind scrim
        ReceiveScreen(viewModel = viewModel)

        // Scrim backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
        )

        // Modal Consent Card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.card)
                    .border(1.dp, colors.line, RoundedCornerShape(22.dp))
                    .padding(22.dp)
                    .testTag(if (isBrowserConsent) "consent_browser_card" else "consent_peer_card"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isBrowserConsent) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0EA5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Browser wants access",
                        color = colors.t1,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "A browser session wants to browse your phone.",
                        color = colors.t2,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Chrome · 192.168.1.88",
                        color = colors.t3,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "R",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Connection request",
                        color = colors.t1,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row {
                        Text(
                            text = "Ravi's Redmi",
                            color = colors.acc,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = " wants to send you files.",
                            color = colors.t2,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Phone · Wi-Fi LAN · 192.168.1.42",
                        color = colors.t3,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (isBrowserConsent) {
                                viewModel.denyBrowserSession()
                            } else {
                                viewModel.showToast("Rejected · sender notified")
                                viewModel.navigateTo(ScreenState.RECEIVE)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.raised,
                            contentColor = colors.t1
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .border(1.dp, colors.line, RoundedCornerShape(999.dp))
                            .testTag("consent_reject_button")
                    ) {
                        Text("Reject", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (isBrowserConsent) {
                                viewModel.approveBrowserSession()
                            } else {
                                viewModel.showToast("Accepted — session open")
                                viewModel.navigateTo(ScreenState.RECEIVING)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorOk,
                            contentColor = Color(0xFF06240F)
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("consent_accept_button")
                    ) {
                        Text("Accept", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Switch dialog link
                Text(
                    text = if (isBrowserConsent) "Show the peer dialog" else "Show the browser dialog",
                    color = colors.acc,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable {
                            viewModel.navigateTo(if (isBrowserConsent) ScreenState.CONSENT_PEER else ScreenState.CONSENT_BROWSER)
                        }
                        .testTag("consent_swap_link")
                )
            }
        }
    }
}
