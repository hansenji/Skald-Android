package dev.vikingsen.skald.feature.assistant

import dev.vikingsen.skald.feature.assistant.engine.AssistantResponse
import dev.vikingsen.skald.feature.assistant.engine.LocalAgentCoordinator
import dev.vikingsen.skald.feature.assistant.mcp.McpToolResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var coordinator: LocalAgentCoordinator
    private lateinit var viewModel: AssistantViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coordinator = mockk(relaxed = true)
        viewModel = AssistantViewModel(coordinator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has welcome message`() {
        val state = viewModel.uiState.value
        assertTrue(state.messages.isNotEmpty())
        assertTrue(state.messages.first() is AssistantMessage.Assistant)
        assertNull(state.pendingConfirmation)
    }

    @Test
    fun `submitQuery with immediate execution updates messages`() = runTest {
        coEvery { coordinator.processQuery("pause") } returns AssistantResponse.ImmediateExecution(
            toolName = "pauseCurrentPlayback",
            arguments = emptyMap(),
            result = McpToolResult.Success("Paused playback.")
        )

        viewModel.submitQuery("pause")

        val state = viewModel.uiState.value
        assertEquals(3, state.messages.size) // welcome + user + assistant
        val userMsg = state.messages[1] as AssistantMessage.User
        assertEquals("pause", userMsg.text)
        val assistantMsg = state.messages[2] as AssistantMessage.Assistant
        assertEquals("Paused playback.", assistantMsg.text)
        assertEquals("pauseCurrentPlayback", assistantMsg.toolName)
    }

    @Test
    fun `submitQuery with download sets pending confirmation`() = runTest {
        coEvery { coordinator.processQuery("download Dune") } returns AssistantResponse.PendingConfirmation(
            toolName = "searchAndDownloadAudiobook",
            arguments = mapOf("query" to "Dune"),
            promptTitle = "Download Audiobook",
            promptDetails = "Do you want to download Dune?",
            targetId = "book-42"
        )

        viewModel.submitQuery("download Dune")

        val state = viewModel.uiState.value
        assertNotNull(state.pendingConfirmation)
        assertEquals("searchAndDownloadAudiobook", state.pendingConfirmation?.toolName)
        assertEquals("Download Audiobook", state.pendingConfirmation?.title)
    }

    @Test
    fun `confirmPendingAction executes tool and clears pending`() = runTest {
        coEvery { coordinator.processQuery(any()) } returns AssistantResponse.PendingConfirmation(
            toolName = "downloadAudiobook",
            arguments = mapOf("bookId" to "book-42"),
            promptTitle = "Download Audiobook",
            promptDetails = "Confirm?",
            targetId = "book-42"
        )
        coEvery { coordinator.executeConfirmedTool("downloadAudiobook", any()) } returns McpToolResult.Success(
            "Download started."
        )

        viewModel.submitQuery("download")
        assertNotNull(viewModel.uiState.value.pendingConfirmation)

        viewModel.confirmPendingAction()

        val state = viewModel.uiState.value
        assertNull(state.pendingConfirmation)
        val lastMsg = state.messages.last() as AssistantMessage.Assistant
        assertEquals("Download started.", lastMsg.text)
        coVerify { coordinator.executeConfirmedTool("downloadAudiobook", any()) }
    }

    @Test
    fun `dismissPendingAction cancels and clears pending`() = runTest {
        coEvery { coordinator.processQuery(any()) } returns AssistantResponse.PendingConfirmation(
            toolName = "downloadAudiobook",
            arguments = mapOf("bookId" to "book-42"),
            promptTitle = "Download Audiobook",
            promptDetails = "Confirm?",
            targetId = "book-42"
        )

        viewModel.submitQuery("download")
        assertNotNull(viewModel.uiState.value.pendingConfirmation)

        viewModel.dismissPendingAction()

        val state = viewModel.uiState.value
        assertNull(state.pendingConfirmation)
        val lastMsg = state.messages.last() as AssistantMessage.Assistant
        assertEquals("Action cancelled.", lastMsg.text)
    }
}
