package dev.vikingsen.absclientapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.vikingsen.absclientapp.core.preferences.PreferencesManager
import dev.vikingsen.absclientapp.core.player.PlayerManager
import dev.vikingsen.absclientapp.feature.login.LoginScreen
import dev.vikingsen.absclientapp.feature.login.api.Login
import dev.vikingsen.absclientapp.feature.library.LibraryScreen
import dev.vikingsen.absclientapp.feature.library.DetailScreen
import dev.vikingsen.absclientapp.feature.library.api.Library
import dev.vikingsen.absclientapp.feature.library.api.Detail
import dev.vikingsen.absclientapp.feature.player.PlayerScreen
import dev.vikingsen.absclientapp.feature.player.api.Player
import dev.vikingsen.absclientapp.feature.miniplayer.MiniPlayerLayout
import org.koin.compose.koinInject

@Composable
fun MainNavigation() {
    val preferencesManager: PreferencesManager = koinInject()
    val playerManager: PlayerManager = koinInject()

    val startDestination = remember {
        if (preferencesManager.isLoggedIn()) Library else Login
    }

    val backStack = rememberNavBackStack(startDestination)
    val currentBook by playerManager.currentBook.collectAsState()
    val currentKey = backStack.lastOrNull()
    val showMiniPlayer = currentBook != null && currentKey != Login && currentKey != Player

    MiniPlayerLayout(
        playerManager = playerManager,
        showMiniPlayer = showMiniPlayer,
        onMiniPlayerClick = {
            backStack.add(Player)
        },
        modifier = Modifier.fillMaxSize()
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<Login> {
                    LoginScreen(
                        onLoginSuccess = {
                            // Navigate to Library
                            backStack.add(Library)
                        }
                    )
                }
                entry<Library> {
                    LibraryScreen(
                        onBookClick = { bookId ->
                            backStack.add(Detail(bookId))
                        },
                        onLogout = {
                            // Go back to Login
                            backStack.add(Login)
                        }
                    )
                }
                entry<Detail> { key ->
                    DetailScreen(
                        bookId = key.bookId,
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onPlayClick = { book, startPos ->
                            playerManager.playBook(book, startPos)
                            backStack.add(Player)
                        }
                    )
                }
                entry<Player> {
                    PlayerScreen(
                        onBackClick = {
                            backStack.removeLastOrNull()
                        }
                    )
                }
            }
        )
    }
}
