package dev.vikingsen.absclientapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.vikingsen.absclientapp.data.PreferencesManager
import dev.vikingsen.absclientapp.player.PlayerManager
import dev.vikingsen.absclientapp.ui.login.LoginScreen
import dev.vikingsen.absclientapp.ui.library.LibraryScreen
import dev.vikingsen.absclientapp.ui.detail.DetailScreen
import dev.vikingsen.absclientapp.ui.player.PlayerScreen
import org.koin.compose.koinInject

@Composable
fun MainNavigation() {
    val preferencesManager: PreferencesManager = koinInject()
    val playerManager: PlayerManager = koinInject()

    val startDestination = remember {
        if (preferencesManager.isLoggedIn()) Library else Login
    }

    val backStack = rememberNavBackStack(startDestination)

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
