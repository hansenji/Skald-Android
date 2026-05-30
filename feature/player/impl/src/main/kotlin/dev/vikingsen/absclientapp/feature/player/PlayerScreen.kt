package dev.vikingsen.absclientapp.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.graphics.vector.ImageVector
import dev.vikingsen.absclientapp.feature.player.icons.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showChaptersSheet by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (uiState == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("No audiobook active.", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    val state = uiState!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showChaptersSheet = true }) {
                        Icon(Icons.Default.List, contentDescription = "Chapters")
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Blurred dynamic background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(state.coverUrl)
                        .apply {
                            if (state.authorizationHeader != null) {
                                setHeader("Authorization", state.authorizationHeader)
                            }
                        }
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Dark semi-transparent overlay to ensure readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCD0A0E1A))
            )

            // Main Content Container
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Book Cover
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxHeight(0.8f)
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                    ) {
                        if (state.authorizationHeader == null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(state.coverUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = state.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(state.coverUrl)
                                    .setHeader("Authorization", state.authorizationHeader)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = state.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.tertiary
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = state.title.take(1).uppercase(),
                                            color = Color.White,
                                            fontSize = 64.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // Text details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.author,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    // Current Chapter name
                    if (state.currentChapterTitle != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = state.currentChapterTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progress SeekBar
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Slider(
                        value = state.currentPosition.toFloat(),
                        onValueChange = { viewModel.seekTo(it.toDouble()) },
                        valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        )
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state.currentPositionText,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = state.timeRemainingText,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Chapter
                    IconButton(
                        onClick = { viewModel.skipToPreviousChapter() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = skip_previous,
                            contentDescription = "Previous Chapter",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Skip Backward
                    IconButton(
                        onClick = { viewModel.skipBackward() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(replay, contentDescription = "Skip Back ${state.skipBackwardDuration}s", tint = Color.White, modifier = Modifier.size(32.dp))
                            Text("${state.skipBackwardDuration}s", fontSize = 10.sp, color = Color.LightGray)
                        }
                    }

                    // Play/Pause
                    IconButton(
                        onClick = {
                            if (!state.isLoading) {
                                if (state.isPlaying) viewModel.pause() else viewModel.play()
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Skip Forward
                    IconButton(
                        onClick = { viewModel.skipForward() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(forward_media, contentDescription = "Skip Forward ${state.skipForwardDuration}s", tint = Color.White, modifier = Modifier.size(32.dp))
                            Text("${state.skipForwardDuration}s", fontSize = 10.sp, color = Color.LightGray)
                        }
                    }

                    // Next Chapter
                    IconButton(
                        onClick = { viewModel.skipToNextChapter() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = skip_next,
                            contentDescription = "Next Chapter",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Extra controls (Speed, Sleep timer)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed Control
                    IconButton(onClick = { viewModel.cyclePlaybackSpeed() }) {
                        Icon(
                            imageVector = getSpeedIcon(state.playbackSpeed),
                            contentDescription = "Playback Speed",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Sleep Timer
                    TextButton(onClick = { showTimerDialog = true }) {
                        Icon(Icons.Default.Snooze, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = state.sleepTimerText,
                            color = if (state.sleepTimerRemaining > 0) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }
            }
        }

        // Chapters Sheet
        if (showChaptersSheet) {
            ModalBottomSheet(
                onDismissRequest = { showChaptersSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Chapters",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    HorizontalDivider(color = Color(0x33FFFFFF))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        state.chapters.forEachIndexed { index, chapter ->
                            val isCurrent = state.currentPosition >= chapter.start && state.currentPosition < chapter.end
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.seekTo(chapter.start)
                                        showChaptersSheet = false
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = chapter.title,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = chapter.startText,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                            HorizontalDivider(color = Color(0x11FFFFFF))
                        }
                    }
                }
            }
        }

        // Speed Dialog
        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                title = { Text("Playback Speed") },
                text = {
                    Column {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f).forEach { speed ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setPlaybackSpeed(speed)
                                        showSpeedDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${speed}x",
                                    fontWeight = if (state.playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                                    color = if (state.playbackSpeed == speed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (state.playbackSpeed == speed) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Sleep Timer Dialog
        if (showTimerDialog) {
            AlertDialog(
                onDismissRequest = { showTimerDialog = false },
                title = { Text("Sleep Timer") },
                text = {
                    Column {
                        listOf(
                            "Off" to 0,
                            "5 minutes" to 5,
                            "15 minutes" to 15,
                            "30 minutes" to 30,
                            "45 minutes" to 45,
                            "60 minutes" to 60
                        ).forEach { (label, minutes) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setSleepTimer(minutes)
                                        showTimerDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (state.currentChapterTitle != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setSleepTimerEndOfChapter()
                                        showTimerDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "End of active chapter",
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTimerDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Playback Settings Dialog (Settings Hook)
        if (showSettingsDialog) {
            var forwardExpanded by remember { mutableStateOf(false) }
            var backwardExpanded by remember { mutableStateOf(false) }
            val options = listOf(10, 30, 60)

            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Playback Settings") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text("Skip Forward Duration", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            TextButton(onClick = { forwardExpanded = true }) {
                                Text("${state.skipForwardDuration} seconds")
                            }
                            DropdownMenu(
                                expanded = forwardExpanded,
                                onDismissRequest = { forwardExpanded = false }
                            ) {
                                options.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text("$option seconds") },
                                        onClick = {
                                            viewModel.setSkipForwardDuration(option)
                                            forwardExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Skip Backward Duration", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            TextButton(onClick = { backwardExpanded = true }) {
                                Text("${state.skipBackwardDuration} seconds")
                            }
                            DropdownMenu(
                                expanded = backwardExpanded,
                                onDismissRequest = { backwardExpanded = false }
                            ) {
                                options.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text("$option seconds") },
                                        onClick = {
                                            viewModel.setSkipBackwardDuration(option)
                                            backwardExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

private fun getSpeedIcon(speed: Float): ImageVector {
    return when {
        speed < 0.625f -> speed_0_5x
        speed < 0.875f -> speed_0_7x
        speed < 1.125f -> speed_1x
        speed < 1.375f -> speed_1_2x
        speed < 1.625f -> speed_1_5x
        speed < 1.875f -> speed_1_7x
        else -> speed_2x
    }
}
