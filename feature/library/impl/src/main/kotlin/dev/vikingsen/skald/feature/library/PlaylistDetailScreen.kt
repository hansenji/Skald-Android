package dev.vikingsen.skald.feature.library

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.vikingsen.skald.core.model.Playlist
import dev.vikingsen.skald.core.model.PlaylistItem
import dev.vikingsen.skald.core.model.formatDuration
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onPlayClick: () -> Unit,
    viewModel: PlaylistDetailViewModel = koinViewModel()
) {
    LaunchedEffect(playlistId) {
        viewModel.setPlaylistId(playlistId)
    }

    val playlist by viewModel.playlist.collectAsState()
    val playlistItems by viewModel.playlistItems.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()
    val showMiniPlayer by viewModel.showMiniPlayer.collectAsState()
    val authorizationHeader = viewModel.authorizationHeader

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlist Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (playlist == null && isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (playlist != null) {
            val currentPlaylist = playlist!!
            val lazyListState = rememberLazyListState()
            
            val dragDropState = remember(lazyListState, playlistItems) {
                DragDropState(
                    lazyListState = lazyListState,
                    onMove = { from, to -> viewModel.moveItem(from, to) },
                    onDragCompleted = { viewModel.syncReorder() }
                )
            }

            val bottomPadding = if (showMiniPlayer) 80.dp else 16.dp

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pointerInput(dragDropState) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset -> dragDropState.onDragStart(offset) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragDropState.onDrag(dragAmount)
                            },
                            onDragEnd = { dragDropState.onDragEnd() },
                            onDragCancel = { dragDropState.onDragEnd() }
                        )
                    },
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    PlaylistHeader(
                        playlist = currentPlaylist,
                        onPlayClick = {
                            viewModel.playPlaylist(0)
                            onPlayClick()
                        }
                    )
                }

                if (playlistItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No items in this playlist",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(playlistItems, key = { _, item -> item.id }) { index, item ->
                        val isDragged = dragDropState.draggedIndex == index + 1
                        val offset = if (isDragged) dragDropState.draggedOffset else 0f
                        val elevation = animateDpAsState(if (isDragged) 8.dp else 0.dp, label = "elevation")

                        PlaylistItemRow(
                            item = item,
                            authHeader = authorizationHeader,
                            isDragged = isDragged,
                            onClick = {
                                viewModel.playPlaylist(index)
                                onPlayClick()
                            },
                            onDeleteClick = {
                                viewModel.deleteItem(item)
                            },
                            modifier = Modifier
                                .zIndex(if (isDragged) 10f else 1f)
                                .graphicsLayer {
                                    translationY = offset
                                }
                                .shadow(elevation.value, shape = RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun PlaylistHeader(
    playlist: Playlist,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // Playlist title
        Text(
            text = playlist.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Description
        val description = playlist.description
        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tracks and Duration metadata row
        val trackText = if (playlist.itemCount == 1) "1 track" else "${playlist.itemCount} tracks"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = trackText,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Duration: ${formatDuration(playlist.duration)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Big Play Button
        Button(
            onClick = onPlayClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Play Playlist",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun PlaylistItemRow(
    item: PlaylistItem,
    authHeader: String?,
    isDragged: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isDragged) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book cover thumbnail
            Box(
                modifier = Modifier
                    .size(40.dp, 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            ) {
                val coverPath = item.coverPath
                if (!coverPath.isNullOrEmpty()) {
                    AsyncCoverImage(
                        coverUrl = coverPath,
                        authHeader = authHeader,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details (Title and Duration)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDuration(item.duration),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete item button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove item",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

class DragDropState(
    val lazyListState: LazyListState,
    val onMove: (Int, Int) -> Unit,
    val onDragCompleted: () -> Unit
) {
    var draggedIndex by mutableStateOf<Int?>(null)
        private set

    var draggedOffset by mutableStateOf(0f)
        private set

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                offset.y.toInt() in item.offset..(item.offset + item.size)
            }?.let { item ->
                // The first item (header) is at index 0, actual items start at index 1
                if (item.index > 0) {
                    draggedIndex = item.index
                }
            }
    }

    fun onDrag(dragAmount: Offset) {
        val draggedIndex = draggedIndex ?: return
        draggedOffset += dragAmount.y

        val draggedItemInfo = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == draggedIndex } ?: return
        
        val currentOffset = draggedItemInfo.offset + draggedOffset

        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                item.index > 0 && item.index != draggedIndex &&
                        currentOffset.toInt() in item.offset..(item.offset + item.size)
            }?.let { hoverItem ->
                onMove(draggedIndex - 1, hoverItem.index - 1)
                draggedOffset += draggedItemInfo.offset - hoverItem.offset
                this.draggedIndex = hoverItem.index
            }
    }

    fun onDragEnd() {
        if (draggedIndex != null) {
            onDragCompleted()
        }
        draggedIndex = null
        draggedOffset = 0f
    }
}
