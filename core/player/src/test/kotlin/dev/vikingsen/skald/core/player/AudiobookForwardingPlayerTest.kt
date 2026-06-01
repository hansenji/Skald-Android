package dev.vikingsen.skald.core.player

import androidx.media3.common.Player
import dev.vikingsen.skald.domain.repository.SettingsRepository
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AudiobookForwardingPlayerTest {

    private val mockExoPlayer = mockk<androidx.media3.exoplayer.ExoPlayer>(relaxed = true)
    private val mockProvider = mockk<ExoPlayerProvider> {
        every { exoPlayer } returns mockExoPlayer
    }
    private val playerManager = mockk<PlayerManager>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    private lateinit var forwardingPlayer: AudiobookForwardingPlayer

    @Before
    fun setUp() {
        forwardingPlayer = AudiobookForwardingPlayer(
            exoPlayerProvider = mockProvider,
            playerManager = playerManager,
            settingsRepository = settingsRepository
        )
    }

    @Test
    fun testSeekForward() {
        every { settingsRepository.getSkipForwardDuration() } returns 30
        every { playerManager.currentPosition } returns MutableStateFlow(100.0)
        every { playerManager.duration } returns MutableStateFlow(200.0)

        forwardingPlayer.seekToNext()
        verify { playerManager.seekTo(130.0) }
    }

    @Test
    fun testSeekBackward() {
        every { settingsRepository.getSkipBackwardDuration() } returns 15
        every { playerManager.currentPosition } returns MutableStateFlow(50.0)

        forwardingPlayer.seekToPrevious()
        verify { playerManager.seekTo(35.0) }
    }

    @Test
    fun testAvailableCommands_excludesStandardSeekCommands() {
        val mockCommands = mockk<Player.Commands>()
        val mockBuilder = mockk<Player.Commands.Builder>(relaxed = true)
        val mockResultCommands = mockk<Player.Commands>()

        every { mockExoPlayer.getAvailableCommands() } returns mockCommands
        every { mockExoPlayer.availableCommands } returns mockCommands
        every { mockCommands.buildUpon() } returns mockBuilder
        every { mockBuilder.remove(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockResultCommands

        val available = forwardingPlayer.availableCommands
        assertSame(mockResultCommands, available)

        verify { mockBuilder.remove(Player.COMMAND_SEEK_TO_PREVIOUS) }
        verify { mockBuilder.remove(Player.COMMAND_SEEK_TO_NEXT) }
        verify { mockBuilder.remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) }
        verify { mockBuilder.remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) }
        verify { mockBuilder.remove(Player.COMMAND_SEEK_BACK) }
        verify { mockBuilder.remove(Player.COMMAND_SEEK_FORWARD) }
    }
}
