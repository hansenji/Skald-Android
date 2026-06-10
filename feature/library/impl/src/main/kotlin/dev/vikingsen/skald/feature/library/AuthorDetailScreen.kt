package dev.vikingsen.skald.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.vikingsen.skald.core.model.Author
import dev.vikingsen.skald.core.model.formatDuration
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorDetailScreen(
    authorId: String,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: AuthorDetailViewModel = koinViewModel()
) {
    LaunchedEffect(authorId) {
        viewModel.setAuthorId(authorId)
    }

    val author by viewModel.author.collectAsState()
    val books by viewModel.books.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()
    val showMiniPlayer by viewModel.showMiniPlayer.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var activeBookForMenu by remember { mutableStateOf<BookCardUiModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Author Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (error != null) {
                    item {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                item {
                    AuthorHeader(
                        author = author,
                        booksCount = books.size
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
                            if (isRefreshing) {
                                CircularProgressIndicator()
                            } else {
                                Text(
                                    text = "No cached books found for this author.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "Books",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
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
            onCreatePlaylist = { name -> viewModel.createPlaylistAndAdd(name, book.id) }
        )
    }
}

@Composable
fun AuthorHeader(
    author: Author?,
    booksCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (author != null) {
            // Large circular avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val context = LocalContext.current
                val serverUrl = author.imagePath?.let { "" } // imagePath mapping will be handled
                // We'll compute the avatarUrl
                val avatarUrl = if (!author.imagePath.isNullOrEmpty()) {
                    author.imagePath
                } else {
                    null
                }

                if (avatarUrl != null) {
                    val imageRequest = remember(avatarUrl) {
                        ImageRequest.Builder(context)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build()
                    }
                    SubcomposeAsyncImage(
                        model = imageRequest,
                        contentDescription = author.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = {
                            InitialAvatar(name = author.name)
                        }
                    )
                } else {
                    InitialAvatar(name = author.name)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Author Name
            Text(
                text = author.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Book count subtitle
            Text(
                text = "${author.bookCount.coerceAtLeast(booksCount)} books",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val description = author.description
            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                // Expansible description text
                ExpandableText(
                    text = description.parseHtml(),
                    modifier = Modifier.fillMaxWidth()
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
                    text = "Loading author metadata...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun InitialAvatar(
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
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ExpandableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 4
) {
    var expanded by remember { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val isTruncated = textLayoutResult?.let { layout ->
        if (layout.lineCount > collapsedMaxLines - 1) {
            layout.isLineEllipsized(collapsedMaxLines - 1)
        } else {
            false
        }
    } ?: false

    Column(modifier = modifier) {
        Text(
            text = text,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult = it },
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
        if (isTruncated || expanded) {
            Text(
                text = if (expanded) "Show Less" else "Read More",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp)
            )
        }
    }
}
