package dev.vikingsen.absclientapp.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.core.model.PlaybackProgress
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: LibraryViewModel = koinViewModel()
) {
    val books by viewModel.books.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()

    val filterStatus by viewModel.filterStatus.collectAsState()
    val filterDownloadedOnly by viewModel.filterDownloadedOnly.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    
    val settingsRepository: SettingsRepository = get()
    val serverUrl = remember { settingsRepository.getServerUrl() ?: "" }
    val token = remember { settingsRepository.getToken() ?: "" }
    val playerManager: dev.vikingsen.absclientapp.core.player.PlayerManager = get()
    val currentBook by playerManager.currentBook.collectAsState()
    val showMiniPlayer = currentBook != null

    val filteredBooks = remember(books, searchQuery) {
        if (searchQuery.isBlank()) {
            books
        } else {
            books.filter { bwp ->
                bwp.book.title.contains(searchQuery, ignoreCase = true) ||
                bwp.book.author.contains(searchQuery, ignoreCase = true) ||
                bwp.book.narrator.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Library",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by title, author, narrator...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0x33FFFFFF)
                )
            )

            // Filter & Sort Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scrollable Row of Filter Chips
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        FilterChip(
                            selected = filterStatus != ReadStatusFilter.ALL,
                            onClick = { statusMenuExpanded = true },
                            label = {
                                Text(
                                    text = when (filterStatus) {
                                        ReadStatusFilter.ALL -> "All Status"
                                        ReadStatusFilter.UNREAD -> "Unread"
                                        ReadStatusFilter.IN_PROGRESS -> "In Progress"
                                        ReadStatusFilter.READ -> "Read"
                                    },
                                    fontSize = 12.sp
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select status filter",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = Color.Transparent,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filterStatus != ReadStatusFilter.ALL,
                                borderColor = Color(0x33FFFFFF),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        DropdownMenu(
                            expanded = statusMenuExpanded,
                            onDismissRequest = { statusMenuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            ReadStatusFilter.values().forEach { filter ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = when (filter) {
                                                ReadStatusFilter.ALL -> "All Status"
                                                ReadStatusFilter.UNREAD -> "Unread"
                                                ReadStatusFilter.IN_PROGRESS -> "In Progress"
                                                ReadStatusFilter.READ -> "Read"
                                            }
                                        )
                                    },
                                    onClick = {
                                        viewModel.setFilterStatus(filter)
                                        statusMenuExpanded = false
                                    },
                                    trailingIcon = if (filterStatus == filter) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color(0x22FFFFFF))
                    )

                    // Downloaded Only filter
                    FilterChip(
                        selected = filterDownloadedOnly,
                        onClick = { viewModel.setFilterDownloadedOnly(!filterDownloadedOnly) },
                        label = { Text("Downloaded", fontSize = 12.sp) },
                        leadingIcon = if (filterDownloadedOnly) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filterDownloadedOnly,
                            borderColor = Color(0x33FFFFFF),
                            selectedBorderColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Dropdown Button
                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier
                            .background(Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Books",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Title (A-Z)") },
                            onClick = {
                                viewModel.setSortBy(SortOption.TITLE_ASC)
                                sortMenuExpanded = false
                            },
                            trailingIcon = if (sortBy == SortOption.TITLE_ASC) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("Title (Z-A)") },
                            onClick = {
                                viewModel.setSortBy(SortOption.TITLE_DESC)
                                sortMenuExpanded = false
                            },
                            trailingIcon = if (sortBy == SortOption.TITLE_DESC) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("Author (A-Z)") },
                            onClick = {
                                viewModel.setSortBy(SortOption.AUTHOR_ASC)
                                sortMenuExpanded = false
                            },
                            trailingIcon = if (sortBy == SortOption.AUTHOR_ASC) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("Author (Z-A)") },
                            onClick = {
                                viewModel.setSortBy(SortOption.AUTHOR_DESC)
                                sortMenuExpanded = false
                            },
                            trailingIcon = if (sortBy == SortOption.AUTHOR_DESC) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("Duration (Shortest)") },
                            onClick = {
                                viewModel.setSortBy(SortOption.DURATION_ASC)
                                sortMenuExpanded = false
                            },
                            trailingIcon = if (sortBy == SortOption.DURATION_ASC) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("Duration (Longest)") },
                            onClick = {
                                viewModel.setSortBy(SortOption.DURATION_DESC)
                                sortMenuExpanded = false
                            },
                            trailingIcon = if (sortBy == SortOption.DURATION_DESC) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("Recently Played") },
                            onClick = {
                                viewModel.setSortBy(SortOption.LAST_PLAYED)
                                sortMenuExpanded = false
                            },
                            trailingIcon = if (sortBy == SortOption.LAST_PLAYED) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (filteredBooks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (books.isEmpty()) "No books synced. Pull to refresh!" else "No books match your filters.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = if (showMiniPlayer) 80.dp else 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredBooks, key = { it.book.id }) { bookWithProgress ->
                        BookCard(
                            book = bookWithProgress.book,
                            progress = bookWithProgress.progress,
                            serverUrl = serverUrl,
                            token = token,
                            onClick = { onBookClick(bookWithProgress.book.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookCard(
    book: Book,
    progress: PlaybackProgress?,
    serverUrl: String,
    token: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                if (!book.coverPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(book.coverPath)
                            .crossfade(true)
                            .build(),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val coverUrl = "${serverUrl.trimEnd('/')}/api/items/${book.id}/cover"
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverUrl)
                            .setHeader("Authorization", "Bearer $token")
                            .crossfade(true)
                            .build(),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = {
                            // Fallback gradient cover if image load fails
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
                                    text = book.title.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                }
                
                // Read badge (top-start / top-left)
                if (progress?.isFinished == true || (progress != null && progress.progress >= 0.99f)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Read",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Downloaded badge (top-end / top-right)
                if (book.isDownloaded) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color(0x99000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Offline", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Progress Bar at the bottom of the cover (bottom-center)
                if (progress != null && progress.progress > 0f && !progress.isFinished) {
                    LinearProgressIndicator(
                        progress = { progress.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0x33FFFFFF)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = book.author,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper function for reactive state of simple string
@Composable
fun mutableStateFlowOf(initialValue: String): MutableState<String> {
    return remember { mutableStateOf(initialValue) }
}
