package com.example.ui.files

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.TransferKind
import com.example.ui.components.ScrubberBar
import com.example.ui.components.VolumeControl
import com.example.ui.components.formatBytes
import com.example.ui.components.formatTime
import com.example.ui.theme.ColorErr
import com.example.ui.theme.ColorOk
import com.example.ui.theme.LocalMorsecodeColors
import com.example.viewmodel.MorsecodeViewModel
import com.example.viewmodel.ScreenState

@Composable
fun FilesCategoryScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val currentTab by viewModel.filesCurrentTab.collectAsState()
    val selectedIds by viewModel.selectedItemIds.collectAsState()

    val tabs = listOf("photos", "videos", "music", "apps", "files")

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .draggable(
                    state = rememberDraggableState { delta ->
                        if (delta < -30) {
                            val nextIdx = (tabs.indexOf(currentTab) + 1).coerceAtMost(tabs.size - 1)
                            viewModel.filesCurrentTab.value = tabs[nextIdx]
                        } else if (delta > 30) {
                            val prevIdx = (tabs.indexOf(currentTab) - 1).coerceAtLeast(0)
                            viewModel.filesCurrentTab.value = tabs[prevIdx]
                        }
                    },
                    orientation = Orientation.Horizontal
                )
        ) {
            // Sticky Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Files",
                    color = colors.t1,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.showToast("Search your files") }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = colors.t2)
                }
                IconButton(onClick = { viewModel.isSortSheetOpen.value = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Sort", tint = colors.t2)
                }
                IconButton(onClick = { viewModel.showToast("View: grid · list · details") }) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "View mode", tint = colors.t2)
                }
            }

            // Category Tab Strip (all 5 fit screen width, accent underline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
            ) {
                tabs.forEach { tab ->
                    val isSelected = currentTab == tab
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.filesCurrentTab.value = tab }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tab.replaceFirstChar { it.uppercase() },
                            color = if (isSelected) colors.acc else colors.t2,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(2.5.dp)
                                .background(if (isSelected) colors.acc else Color.Transparent)
                        )
                    }
                }
            }
            HorizontalDivider(color = colors.line, thickness = 1.dp)

            // Category Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    "photos" -> PhotosTabContent(viewModel)
                    "videos" -> VideosTabContent(viewModel)
                    "music" -> MusicTabContent(viewModel)
                    "apps" -> AppsTabContent(viewModel)
                    "files" -> FileBrowserTabContent(viewModel)
                }
            }
        }

        // Floating Selection Bar
        if (selectedIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.card)
                    .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.queueSelectedFilesForSend() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorOk,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send (${selectedIds.size}) · 24.6 MB", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { viewModel.isShareSheetOpen.value = true }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = colors.t1, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.showToast("Moved to Trash") }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ColorErr, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.clearFileSelection() }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = colors.t1, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun PhotosTabContent(viewModel: MorsecodeViewModel) {
    val colors = LocalMorsecodeColors.current
    val photos by viewModel.photosList.collectAsState()
    val selected by viewModel.selectedItemIds.collectAsState()
    val scrollState = rememberScrollState()

    val todayPhotos = photos.take(6)
    val yesterdayPhotos = photos.drop(6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("Tap a photo to open · tap its circle to select", color = colors.t3, fontSize = 11.sp)

        Spacer(modifier = Modifier.height(10.dp))

        // Today Group
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TODAY · ${todayPhotos.size} ITEMS", color = colors.t3, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            val allTodaySelected = todayPhotos.isNotEmpty() && todayPhotos.all { selected.contains(it.id) }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (allTodaySelected) ColorOk else colors.t3, CircleShape)
                    .background(if (allTodaySelected) ColorOk else Color.Transparent)
                    .clickable { viewModel.selectAllFiles(todayPhotos.map { it.id }) },
                contentAlignment = Alignment.Center
            ) {
                if (allTodaySelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PhotoGridRow(todayPhotos, selected, viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // Yesterday Group
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("YESTERDAY · ${yesterdayPhotos.size} ITEMS", color = colors.t3, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            val allYdaySelected = yesterdayPhotos.isNotEmpty() && yesterdayPhotos.all { selected.contains(it.id) }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (allYdaySelected) ColorOk else colors.t3, CircleShape)
                    .background(if (allYdaySelected) ColorOk else Color.Transparent)
                    .clickable { viewModel.selectAllFiles(yesterdayPhotos.map { it.id }) },
                contentAlignment = Alignment.Center
            ) {
                if (allYdaySelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PhotoGridRow(yesterdayPhotos, selected, viewModel)

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun PhotoGridRow(
    photos: List<com.example.core.storage.MediaFile>,
    selected: Set<String>,
    viewModel: MorsecodeViewModel
) {
    val colors = LocalMorsecodeColors.current
    val chunked = photos.chunked(3)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        chunked.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEachIndexed { _, photo ->
                    val isSel = selected.contains(photo.id)
                    val seed = photo.id.hashCode()
                    val hue1 = Math.abs(seed * 137 % 360)
                    val hue2 = (hue1 + 40) % 360

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color.hsl(hue1.toFloat(), 0.45f, 0.35f),
                                        Color.hsl(hue2.toFloat(), 0.45f, 0.20f)
                                    )
                                )
                            )
                            .border(if (isSel) 2.dp else 0.dp, colors.acc, RoundedCornerShape(8.dp))
                            .clickable {
                                if (selected.isNotEmpty()) {
                                    viewModel.toggleFileSelection(photo.id)
                                } else {
                                    viewModel.navigateTo(ScreenState.PHOTO_VIEWER)
                                }
                            }
                    ) {
                        // Circle select button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, if (isSel) ColorOk else Color.White.copy(alpha = 0.8f), CircleShape)
                                .background(if (isSel) ColorOk else Color.Black.copy(alpha = 0.35f))
                                .clickable { viewModel.toggleFileSelection(photo.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
                if (row.size < 3) {
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun VideosTabContent(viewModel: MorsecodeViewModel) {
    val colors = LocalMorsecodeColors.current
    val videos by viewModel.videosList.collectAsState()
    val selected by viewModel.selectedItemIds.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("Tap a clip to open player · tap circle to select", color = colors.t3, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(10.dp))

        val chunked = videos.chunked(3)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { vid ->
                        val isSel = selected.contains(vid.id)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.raised)
                                .border(if (isSel) 2.dp else 0.dp, colors.acc, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (selected.isNotEmpty()) {
                                        viewModel.toggleFileSelection(vid.id)
                                    } else {
                                        viewModel.navigateTo(ScreenState.VIDEO_PLAYER)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))

                            Text(
                                text = vid.durationFormatted,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, if (isSel) ColorOk else Color.White.copy(alpha = 0.8f), CircleShape)
                                    .background(if (isSel) ColorOk else Color.Black.copy(alpha = 0.35f))
                                    .clickable { viewModel.toggleFileSelection(vid.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                    if (row.size < 3) {
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun MusicTabContent(viewModel: MorsecodeViewModel) {
    val colors = LocalMorsecodeColors.current
    val music by viewModel.musicList.collectAsState()
    val selected by viewModel.selectedItemIds.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ALL SONGS · ${music.size}", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            val allSel = music.isNotEmpty() && music.all { selected.contains(it.id) }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (allSel) ColorOk else colors.t3, CircleShape)
                    .background(if (allSel) ColorOk else Color.Transparent)
                    .clickable { viewModel.selectAllFiles(music.map { it.id }) },
                contentAlignment = Alignment.Center
            ) {
                if (allSel) Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        music.forEachIndexed { idx, song ->
            val isSel = selected.contains(song.id)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (selected.isNotEmpty()) viewModel.toggleFileSelection(song.id)
                        else viewModel.navigateTo(ScreenState.MUSIC_PLAYER)
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, if (isSel) ColorOk else colors.t3, CircleShape)
                        .background(if (isSel) ColorOk else Color.Transparent)
                        .clickable { viewModel.toggleFileSelection(song.id) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(song.name.removeSuffix(".mp3"), color = colors.t1, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("${song.artist} · ${song.durationFormatted} · ${formatBytes(song.size)}", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                if (idx == 1 && selected.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.acc.copy(alpha = 0.2f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text("PLAYING", color = colors.acc, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun AppsTabContent(viewModel: MorsecodeViewModel) {
    val colors = LocalMorsecodeColors.current
    val apps by viewModel.appsList.collectAsState()
    val selected by viewModel.selectedItemIds.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("USER APPS · ${apps.size}", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            val allSel = apps.isNotEmpty() && apps.all { selected.contains(it.packageName) }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (allSel) ColorOk else colors.t3, CircleShape)
                    .background(if (allSel) ColorOk else Color.Transparent)
                    .clickable { viewModel.selectAllFiles(apps.map { it.packageName }) },
                contentAlignment = Alignment.Center
            ) {
                if (allSel) Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val chunked = apps.chunked(4)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { app ->
                        val isSel = selected.contains(app.packageName)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.toggleFileSelection(app.packageName) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(Color(android.graphics.Color.parseColor(app.iconColorHex)))
                                    .border(if (isSel) 2.dp else 0.dp, colors.acc, RoundedCornerShape(15.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(app.appName.take(1), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(app.appName, color = colors.t1, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(formatBytes(app.apkSize), color = colors.t3, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    if (row.size < 4) {
                        repeat(4 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun FileBrowserTabContent(viewModel: MorsecodeViewModel) {
    val colors = LocalMorsecodeColors.current
    val scrollState = rememberScrollState()

    val categories = listOf(
        Triple("Documents", 155, Color(0xFF22C55E)),
        Triple("Ebooks", 111, Color(0xFF0EA5E9)),
        Triple("Archives", 3, Color(0xFFEF4444)),
        Triple("APKs", 1, Color(0xFF8B5CF6)),
        Triple("Large files", 1, Color(0xFFF59E0B))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("CATEGORIES", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        categories.forEach { (cat, count, color) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(ScreenState.BROWSE_STORAGE) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(cat, color = colors.t1, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("$count", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.t3, modifier = Modifier.size(18.dp))
            }
            HorizontalDivider(color = colors.line, thickness = 1.dp)
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text("FOLDERS", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        listOf("Download", "Internal storage").forEach { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.currentFolder.value = folder
                        viewModel.navigateTo(ScreenState.BROWSE_STORAGE)
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.raised),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = colors.t2, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(folder, color = colors.t1, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.t3, modifier = Modifier.size(18.dp))
            }
            HorizontalDivider(color = colors.line, thickness = 1.dp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.showToast("SAF: pick a folder to grant access") }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.raised),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = colors.acc, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Add a folder", color = colors.t1, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Storage Access Framework grant", color = colors.t3, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun BrowseStorageScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val currentDir by viewModel.currentFolder.collectAsState()
    val selected by viewModel.selectedItemIds.collectAsState()
    val scrollState = rememberScrollState()

    val rows = listOf(
        Triple("DCIM", "folder", "43.6 GB · 812 items"),
        Triple("Download", "folder", "2.1 GB · 31 items"),
        Triple("Movies", "folder", "33.1 GB · 88 items"),
        Triple("Music", "folder", "26.8 GB · 412 items"),
        Triple("Pictures", "folder", "6.4 GB · 241 items"),
        Triple("WhatsApp", "folder", "14.7 GB · 1204 items"),
        Triple("Morsecode", "folder", "1.4 GB · 18 items"),
        Triple("notes_backup.zip", "zip", "18.2 MB · Yesterday"),
        Triple("invoice_2024.pdf", "doc", "2.4 MB · Today 14:42"),
        Triple("holiday_2019.mp4", "video", "144 MB · 12 May")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Sticky Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(ScreenState.FILES) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.t1)
            }
            Text(currentDir, color = colors.t1, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.showToast("Search this folder") }) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = colors.t2)
            }
            IconButton(onClick = { viewModel.isSortSheetOpen.value = true }) {
                Icon(Icons.Default.FilterList, contentDescription = "Sort", tint = colors.t2)
            }
        }

        // Sticky Path / Breadcrumb Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.raised)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "/storage/emulated/0/$currentDir",
                color = colors.t1,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Copy",
                color = colors.acc,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.clickable { viewModel.showToast("Path copied to clipboard") }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            rows.forEach { (name, kind, meta) ->
                val isSel = selected.contains(name)
                val isFolder = kind == "folder"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isFolder) {
                                viewModel.currentFolder.value = name
                            } else {
                                viewModel.toggleFileSelection(name)
                            }
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, if (isSel) ColorOk else colors.t3, CircleShape)
                            .background(if (isSel) ColorOk else Color.Transparent)
                            .clickable { viewModel.toggleFileSelection(name) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.raised),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFolder) Icons.Default.Folder else Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = if (isFolder) colors.acc else colors.t2,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, color = colors.t1, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(meta, color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    if (isFolder) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.t3, modifier = Modifier.size(18.dp))
                    }
                }
                HorizontalDivider(color = colors.line, thickness = 1.dp)
            }
        }
    }
}

@Composable
fun PhotoViewerScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val photoIdx by viewModel.activePhotoIndex.collectAsState()
    val photos by viewModel.photosList.collectAsState()
    val totalPhotos = photos.size.coerceAtLeast(1)
    val safeIdx = (photoIdx - 1).coerceIn(0, totalPhotos - 1)
    val currentPhoto = photos.getOrNull(safeIdx)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(ScreenState.FILES) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(currentPhoto?.name ?: "IMG_${2040 + photoIdx}.jpg", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${safeIdx + 1} of $totalPhotos · ${formatBytes(currentPhoto?.size ?: 2400000L)}", color = Color(0xFF8A8A8A), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Large Swiping Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1E1E))
                .draggable(
                    state = rememberDraggableState { delta ->
                        if (delta < -30) {
                            val next = if (photoIdx < totalPhotos) photoIdx + 1 else 1
                            viewModel.activePhotoIndex.value = next
                        } else if (delta > 30) {
                            val prev = if (photoIdx > 1) photoIdx - 1 else totalPhotos
                            viewModel.activePhotoIndex.value = prev
                        }
                    },
                    orientation = Orientation.Horizontal
                ),
            contentAlignment = Alignment.Center
        ) {
            if (currentPhoto?.uri != null) {
                AsyncImage(
                    model = currentPhoto.uri,
                    contentDescription = currentPhoto.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val seed = photoIdx.hashCode()
                val hue1 = Math.abs(seed * 137 % 360)
                val hue2 = (hue1 + 45) % 360
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color.hsl(hue1.toFloat(), 0.45f, 0.46f),
                                    Color.hsl(hue2.toFloat(), 0.45f, 0.26f)
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Dots Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 1..minOf(totalPhotos, 15)) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(width = if (i == photoIdx) 14.dp else 5.dp, height = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i == photoIdx) Color(0xFFFACC15) else Color.White.copy(alpha = 0.3f))
                )
            }
        }

        Text("‹ swipe to browse ›", color = Color(0xFF9A9A9A), fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Action Icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.isShareSheetOpen.value = true },
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF1A1A1A))
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
            }
            IconButton(
                onClick = { viewModel.showToast("Opening external editor") },
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF1A1A1A))
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
            }
            IconButton(
                onClick = { viewModel.showToast("Delete photo") },
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF1A1A1A))
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
            IconButton(
                onClick = { viewModel.showToast("${currentPhoto?.name} · ${formatBytes(currentPhoto?.size ?: 0L)}") },
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF1A1A1A))
            ) {
                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
            }
            IconButton(
                onClick = {
                    currentPhoto?.let { p ->
                        viewModel.selectedItemIds.value = setOf(p.id)
                        viewModel.queueSelectedFilesForSend()
                    }
                },
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFFACC15))
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send this photo", tint = Color.Black)
            }
        }
    }
}

@Composable
fun MusicPlayerScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMorsecodeColors.current
    val musicState by viewModel.playerManager.musicState.collectAsState()
    val songs by viewModel.musicList.collectAsState()

    val currentTitle = musicState.activeTrack?.name?.removeSuffix(".mp3") ?: "Ocean Eyes (Live)"
    val currentArtist = musicState.activeTrack?.artist?.ifEmpty { "Harbour Lights" } ?: "Harbour Lights · Midnight Sessions"
    val posSec = (musicState.currentPositionMs / 1000).toInt()
    val durSec = (musicState.durationMs / 1000).toInt().coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(ScreenState.FILES) }) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back", tint = colors.t1)
            }
            Text(
                text = "NOW PLAYING",
                color = colors.t2,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.showToast("Queue: ${songs.size} tracks") }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = colors.t2)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Artwork
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFF59E0B), Color(0xFFEA580C), Color(0xFF84CC16))
                    )
                )
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Black, modifier = Modifier.size(64.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = currentTitle,
            color = colors.t1,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = currentArtist,
            color = colors.t3,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Scrubber
        ScrubberBar(
            progress = musicState.progress,
            onSeek = { frac -> viewModel.playerManager.seekMusic(frac) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(posSec), color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("-${formatTime((durSec - posSec).coerceAtLeast(0))}", color = colors.t3, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.showToast("Shuffle toggled") }) {
                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = colors.t2)
            }
            IconButton(onClick = {
                val prevIdx = (musicState.trackIndex - 1 + songs.size) % songs.size.coerceAtLeast(1)
                songs.getOrNull(prevIdx)?.let { viewModel.playMusic(it, prevIdx) }
            }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = colors.t1)
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.acc)
                    .clickable { viewModel.playerManager.toggleMusicPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (musicState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (musicState.isPlaying) "Pause" else "Play",
                    tint = colors.accInk,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = {
                val nextIdx = (musicState.trackIndex + 1) % songs.size.coerceAtLeast(1)
                songs.getOrNull(nextIdx)?.let { viewModel.playMusic(it, nextIdx) }
            }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = colors.t1)
            }
            IconButton(onClick = { viewModel.showToast("Repeat toggled") }) {
                Icon(Icons.Default.Repeat, contentDescription = "Repeat", tint = colors.t2)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Volume and relative seeking controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { viewModel.playerManager.seekMusicRelative(-10000L) }) {
                    Icon(Icons.Default.FastRewind, contentDescription = "-10s", tint = colors.t2)
                }
                IconButton(onClick = { viewModel.playerManager.seekMusicRelative(10000L) }) {
                    Icon(Icons.Default.FastForward, contentDescription = "+10s", tint = colors.t2)
                }
            }

            VolumeControl(
                volume = musicState.volume,
                isMuted = musicState.isMuted,
                onVolumeChange = { viewModel.playerManager.setMusicVolume(it) },
                onMuteToggle = { viewModel.playerManager.toggleMusicMute() }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            IconButton(onClick = { viewModel.showToast("Saved track") }) {
                Icon(Icons.Default.Favorite, contentDescription = "Like", tint = colors.acc)
            }
            IconButton(onClick = { viewModel.isShareSheetOpen.value = true }) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = colors.t2)
            }
            IconButton(onClick = { viewModel.showToast("Queue opened") }) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Queue", tint = colors.t2)
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(
    viewModel: MorsecodeViewModel,
    modifier: Modifier = Modifier
) {
    val videoState by viewModel.playerManager.videoState.collectAsState()
    val posSec = (videoState.currentPositionMs / 1000).toInt()
    val durSec = (videoState.durationMs / 1000).toInt().coerceAtLeast(1)
    val videoTitle = videoState.activeTrack?.name ?: "holiday_2019.mp4"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(ScreenState.FILES) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(videoTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("1080p · ${formatBytes(videoState.activeTrack?.size ?: 144000000L)} · ${formatTime(durSec)}", color = Color(0xFF8A8A8A), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Center Video View or Play Circle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF141414)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { viewModel.playerManager.toggleVideoPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (videoState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(posSec), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(formatTime(durSec), color = Color(0xFF8A8A8A), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        ScrubberBar(
            progress = videoState.progress,
            onSeek = { frac -> viewModel.playerManager.seekVideo(frac) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.playerManager.seekVideoRelative(-10000L) }) {
                Icon(Icons.Default.FastRewind, contentDescription = "-10s", tint = Color.White)
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFACC15))
                    .clickable { viewModel.playerManager.toggleVideoPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (videoState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black
                )
            }
            IconButton(onClick = { viewModel.playerManager.seekVideoRelative(10000L) }) {
                Icon(Icons.Default.FastForward, contentDescription = "+10s", tint = Color.White)
            }

            // Real Volume Slider
            VolumeControl(
                volume = videoState.volume,
                isMuted = videoState.isMuted,
                onVolumeChange = { viewModel.playerManager.setVideoVolume(it) },
                onMuteToggle = { viewModel.playerManager.toggleVideoMute() }
            )

            IconButton(onClick = { viewModel.showToast("No subtitle tracks") }) {
                Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White)
            }
        }
    }
}
