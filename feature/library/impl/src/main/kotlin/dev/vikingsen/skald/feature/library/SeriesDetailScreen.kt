package dev.vikingsen.skald.feature.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import dev.vikingsen.skald.core.model.formatDuration
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
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    seriesId: String,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: SeriesDetailViewModel = koinViewModel()
) {
    LaunchedEffect(seriesId) {
        viewModel.setSeriesId(seriesId)
    }

    val series by viewModel.series.collectAsState()
    val books by viewModel.books.collectAsState()
    val showMiniPlayer by viewModel.showMiniPlayer.collectAsState()

    var activeBookForMenu by remember { mutableStateOf<BookCardUiModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Series Details") },
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
                SeriesHeader(
                    series = series,
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
            libraryId = series?.libraryId ?: "",
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
fun SeriesHeader(
    series: dev.vikingsen.skald.core.model.Series?,
    books: List<BookCardUiModel>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // 1:1 Collage Cover artwork centered
        if (series != null) {
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
                    val covers = books.take(3).map { it.coverUrl }
                    val authHeader = books.firstOrNull()?.authorizationHeader
                    SeriesCoverCollage(
                        covers = covers,
                        seriesName = series.name,
                        authorizationHeader = authHeader,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Series Name
            Text(
                text = series.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics and Progress
            val totalBooks = series.bookCount.coerceAtLeast(books.size)
            val readBooks = books.count { it.progress?.isFinished == true || (it.progress?.progress ?: 0f) >= 0.99f }
            val progressFraction = if (totalBooks > 0) readBooks.toFloat() / totalBooks else 0f
            val progressPercent = (progressFraction * 100).toInt()

            val progressText = if (readBooks == totalBooks && totalBooks > 0) {
                "Completed"
            } else {
                "$readBooks/$totalBooks read ($progressPercent%)"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$totalBooks books in series",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = progressText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (readBooks == totalBooks && totalBooks > 0) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Description
            val description = series.description
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
                    text = "Loading series metadata...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookRowItem(
    book: BookCardUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Cover
            Box(
                modifier = Modifier
                    .size(50.dp, 70.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.DarkGray)
            ) {
                val context = LocalContext.current
                val imageRequest = remember(book.coverUrl) {
                    ImageRequest.Builder(context)
                        .data(book.coverUrl)
                        .crossfade(true)
                        .build()
                }
                SubcomposeAsyncImage(
                    model = imageRequest,
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
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (!book.seriesSequence.isNullOrEmpty()) {
                    Text(
                        text = "Book ${book.seriesSequence}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = book.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = book.author,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (book.progress != null) {
                    val isFinished = book.progress.isFinished || book.progress.progress >= 0.99f
                    Spacer(modifier = Modifier.height(6.dp))
                    if (isFinished) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Completed",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else if (book.progress.progress > 0f) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { book.progress.progress },
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color(0x33FFFFFF)
                            )
                            Text(
                                text = "${(book.progress.progress * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
