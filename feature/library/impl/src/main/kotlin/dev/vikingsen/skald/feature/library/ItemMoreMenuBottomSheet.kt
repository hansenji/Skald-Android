package dev.vikingsen.skald.feature.library

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vikingsen.skald.core.model.Playlist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemMoreMenuBottomSheet(
    book: BookDetailUiModel,
    serverUrl: String,
    onDismiss: () -> Unit,
    onToggleFinished: () -> Unit,
    onDiscardProgress: () -> Unit,
    onDeleteDownload: () -> Unit,
    playlistId: String? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showConfirmFinishedDialog by remember { mutableStateOf(false) }
    var showConfirmDiscardDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    val isFinished = book.progress?.isFinished ?: false
    val progressPercent = book.progress?.progress ?: 0f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Mark as Finished / Unfinished
            ListItem(
                headlineContent = {
                    Text(if (isFinished) "Mark as Unfinished" else "Mark as Finished")
                },
                leadingContent = {
                    Icon(Icons.Default.Check, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    if (!isFinished && progressPercent < 1.0f) {
                        showConfirmFinishedDialog = true
                    } else {
                        onToggleFinished()
                        onDismiss()
                    }
                }
            )

            // Discard Progress (only if there is progress and the book is not finished)
            if (book.progress != null && !isFinished) {
                ListItem(
                    headlineContent = { Text("Discard Progress") },
                    leadingContent = {
                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        showConfirmDiscardDialog = true
                    }
                )
            }

            // Add to Playlist
            ListItem(
                headlineContent = { Text("Add to Playlist") },
                leadingContent = {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    showAddToPlaylistDialog = true
                }
            )

            // Remove from Playlist
            if (playlistId != null && onRemoveFromPlaylist != null) {
                ListItem(
                    headlineContent = { Text("Remove from Playlist") },
                    leadingContent = {
                        Icon(Icons.Default.Close, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        onRemoveFromPlaylist()
                        onDismiss()
                    }
                )
            }

            // Delete Download (only if downloaded)
            if (book.isDownloaded) {
                ListItem(
                    headlineContent = { Text("Delete Download") },
                    leadingContent = {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        onDeleteDownload()
                        onDismiss()
                    }
                )
            }

            // Go to Web Client
            if (serverUrl.isNotEmpty()) {
                ListItem(
                    headlineContent = { Text("Go to Web Client") },
                    leadingContent = {
                        Icon(Icons.Default.Language, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        val webUrl = "${serverUrl.trimEnd('/')}/item/${book.id}"
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                            context.startActivity(intent)
                        }
                        onDismiss()
                    }
                )
            }
        }
    }

    // Confirmation: Mark as Finished
    if (showConfirmFinishedDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmFinishedDialog = false },
            title = { Text("Confirm Finished") },
            text = { Text("Are you sure you want to mark this book as finished?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmFinishedDialog = false
                        onToggleFinished()
                        onDismiss()
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmFinishedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation: Discard Progress
    if (showConfirmDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDiscardDialog = false },
            title = { Text("Discard Progress") },
            text = { Text("Are you sure you want to discard progress?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDiscardDialog = false
                        onDiscardProgress()
                        onDismiss()
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Add to Playlist
    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            bookId = book.id,
            libraryId = book.libraryId,
            onDismiss = { showAddToPlaylistDialog = false },
            onDismissAll = {
                showAddToPlaylistDialog = false
                onDismiss()
            }
        )
    }
}
