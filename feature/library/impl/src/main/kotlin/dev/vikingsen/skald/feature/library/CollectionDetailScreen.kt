package dev.vikingsen.skald.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vikingsen.skald.core.model.formatDuration
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    collectionId: String,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: CollectionDetailViewModel = koinViewModel()
) {
    LaunchedEffect(collectionId) {
        viewModel.setCollectionId(collectionId)
    }

    val collection by viewModel.collection.collectAsState()
    val books by viewModel.books.collectAsState()
    val showMiniPlayer by viewModel.showMiniPlayer.collectAsState()

    var activeBookForMenu by remember { mutableStateOf<BookCardUiModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collection Details") },
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
        val bottomPadding = if (showMiniPlayer) 80.dp else 16.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CollectionHeader(
                    collection = collection,
                    books = books
                )
            }

            if (books.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(books, key = { it.id }) { book ->
                    BookRowItem(
                        book = book,
                        onClick = { onBookClick(book.id) },
                        onLongClick = { activeBookForMenu = book },
                        onMenuClick = { activeBookForMenu = book }
                    )
                }
            }
        }
    }

    if (activeBookForMenu != null) {
        val book = activeBookForMenu!!
        val bookDetail = BookDetailUiModel(
            id = book.id,
            libraryId = collection?.libraryId ?: "",
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
            onDismiss = { activeBookForMenu = null },
            onToggleFinished = { viewModel.toggleFinished(book) },
            onDiscardProgress = { viewModel.discardProgress(book.id) },
            onDeleteDownload = { viewModel.deleteDownloadedBook(book.id) }
        )
    }
}

@Composable
fun CollectionHeader(
    collection: dev.vikingsen.skald.core.model.BookCollection?,
    books: List<BookCardUiModel>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        if (collection != null) {
            // 1:1 Collage Cover artwork centered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    val covers = collection.bookCovers.take(4)
                    val authHeader = books.firstOrNull()?.authorizationHeader
                    CollectionCoverCollage(
                        covers = covers,
                        collectionName = collection.name,
                        authorizationHeader = authHeader,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Collection Name
            Text(
                text = collection.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics
            val totalBooks = collection.bookIds.size
            val totalDuration = books.sumOf { it.duration }
            val formattedTime = formatDuration(totalDuration)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$totalBooks books in collection",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Duration: $formattedTime",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Description
            val description = collection.description
            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Description",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description.parseHtml(),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        } else {
            // Loading placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading collection metadata...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun CollectionCoverCollage(
    covers: List<String>,
    collectionName: String,
    authorizationHeader: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
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
                    text = collectionName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (covers.size == 1) {
            AsyncCoverImage(
                coverUrl = covers[0],
                authHeader = authorizationHeader,
                contentDescription = collectionName,
                modifier = Modifier.fillMaxSize()
            )
        } else if (covers.size == 2) {
            Row(modifier = Modifier.fillMaxSize()) {
                AsyncCoverImage(
                    coverUrl = covers[0],
                    authHeader = authorizationHeader,
                    contentDescription = collectionName,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                AsyncCoverImage(
                    coverUrl = covers[1],
                    authHeader = authorizationHeader,
                    contentDescription = collectionName,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else if (covers.size == 3) {
            Row(modifier = Modifier.fillMaxSize()) {
                AsyncCoverImage(
                    coverUrl = covers[0],
                    authHeader = authorizationHeader,
                    contentDescription = collectionName,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AsyncCoverImage(
                        coverUrl = covers[1],
                        authHeader = authorizationHeader,
                        contentDescription = collectionName,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                    Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.Black))
                    AsyncCoverImage(
                        coverUrl = covers[2],
                        authHeader = authorizationHeader,
                        contentDescription = collectionName,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        } else {
            // 2x2 grid
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AsyncCoverImage(
                        coverUrl = covers[0],
                        authHeader = authorizationHeader,
                        contentDescription = collectionName,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                    AsyncCoverImage(
                        coverUrl = covers[1],
                        authHeader = authorizationHeader,
                        contentDescription = collectionName,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.Black))
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AsyncCoverImage(
                        coverUrl = covers[2],
                        authHeader = authorizationHeader,
                        contentDescription = collectionName,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                    AsyncCoverImage(
                        coverUrl = covers[3],
                        authHeader = authorizationHeader,
                        contentDescription = collectionName,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}
