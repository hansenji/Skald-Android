package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.Playlist
import dev.vikingsen.skald.domain.repository.PlaylistPlayer

class PlayPlaylistUseCase(private val playlistPlayer: PlaylistPlayer) {
    operator fun invoke(playlist: Playlist, startIndex: Int = 0, startPosition: Double = 0.0) {
        playlistPlayer.playPlaylist(playlist, startIndex, startPosition)
    }
}
