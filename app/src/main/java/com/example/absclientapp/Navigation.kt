package com.example.absclientapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.absclientapp.data.PreferencesManager
import com.example.absclientapp.player.PlayerManager
import com.example.absclientapp.ui.login.LoginScreen
import com.example.absclientapp.ui.library.LibraryScreen
import com.example.absclientapp.ui.detail.DetailScreen
import com.example.absclientapp.ui.player.PlayerScreen
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
