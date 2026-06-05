package dev.vikingsen.skald.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import dev.vikingsen.skald.core.model.ReadStatusFilter
import dev.vikingsen.skald.core.model.SortOption
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MusicNote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    viewModel: LibraryViewModel = koinViewModel()
) {
    val lazyBookCards = viewModel.books.collectAsLazyPagingItems()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()

    val filterStatus by viewModel.filterStatus.collectAsState()
    val filterDownloadedOnly by viewModel.filterDownloadedOnly.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val selectedLibraryId by viewModel.selectedLibraryId.collectAsState()
    val libraries by viewModel.libraries.collectAsState()
    val syncIntervalHours by viewModel.syncIntervalHours.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val visibleTabs by viewModel.visibleTabs.collectAsState()
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    val showMiniPlayer by viewModel.showMiniPlayer.collectAsState()

    if (showSettingsDialog) {
        SettingsDialog(
            currentInterval = syncIntervalHours,
            onIntervalSelected = { hours ->
                viewModel.setSyncIntervalHours(hours)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    var libraryMenuExpanded by remember { mutableStateOf(false) }
                    val selectedLib = libraries.find { it.id == selectedLibraryId }
                    val titleText = selectedLib?.name ?: "Select Library"
                    
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { libraryMenuExpanded = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = titleText,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Library",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        DropdownMenu(
                            expanded = libraryMenuExpanded,
                            onDismissRequest = { libraryMenuExpanded = false }
                        ) {
                            libraries.forEach { lib ->
                                DropdownMenuItem(
                                    text = { Text(lib.name) },
                                    onClick = {
                                        viewModel.setLibraryId(lib.id)
                                        libraryMenuExpanded = false
                                    },
                                    trailingIcon = if (lib.id == selectedLibraryId) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }
                },
                actions = {
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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh(forceRefresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
            ScrollableTabRow(
                selectedTabIndex = visibleTabs.indexOf(currentTab).coerceAtLeast(0),
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                divider = { HorizontalDivider(color = Color(0x11FFFFFF)) }
            ) {
                visibleTabs.forEach { tab ->
                    val selected = tab == currentTab
                    Tab(
                        selected = selected,
                        onClick = { viewModel.setCurrentTab(tab) },
                        text = {
                            Text(
                                text = when (tab) {
                                    LibraryTab.BOOKS -> "Books"
                                    LibraryTab.SERIES -> "Series"
                                    LibraryTab.COLLECTIONS -> "Collections"
                                    LibraryTab.AUTHORS -> "Authors"
                                    LibraryTab.PLAYLISTS -> "Playlists"
                                },
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            when (currentTab) {
                LibraryTab.BOOKS -> {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
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
                                    ReadStatusFilter.entries.forEach { filter ->
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

                    val refreshLoadState = lazyBookCards.loadState.refresh

                    if (selectedLibraryId.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No library selected. Please select a library.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (refreshLoadState is LoadState.Loading && lazyBookCards.itemCount == 0) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (refreshLoadState is LoadState.Error && lazyBookCards.itemCount == 0) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Error loading books: ${(refreshLoadState as LoadState.Error).error.message}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else if (lazyBookCards.itemCount == 0) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No books found.",
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
                            items(
                                count = lazyBookCards.itemCount,
                                key = lazyBookCards.itemKey { it.id },
                                contentType = lazyBookCards.itemContentType { "BookCard" }
                            ) { index ->
                                val bookCard = lazyBookCards[index]
                                if (bookCard != null) {
                                    BookCard(
                                        book = bookCard,
                                        onClick = { onBookClick(bookCard.id) }
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    NorseStubTabContent(tab = currentTab)
                }
            }
        }
    }
}
}

@Composable
fun BookCard(
    book: BookCardUiModel,
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
                    .aspectRatio(1f) // 1:1 square aspect ratio
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(book.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = book.title,
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
                                text = book.title.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
                
                // Read badge (top-start / top-left) - icon-only, no text label
                val progress = book.progress
                val isFinished = progress?.isFinished == true || (progress != null && progress.progress >= 0.99f)
                if (isFinished) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color(0x99000000), CircleShape)
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Read Completed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Downloaded badge (top-end / top-right) - icon-only, no text label
                if (book.isDownloaded) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color(0x99000000), CircleShape)
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflinePin,
                            contentDescription = "Downloaded Offline",
                            tint = Color.Green,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Progress Bar at the bottom of the cover
                if (progress != null && progress.progress > 0f && !isFinished) {
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

@Composable
fun SettingsDialog(
    currentInterval: Int,
    onIntervalSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Automatic Sync Interval", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                val options = listOf(
                    0 to "Disabled",
                    1 to "1 Hour",
                    6 to "6 Hours",
                    12 to "12 Hours",
                    24 to "24 Hours (Default)",
                    48 to "48 Hours",
                    72 to "72 Hours"
                )
                
                options.forEach { (hours, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIntervalSelected(hours) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentInterval == hours,
                            onClick = { onIntervalSelected(hours) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun NorseStubTabContent(tab: LibraryTab) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val icon = when (tab) {
                    LibraryTab.SERIES -> Icons.Default.List
                    LibraryTab.COLLECTIONS -> Icons.Default.Folder
                    LibraryTab.AUTHORS -> Icons.Default.Person
                    LibraryTab.PLAYLISTS -> Icons.Default.MusicNote
                    else -> Icons.Default.Settings
                }
                
                val title = when (tab) {
                    LibraryTab.SERIES -> "Saga Series"
                    LibraryTab.COLLECTIONS -> "Hoards & Treasures"
                    LibraryTab.AUTHORS -> "Skalds & Masters"
                    LibraryTab.PLAYLISTS -> "War Chants & Playlists"
                    else -> ""
                }
                
                val description = when (tab) {
                    LibraryTab.SERIES -> "Gather the threads of continuous sagas. Soon you will trace the exploits of heroes across sequential volumes."
                    LibraryTab.COLLECTIONS -> "Group your scrolls and codices by subject or event. Your curated hoards will appear here once the builders complete their work."
                    LibraryTab.AUTHORS -> "Browse the lore-masters, poets, and writers of the realm. A directory of all storytellers will soon be unveiled."
                    LibraryTab.PLAYLISTS -> "Queue up your chosen tracts for the march or the voyage. Soon you will forge custom paths of listening."
                    else -> ""
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
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
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "COMING SOON",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
