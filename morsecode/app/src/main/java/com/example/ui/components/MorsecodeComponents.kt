package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Peer
import com.example.core.model.TransferItem
import com.example.core.model.TransferState
import com.example.ui.theme.ColorErr
import com.example.ui.theme.ColorOk
import com.example.ui.theme.ColorRecv
import com.example.ui.theme.ColorRecvBg
import com.example.ui.theme.ColorWarn
import com.example.ui.theme.LocalMorsecodeColors

@Composable
fun RadarView(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 150.dp
) {
    val colors = LocalMorsecodeColors.current
    val transition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .border(1.dp, colors.line, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f

            // Inner circle
            drawCircle(
                color = colors.line,
                radius = radius * 0.72f,
                style = Stroke(width = 1.dp.toPx())
            )
            // Intermediate circle
            drawCircle(
                color = colors.line,
                radius = radius * 0.38f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Conic sweep gradient
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to colors.acc.copy(alpha = 0.42f),
                    0.15f to Color.Transparent,
                    1.0f to Color.Transparent,
                    center = center
                ),
                startAngle = sweepAngle,
                sweepAngle = 90f,
                useCenter = true
            )

            // Blip 1
            drawCircle(
                color = ColorOk.copy(alpha = pulseAlpha),
                radius = 4.dp.toPx(),
                center = Offset(size.width * 0.22f, size.height * 0.32f)
            )
            // Blip 2
            drawCircle(
                color = ColorOk.copy(alpha = (1.35f - pulseAlpha).coerceIn(0.2f, 1f)),
                radius = 4.dp.toPx(),
                center = Offset(size.width * 0.70f, size.height * 0.26f)
            )
            // Blip 3
            drawCircle(
                color = ColorOk.copy(alpha = pulseAlpha),
                radius = 4.dp.toPx(),
                center = Offset(size.width * 0.58f, size.height * 0.70f)
            )
        }

        // Center target core
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(colors.acc),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(16.dp)) {
                drawCircle(color = colors.accInk, style = Stroke(width = 2.dp.toPx()))
                drawCircle(color = colors.accInk, radius = 3.dp.toPx())
            }
        }
    }
}

@Composable
fun ScrubberBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 4.dp
) {
    val colors = LocalMorsecodeColors.current
    var isDragging by remember { androidx.compose.runtime.mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(fraction)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek(fraction)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = maxWidth
        val safeProgress = progress.coerceIn(0f, 1f)

        // Background track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.pressed)
        )

        // Active progress fill
        Box(
            modifier = Modifier
                .width(totalWidth * safeProgress)
                .height(barHeight)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(colors.acc, Color(0xFF84CC16))
                    )
                )
        )

        // Draggable Knob
        val knobOffset = (totalWidth * safeProgress) - 6.5.dp
        Box(
            modifier = Modifier
                .offset(x = knobOffset.coerceAtLeast(0.dp))
                .size(13.dp)
                .clip(CircleShape)
                .background(colors.acc)
                .border(2.dp, colors.acc.copy(alpha = 0.35f), CircleShape)
        )
    }
}

@Composable
fun VolumeControl(
    volume: Float,
    isMuted: Boolean,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val currentLevel = if (isMuted) 0 else (volume * 10f).toInt().coerceIn(0, 10)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onMuteToggle,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isMuted || currentLevel == 0) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = colors.t1,
                modifier = Modifier.size(18.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (i in 1..10) {
                val isOn = i <= currentLevel
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isOn) colors.acc else colors.pressed)
                        .clickable { onVolumeChange(i / 10f) }
                )
            }
        }

        Text(
            text = if (isMuted) "mute" else "${(volume * 100).toInt()}%",
            color = colors.t3,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(32.dp)
        )
    }
}

