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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.text.style.TextAlign
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import dev.vikingsen.skald.core.model.ReadStatusFilter
import dev.vikingsen.skald.core.model.SortOption
import dev.vikingsen.skald.core.model.SeriesFilter
import dev.vikingsen.skald.core.model.SeriesSortOption
import dev.vikingsen.skald.core.model.Author
import dev.vikingsen.skald.core.model.AuthorsSortOption
import dev.vikingsen.skald.core.model.BookCollection
import dev.vikingsen.skald.core.model.CollectionsSortOption
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.PlayArrow
import dev.vikingsen.skald.core.model.Playlist
import dev.vikingsen.skald.core.model.PlaylistsSortOption
import dev.vikingsen.skald.core.model.formatDuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.MoreVert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onSeriesClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onCollectionClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onPlayClick: () -> Unit,
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

    var activeBookForMenu by remember { mutableStateOf<BookCardUiModel?>(null) }
    val playlists by viewModel.allPlaylists.collectAsState()

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
                        LibraryEmptyState(
                            icon = Icons.Default.Folder,
                            title = "No Library Selected",
                            description = "Please select a library or refresh to load available libraries.",
                            buttonText = "Refresh Libraries",
                            onButtonClick = { viewModel.refresh(forceRefresh = true) }
                        )
                    } else if (refreshLoadState is LoadState.Loading && lazyBookCards.itemCount == 0) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (refreshLoadState is LoadState.Error && lazyBookCards.itemCount == 0) {
                        val loadError = refreshLoadState.error
                        LibraryEmptyState(
                            icon = Icons.Default.Error,
                            title = "Error Loading Books",
                            description = loadError.message ?: "Unknown error occurred.",
                            buttonText = "Sync Now",
                            onButtonClick = { viewModel.refresh(forceRefresh = true) }
                        )
                    } else if (lazyBookCards.itemCount == 0) {
                        LibraryEmptyState(
                            icon = Icons.Default.Folder,
                            title = "No Books Found",
                            description = "There are no books in this library, or sync is required.",
                            buttonText = "Sync Now",
                            onButtonClick = { viewModel.refresh(forceRefresh = true) }
                        )
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
                                        onClick = { onBookClick(bookCard.id) },
                                        onLongClick = { activeBookForMenu = bookCard },
                                        onMenuClick = { activeBookForMenu = bookCard }
                                    )
                                }
                            }
                        }
                    }
                }
                LibraryTab.SERIES -> {
                    SeriesTabContent(
                        onSeriesClick = onSeriesClick,
                        viewModel = viewModel
                    )
                }
                LibraryTab.COLLECTIONS -> {
                    CollectionsTabContent(
                        onCollectionClick = onCollectionClick,
                        viewModel = viewModel
                    )
                }
                LibraryTab.AUTHORS -> {
                    AuthorsTabContent(
                        onAuthorClick = onAuthorClick,
                        viewModel = viewModel
                    )
                }
                LibraryTab.PLAYLISTS -> {
                    PlaylistsTabContent(
                        onPlaylistClick = onPlaylistClick,
                        onPlayClick = onPlayClick,
                        viewModel = viewModel
                    )
                }
                else -> {
                    NorseStubTabContent(tab = currentTab)
                }
            }
        }
    }

    if (activeBookForMenu != null) {
        val book = activeBookForMenu!!
        val bookDetail = BookDetailUiModel(
            id = book.id,
            title = book.title,
            author = book.author,
            narrator = book.narrator,
            duration = book.duration,
            durationText = formatDuration(book.duration),
            coverUrl = book.coverUrl,
            authorizationHeader = book.authorizationHeader,
            isDownloaded = book.isDownloaded,
            description = "",
            chapters = emptyList(),
            progress = book.progress,
            progressLeftText = book.progress?.let {
                val left = book.duration - it.currentTime
                formatDuration(left)
            }
        )
        ItemMoreMenuBottomSheet(
            book = bookDetail,
            serverUrl = viewModel.serverUrl,
            playlists = playlists,
            onDismiss = { activeBookForMenu = null },
            onToggleFinished = { viewModel.toggleFinished(book) },
            onDiscardProgress = { viewModel.discardProgress(book.id) },
            onDeleteDownload = { viewModel.deleteDownloadedBook(book.id) },
            onAddToPlaylist = { playlistId -> viewModel.addToPlaylist(playlistId, book.id) },
            onCreatePlaylist = { name -> viewModel.createPlaylistAndAdd(name, selectedLibraryId, book.id) }
        )
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesTabContent(
    onSeriesClick: (String) -> Unit,
    viewModel: LibraryViewModel
) {
    val seriesList by viewModel.series.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filter by viewModel.seriesFilter.collectAsState()
    val sort by viewModel.seriesSort.collectAsState()
    val showMiniPlayer by viewModel.showMiniPlayer.collectAsState()

    var statusMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search series by name...") },
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
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    FilterChip(
                        selected = filter != SeriesFilter.ALL,
                        onClick = { statusMenuExpanded = true },
                        label = {
                            Text(
                                text = when (filter) {
                                    SeriesFilter.ALL -> "All Series"
                                    SeriesFilter.IN_PROGRESS -> "In Progress"
                                    SeriesFilter.COMPLETED -> "Completed"
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
                            selected = filter != SeriesFilter.ALL,
                            borderColor = Color(0x33FFFFFF),
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        SeriesFilter.entries.forEach { statusFilter ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when (statusFilter) {
                                            SeriesFilter.ALL -> "All Series"
                                            SeriesFilter.IN_PROGRESS -> "In Progress"
                                            SeriesFilter.COMPLETED -> "Completed"
                                        }
                                    )
                                },
                                onClick = {
                                    viewModel.setSeriesFilter(statusFilter)
                                    statusMenuExpanded = false
                                },
                                trailingIcon = if (filter == statusFilter) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
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
                        contentDescription = "Sort Series",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    SeriesSortOption.entries.forEach { sortOption ->
                        val label = when (sortOption) {
                            SeriesSortOption.NAME_ASC -> "Name (A-Z)"
                            SeriesSortOption.NAME_DESC -> "Name (Z-A)"
                            SeriesSortOption.BOOKS_COUNT_DESC -> "Books Count"
                            SeriesSortOption.RECENTLY_UPDATED -> "Recently Updated"
                        }
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.setSeriesSort(sortOption)
                                sortMenuExpanded = false
                            },
                            trailingIcon = if (sort == sortOption) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
        }

        if (seriesList.isEmpty()) {
            LibraryEmptyState(
                icon = Icons.Default.List,
                title = "No Series Found",
                description = "There are no series in this library, or sync is required.",
                buttonText = "Sync Now",
                onButtonClick = { viewModel.refresh(forceRefresh = true) }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = if (showMiniPlayer) 80.dp else 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(seriesList.size, key = { index -> seriesList[index].id }) { index ->
                    val seriesCard = seriesList[index]
                    SeriesCard(
                        series = seriesCard,
                        onClick = { onSeriesClick(seriesCard.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SeriesCard(
    series: SeriesCardUiModel,
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
            SeriesCoverCollage(
                covers = series.covers,
                seriesName = series.name,
                authorizationHeader = series.authorizationHeader,
                modifier = Modifier.fillMaxWidth()
            )

            // Progress Bar at the bottom of the collage
            if (series.progress > 0f && series.progress < 1f) {
                LinearProgressIndicator(
                    progress = { series.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0x33FFFFFF)
                )
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = series.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                val progressText = if (series.readBookCount == series.bookCount && series.bookCount > 0) {
                    "Completed"
                } else {
                    "${series.readBookCount}/${series.bookCount} read"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${series.bookCount} books",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = progressText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (series.readBookCount == series.bookCount && series.bookCount > 0) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SeriesCoverCollage(
    covers: List<String>,
    seriesName: String,
    authorizationHeader: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color.DarkGray)
    ) {
        if (covers.isEmpty()) {
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
                    text = seriesName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (covers.size == 1) {
            AsyncCoverImage(
                coverUrl = covers[0],
                authHeader = authorizationHeader,
                contentDescription = seriesName,
                modifier = Modifier.fillMaxSize()
            )
        } else if (covers.size == 2) {
            Row(modifier = Modifier.fillMaxSize()) {
                AsyncCoverImage(
                    coverUrl = covers[0],
                    authHeader = authorizationHeader,
                    contentDescription = seriesName,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                AsyncCoverImage(
                    coverUrl = covers[1],
                    authHeader = authorizationHeader,
                    contentDescription = seriesName,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                AsyncCoverImage(
                    coverUrl = covers[0],
                    authHeader = authorizationHeader,
                    contentDescription = seriesName,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AsyncCoverImage(
                        coverUrl = covers[1],
                        authHeader = authorizationHeader,
                        contentDescription = seriesName,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                    Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.Black))
                    AsyncCoverImage(
                        coverUrl = covers[2],
                        authHeader = authorizationHeader,
                        contentDescription = seriesName,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun AsyncCoverImage(
    coverUrl: String,
    authHeader: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(coverUrl) {
        ImageRequest.Builder(context)
            .data(coverUrl)
            .crossfade(true)
            .build()
    }
    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: BookCardUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
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
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Book Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
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

@Composable
fun LibraryEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Box(
        modifier = modifier.verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (buttonText != null && onButtonClick != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onButtonClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(buttonText)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorsTabContent(
    onAuthorClick: (String) -> Unit,
    viewModel: LibraryViewModel
) {
    val authorsList by viewModel.authors.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sort by viewModel.authorsSort.collectAsState()
    val showMiniPlayer by viewModel.showMiniPlayer.collectAsState()

    var sortMenuExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val alphabet = ('A'..'Z').toList()
    
    val letterToIndex = remember(authorsList) {
        alphabet.associateWith { letter ->
            authorsList.indexOfFirst { it.name.trimStart().startsWith(letter, ignoreCase = true) }
        }.filterValues { it != -1 }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search authors by name...") },
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${authorsList.size} Authors",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                        contentDescription = "Sort Authors",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    AuthorsSortOption.entries.forEach { sortOption ->
                        val label = when (sortOption) {
                            AuthorsSortOption.NAME_ASC -> "Name (A-Z)"
                            AuthorsSortOption.NAME_DESC -> "Name (Z-A)"
                            AuthorsSortOption.BOOKS_COUNT_DESC -> "Books Count"
                        }
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.setAuthorsSort(sortOption)
                                sortMenuExpanded = false
                            },
                            trailingIcon = if (sort == sortOption) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
        }

        if (authorsList.isEmpty()) {
            LibraryEmptyState(
                icon = Icons.Default.Person,
                title = "No Authors Found",
                description = "There are no authors in this library, or sync is required.",
                buttonText = "Sync Now",
                onButtonClick = { viewModel.refresh(forceRefresh = true) }
            )
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 24.dp),
                    contentPadding = PaddingValues(bottom = if (showMiniPlayer) 80.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(authorsList, key = { it.id }) { author ->
                        AuthorRowItem(
                            author = author,
                            onClick = { onAuthorClick(author.id) }
                        )
                    }
                }

                // A-Z Scroller Strip on the right
                AlphabetScroller(
                    alphabet = alphabet,
                    letterToIndex = letterToIndex,
                    onLetterClick = { index ->
                        coroutineScope.launch {
                            listState.scrollToItem(index)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(24.dp)
                        .padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
fun AlphabetScroller(
    alphabet: List<Char>,
    letterToIndex: Map<Char, Int>,
    onLetterClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        alphabet.forEach { letter ->
            val targetIndex = letterToIndex[letter]
            val isEnabled = targetIndex != null
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(enabled = isEnabled) { targetIndex?.let { onLetterClick(it) } },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)
                )
            }
        }
    }
}


@Composable
fun AuthorRowItem(
    author: Author,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Author Avatar (48.dp circular)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                if (!author.imagePath.isNullOrEmpty()) {
                    val context = LocalContext.current
                    val imageRequest = remember(author.imagePath) {
                        ImageRequest.Builder(context)
                            .data(author.imagePath)
                            .crossfade(true)
                            .build()
                    }
                    SubcomposeAsyncImage(
                        model = imageRequest,
                        contentDescription = author.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = {
                            InitialAvatarMini(name = author.name)
                        }
                    )
                } else {
                    InitialAvatarMini(name = author.name)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Author details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = author.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${author.bookCount} books",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InitialAvatarMini(
    name: String,
    modifier: Modifier = Modifier
) {
    val brandColors = listOf(
        Color(0xFFBB86FC), // ElectricPurple
        Color(0xFF03DAC6), // CyanAccent
        Color(0xFFFF79C6)  // SoftPink
    )
    val hashCode = name.hashCode()
    val index = if (hashCode == Int.MIN_VALUE) 0 else kotlin.math.abs(hashCode) % brandColors.size
    val bgColor = brandColors[index]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().take(1).uppercase(),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsTabContent(
    onCollectionClick: (String) -> Unit,
    viewModel: LibraryViewModel
) {
    val collectionsList by viewModel.collections.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sort by viewModel.collectionsSort.collectAsState()
    val showMiniPlayer by viewModel.showMiniPlayer.collectAsState()

    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search collections by name...") },
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
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        contentDescription = "Sort Collections",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    CollectionsSortOption.entries.forEach { sortOption ->
                        val label = when (sortOption) {
                            CollectionsSortOption.NAME_ASC -> "Name (A-Z)"
                            CollectionsSortOption.NAME_DESC -> "Name (Z-A)"
                            CollectionsSortOption.BOOKS_COUNT_DESC -> "Books Count"
                            CollectionsSortOption.LAST_MODIFIED -> "Recently Updated"
                        }
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.setCollectionsSort(sortOption)
                                sortMenuExpanded = false
                            },
                            trailingIcon = if (sort == sortOption) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
        }

        if (collectionsList.isEmpty()) {
            LibraryEmptyState(
                icon = Icons.Default.Folder,
                title = "No Collections Found",
                description = "There are no collections in this library, or sync is required.",
                buttonText = "Sync Now",
                onButtonClick = { viewModel.refresh(forceRefresh = true) }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = if (showMiniPlayer) 80.dp else 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(collectionsList.size, key = { index -> collectionsList[index].id }) { index ->
                    val collectionCard = collectionsList[index]
                    CollectionCard(
                        collection = collectionCard,
                        onClick = { onCollectionClick(collectionCard.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CollectionCard(
    collection: BookCollection,
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
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                val covers = collection.bookCovers.take(4)
                CollectionCoverCollage(
                    covers = covers,
                    collectionName = collection.name,
                    authorizationHeader = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = collection.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                val bookCountText = if (collection.bookIds.size == 1) "1 book" else "${collection.bookIds.size} books"
                Text(
                    text = bookCountText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsTabContent(
    onPlaylistClick: (String) -> Unit,
    onPlayClick: () -> Unit,
    viewModel: LibraryViewModel
) {
    val playlistsList by viewModel.playlists.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sort by viewModel.playlistsSort.collectAsState()
    val showMiniPlayer by viewModel.showMiniPlayer.collectAsState()

    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search playlists by name or description...") },
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${playlistsList.size} Playlists",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                        contentDescription = "Sort Playlists",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    PlaylistsSortOption.entries.forEach { sortOption ->
                        val label = when (sortOption) {
                            PlaylistsSortOption.NAME_ASC -> "Name (A-Z)"
                            PlaylistsSortOption.NAME_DESC -> "Name (Z-A)"
                            PlaylistsSortOption.TRACKS_COUNT_DESC -> "Tracks Count"
                            PlaylistsSortOption.DURATION_DESC -> "Total Duration"
                        }
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.setPlaylistsSort(sortOption)
                                sortMenuExpanded = false
                            },
                            trailingIcon = if (sort == sortOption) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
        }

        if (playlistsList.isEmpty()) {
            LibraryEmptyState(
                icon = Icons.Default.Folder,
                title = "No Playlists Found",
                description = "There are no playlists in this library, or sync is required.",
                buttonText = "Sync Now",
                onButtonClick = { viewModel.refresh(forceRefresh = true) }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = if (showMiniPlayer) 80.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playlistsList, key = { it.id }) { playlist ->
                    PlaylistRowItem(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.id) },
                        onPlayPlaylistClick = {
                            viewModel.playPlaylist(playlist)
                            onPlayClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistRowItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onPlayPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail: rounded square, generic playlist icon with cyan accent
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2C2C2C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = null,
                    tint = Color(0xFF03DAC6), // CyanAccent
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val description = playlist.description
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                val trackText = if (playlist.itemCount == 1) "1 track" else "${playlist.itemCount} tracks"
                Text(
                    text = "$trackText • ${formatDuration(playlist.duration)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play button
            IconButton(
                onClick = onPlayPlaylistClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play Playlist",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


