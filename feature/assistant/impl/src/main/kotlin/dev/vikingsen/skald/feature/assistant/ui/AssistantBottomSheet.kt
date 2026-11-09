package dev.vikingsen.skald.feature.assistant.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.vikingsen.skald.feature.assistant.AssistantMessage
import dev.vikingsen.skald.feature.assistant.AssistantViewModel
import dev.vikingsen.skald.feature.assistant.PendingConfirmation
import dev.vikingsen.skald.feature.assistant.R
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AssistantViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.stopListening()
            onDismiss()
        },
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            AssistantHeader(
                isListening = uiState.isListening,
                isExecuting = uiState.isExecuting,
                onClose = {
                    viewModel.stopListening()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Conversation Stream
            val listState = rememberLazyListState()
            LaunchedEffect(uiState.messages.size, uiState.pendingConfirmation) {
                if (uiState.messages.isNotEmpty()) {
                    listState.animateScrollToItem(uiState.messages.size - 1)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 120.dp, max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    when (message) {
                        is AssistantMessage.User -> UserMessageBubble(text = message.text)
                        is AssistantMessage.Assistant -> AssistantMessageBubble(
                            text = message.text,
                            toolName = message.toolName,
                            isError = message.isError
                        )
                    }
                }

                uiState.pendingConfirmation?.let { pending ->
                    item(key = pending.id) {
                        ConfirmationCard(
                            pending = pending,
                            onConfirm = viewModel::confirmPendingAction,
                            onDismiss = viewModel::dismissPendingAction
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Suggestion Chips
            SuggestedActionChips(
                onChipClicked = viewModel::submitQuery
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Input Row
            AssistantInputRow(
                isListening = uiState.isListening,
                onToggleVoice = {
                    if (uiState.isListening) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening(context)
                    }
                },
                onSubmit = viewModel::submitQuery
            )
        }
    }
}

@Composable
private fun AssistantHeader(
    isListening: Boolean,
    isExecuting: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.assistant_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Status chip
            val statusText = when {
                isListening -> stringResource(R.string.assistant_status_listening)
                isExecuting -> stringResource(R.string.assistant_status_processing)
                else -> stringResource(R.string.assistant_status_ready)
            }
            Surface(
                shape = CircleShape,
                color = when {
                    isListening -> MaterialTheme.colorScheme.errorContainer
                    isExecuting -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.assistant_close)
            )
        }
    }
}

@Composable
private fun UserMessageBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(start = 48.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun AssistantMessageBubble(
    text: String,
    toolName: String?,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 48.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (toolName != null) {
                    Text(
                        text = "Tool: $toolName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ConfirmationCard(
    pending: PendingConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = pending.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pending.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.assistant_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onConfirm) {
                    Text(stringResource(R.string.assistant_confirm_download))
                }
            }
        }
    }
}

@Composable
private fun SuggestedActionChips(
    onChipClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        item {
            SuggestionChip(
                onClick = { onChipClicked("Resume") },
                label = { Text(stringResource(R.string.assistant_chip_resume)) }
            )
        }
        item {
            SuggestionChip(
                onClick = { onChipClicked("Pause") },
                label = { Text(stringResource(R.string.assistant_chip_pause)) }
            )
        }
        item {
            SuggestionChip(
                onClick = { onChipClicked("Sleep timer 30 minutes") },
                label = { Text(stringResource(R.string.assistant_chip_sleep_timer)) }
            )
        }
        item {
            SuggestionChip(
                onClick = { onChipClicked("Speed 1.5") },
                label = { Text(stringResource(R.string.assistant_chip_speed)) }
            )
        }
        item {
            SuggestionChip(
                onClick = { onChipClicked("What's playing") },
                label = { Text(stringResource(R.string.assistant_chip_status)) }
            )
        }
    }
}

@Composable
private fun AssistantInputRow(
    isListening: Boolean,
    onToggleVoice: () -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var queryText by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by if (isListening) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Voice Button
        IconButton(
            onClick = onToggleVoice,
            modifier = Modifier
                .size(48.dp)
                .scale(pulseScale)
                .background(
                    color = if (isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = stringResource(
                    if (isListening) R.string.assistant_stop_voice else R.string.assistant_voice_input
                ),
                tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Text Field
        OutlinedTextField(
            value = queryText,
            onValueChange = { queryText = it },
            placeholder = { Text(stringResource(R.string.assistant_input_placeholder)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp)
        )

        // Send Button
        IconButton(
            onClick = {
                val text = queryText.trim()
                if (text.isNotBlank()) {
                    onSubmit(text)
                    queryText = ""
                }
            },
            enabled = queryText.isNotBlank(),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.assistant_send),
                tint = if (queryText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}