@Composable
fun PeerHeaderCard(
    peer: Peer,
    modifier: Modifier = Modifier,
    statusText: String = "Connected · Phone · LAN"
) {
    val colors = LocalMorsecodeColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.acc.copy(alpha = 0.12f))
            .border(1.dp, colors.acc.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(android.graphics.Color.parseColor(peer.colorHex))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = peer.letter,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = peer.name,
                color = colors.t1,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = if (peer.sub.isNotEmpty()) peer.sub else statusText,
                color = colors.t3,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .border(1.dp, colors.acc.copy(alpha = 0.45f), RoundedCornerShape(7.dp))
                .padding(horizontal = 7.dp, vertical = 4.dp)
        ) {
            Text(
                text = peer.badge,
                color = colors.acc,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TransferRow(
    item: TransferItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val isDone = item.state == TransferState.DONE
    val isPaused = item.state == TransferState.PAUSED
    val isSending = item.state == TransferState.SENDING
    val isReceiving = item.state == TransferState.RECEIVING
    val isFailed = item.state == TransferState.FAILED

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status / Direction Icon
        Icon(
            imageVector = when {
                isDone -> Icons.Default.Check
                item.isIncoming -> Icons.Default.ArrowDownward
                else -> Icons.Default.ArrowUpward
            },
            contentDescription = null,
            tint = if (isDone) ColorOk else colors.t2,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = colors.t1,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val metaText = when (item.state) {
                TransferState.DONE -> "${formatBytes(item.size)} · CRC verified"
                TransferState.QUEUED -> "${formatBytes(item.size)} · waiting"
                TransferState.PAUSED -> "${formatBytes(item.sent)} / ${formatBytes(item.size)} · paused"
                TransferState.FAILED -> "${formatBytes(item.sent)} / ${formatBytes(item.size)} · failed"
                else -> "${formatBytes(item.sent)} / ${formatBytes(item.size)} · ${formatSpeed(item.speed)}"
            }

            Text(
                text = metaText,
                color = colors.t3,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Progress Bar
            val barColor = when (item.state) {
                TransferState.DONE -> ColorOk
                TransferState.PAUSED -> ColorWarn
                TransferState.FAILED -> ColorErr
                else -> colors.acc
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.pressed)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.progress)
                        .height(4.dp)
                        .background(barColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // State Chip
        StatusChip(state = item.state)

        // Action Matrix
        if (!isDone) {
            Row(
                modifier = Modifier.padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSending || isReceiving) {
                    IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = colors.t2, modifier = Modifier.size(16.dp))
                    }
                }
                if (isPaused) {
                    IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = colors.t2, modifier = Modifier.size(16.dp))
                    }
                }
                if (isFailed) {
                    IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = colors.acc, modifier = Modifier.size(16.dp))
                    }
                }
                if (isSending || isReceiving || isPaused || item.state == TransferState.QUEUED) {
                    IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = colors.t2, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(state: TransferState) {
    val (label, bg, fg) = when (state) {
        TransferState.SENDING -> Triple("SENDING", LocalMorsecodeColors.current.acc.copy(alpha = 0.18f), LocalMorsecodeColors.current.acc)
        TransferState.RECEIVING -> Triple("RECV", ColorRecvBg, ColorRecv)
        TransferState.QUEUED -> Triple("QUEUED", LocalMorsecodeColors.current.raised, LocalMorsecodeColors.current.t2)
        TransferState.PAUSED -> Triple("PAUSED", ColorWarn.copy(alpha = 0.18f), ColorWarn)
        TransferState.DONE -> Triple("DONE", ColorOk.copy(alpha = 0.18f), ColorOk)
        TransferState.FAILED -> Triple("FAILED", ColorErr.copy(alpha = 0.18f), ColorErr)
        TransferState.ERROR -> Triple("ERROR", ColorErr.copy(alpha = 0.18f), ColorErr)
        TransferState.SKIPPED -> Triple("SKIPPED", LocalMorsecodeColors.current.raised, LocalMorsecodeColors.current.t3)
        TransferState.CANCELLED -> Triple("CANCELLED", LocalMorsecodeColors.current.raised, LocalMorsecodeColors.current.t3)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000L -> String.format(java.util.Locale.US, "%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000L -> {
            val mb = bytes / 1_000_000.0
            if (mb < 10) String.format(java.util.Locale.US, "%.1f MB", mb) else "${mb.toInt()} MB"
        }
        bytes >= 1_000L -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}

fun formatSpeed(speedBytes: Long): String {
    val mb = speedBytes / 1_000_000.0
    return String.format(java.util.Locale.US, "%.1f MB/s", mb)
}

fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", mins, secs)
}
