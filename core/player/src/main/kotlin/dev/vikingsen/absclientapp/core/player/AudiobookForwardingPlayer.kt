package dev.vikingsen.absclientapp.core.player

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository

@OptIn(UnstableApi::class)
class AudiobookForwardingPlayer(
    exoPlayerProvider: ExoPlayerProvider,
    private val playerManager: PlayerManager,
    private val settingsRepository: SettingsRepository
) : ForwardingPlayer(exoPlayerProvider.exoPlayer) {

    private fun seekForwardByDuration() {
        val duration = settingsRepository.getSkipForwardDuration()
        val current = playerManager.currentPosition.value
        val total = playerManager.duration.value
        val target = current + duration
        if (target >= total) {
            playerManager.seekTo(total)
            playerManager.pause()
        } else {
            playerManager.seekTo(target)
        }
    }

    private fun seekBackwardByDuration() {
        val duration = settingsRepository.getSkipBackwardDuration()
        val current = playerManager.currentPosition.value
        val target = (current - duration).coerceAtLeast(0.0)
        playerManager.seekTo(target)
    }

    override fun seekToPrevious() {
        seekBackwardByDuration()
    }

    override fun seekToNext() {
        seekForwardByDuration()
    }

    override fun seekToPreviousMediaItem() {
        seekBackwardByDuration()
    }

    override fun seekToNextMediaItem() {
        seekForwardByDuration()
    }

    override fun getAvailableCommands(): Player.Commands {
        val commands = super.getAvailableCommands().buildUpon()
        // Always remove standard seek previous/next to prevent default UI buttons
        commands.remove(Player.COMMAND_SEEK_TO_PREVIOUS)
        commands.remove(Player.COMMAND_SEEK_TO_NEXT)
        commands.remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        commands.remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        commands.remove(Player.COMMAND_SEEK_BACK)
        commands.remove(Player.COMMAND_SEEK_FORWARD)
        return commands.build()
    }
}
