package dev.vikingsen.skald.feature.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.skald.feature.assistant.engine.AssistantResponse
import dev.vikingsen.skald.feature.assistant.engine.LocalAgentCoordinator
import dev.vikingsen.skald.feature.assistant.mcp.McpToolResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AssistantMessage {
    val id: String
    val timestamp: Long

    data class User(
        override val id: String = UUID.randomUUID().toString(),
        val text: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : AssistantMessage

    data class Assistant(
        override val id: String = UUID.randomUUID().toString(),
        val text: String,
        val toolName: String? = null,
        val isError: Boolean = false,
        override val timestamp: Long = System.currentTimeMillis()
    ) : AssistantMessage
}

data class PendingConfirmation(
    val id: String = UUID.randomUUID().toString(),
    val toolName: String,
    val arguments: Map<String, Any?> = emptyMap(),
    val title: String,
    val description: String,
    val targetId: String
)

data class AssistantUiState(
    val messages: List<AssistantMessage> = emptyList(),
    val pendingConfirmation: PendingConfirmation? = null,
    val isListening: Boolean = false,
    val isExecuting: Boolean = false,
    val speechError: String? = null
)

class AssistantViewModel(
    private val coordinator: LocalAgentCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AssistantUiState(
            messages = listOf(
                AssistantMessage.Assistant(
                    text = "Hello! I can help you search, play, manage downloads, set sleep timers, and control playback. What would you like to listen to?"
                )
            )
        )
    )
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    fun submitQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        val userMessage = AssistantMessage.User(text = trimmed)
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                isExecuting = true,
                speechError = null
            )
        }

        viewModelScope.launch {
            try {
                when (val response = coordinator.processQuery(trimmed)) {
                    is AssistantResponse.ImmediateExecution -> {
                        val (messageText, isError) = when (val res = response.result) {
                            is McpToolResult.Success -> res.message to false
                            is McpToolResult.Error -> res.errorMessage to true
                        }
                        val assistantMsg = AssistantMessage.Assistant(
                            text = messageText,
                            toolName = response.toolName,
                            isError = isError
                        )
                        _uiState.update { it.copy(messages = it.messages + assistantMsg, isExecuting = false) }
                    }
                    is AssistantResponse.PendingConfirmation -> {
                        val pending = PendingConfirmation(
                            toolName = response.toolName,
                            arguments = response.arguments,
                            title = response.promptTitle,
                            description = response.promptDetails,
                            targetId = response.targetId
                        )
                        _uiState.update { it.copy(pendingConfirmation = pending, isExecuting = false) }
                    }
                    is AssistantResponse.Conversational -> {
                        val assistantMsg = AssistantMessage.Assistant(text = response.replyText)
                        _uiState.update { it.copy(messages = it.messages + assistantMsg, isExecuting = false) }
                    }
                }
            } catch (e: Exception) {
                val errorMsg = AssistantMessage.Assistant(
                    text = e.message ?: "An unexpected error occurred.",
                    isError = true
                )
                _uiState.update { it.copy(messages = it.messages + errorMsg, isExecuting = false) }
            }
        }
    }

    fun confirmPendingAction() {
        val pending = _uiState.value.pendingConfirmation ?: return
        _uiState.update { it.copy(pendingConfirmation = null, isExecuting = true) }

        viewModelScope.launch {
            try {
                val result = coordinator.executeConfirmedTool(pending.toolName, pending.arguments)
                val (text, isError) = when (result) {
                    is McpToolResult.Success -> result.message to false
                    is McpToolResult.Error -> result.errorMessage to true
                }
                val assistantMsg = AssistantMessage.Assistant(
                    text = text,
                    toolName = pending.toolName,
                    isError = isError
                )
                _uiState.update { it.copy(messages = it.messages + assistantMsg, isExecuting = false) }
            } catch (e: Exception) {
                val errorMsg = AssistantMessage.Assistant(
                    text = "Failed to execute: ${e.message}",
                    isError = true
                )
                _uiState.update { it.copy(messages = it.messages + errorMsg, isExecuting = false) }
            }
        }
    }

    fun dismissPendingAction() {
        _uiState.update { state ->
            state.copy(
                pendingConfirmation = null,
                messages = state.messages + AssistantMessage.Assistant(text = "Action cancelled.")
            )
        }
    }

    fun startListening(context: Context) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _uiState.update { it.copy(speechError = "Speech recognition is not available.") }
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _uiState.update { it.copy(isListening = true, speechError = null) }
                    }

                    override fun onResults(results: Bundle?) {
                        _uiState.update { it.copy(isListening = false) }
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            submitQuery(text)
                        }
                    }

                    override fun onError(error: Int) {
                        _uiState.update { it.copy(isListening = false) }
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _uiState.update { it.copy(isListening = false) }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _uiState.update { it.copy(isListening = false, speechError = e.message) }
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _uiState.update { it.copy(isListening = false) }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
