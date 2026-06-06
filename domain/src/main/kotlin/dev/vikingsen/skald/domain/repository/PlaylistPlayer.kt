package dev.vikingsen.skald.domain.repository

import dev.vikingsen.skald.core.model.Playlist

interface PlaylistPlayer {
    fun playPlaylist(playlist: Playlist, startIndex: Int, startPosition: Double = 0.0)
}
