package dev.vikingsen.skald

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import dev.vikingsen.skald.feature.assistant.ui.AssistantBottomSheet
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.vikingsen.skald.feature.login.LoginScreen
import dev.vikingsen.skald.feature.login.api.Login
import dev.vikingsen.skald.feature.library.LibraryScreen
import dev.vikingsen.skald.feature.library.DetailScreen
import dev.vikingsen.skald.feature.library.SeriesDetailScreen
import dev.vikingsen.skald.feature.library.AuthorDetailScreen
import dev.vikingsen.skald.feature.library.CollectionDetailScreen
import dev.vikingsen.skald.feature.library.PlaylistDetailScreen
import dev.vikingsen.skald.feature.library.api.Library
import dev.vikingsen.skald.feature.library.api.Detail
import dev.vikingsen.skald.feature.library.api.SeriesDetail
import dev.vikingsen.skald.feature.library.api.AuthorDetail
import dev.vikingsen.skald.feature.library.api.CollectionDetail
import dev.vikingsen.skald.feature.library.api.PlaylistDetail
import dev.vikingsen.skald.feature.home.HomeScreen
import dev.vikingsen.skald.feature.home.api.Home
import dev.vikingsen.skald.feature.settings.SettingsScreen
import dev.vikingsen.skald.feature.settings.api.Settings
import dev.vikingsen.skald.feature.player.PlayerScreen
import dev.vikingsen.skald.feature.player.api.Player
import dev.vikingsen.skald.feature.miniplayer.MiniPlayerLayout
import dev.vikingsen.skald.feature.miniplayer.MiniPlayerViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.platform.LocalContext
import android.app.Activity

private enum class TopLevelDestination(
    val navKey: NavKey,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelResId: Int
) {
    HOME(
        navKey = Home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        labelResId = R.string.nav_home
    ),
    LIBRARY(
        navKey = Library,
        selectedIcon = Icons.Filled.LocalLibrary,
        unselectedIcon = Icons.Outlined.LocalLibrary,
        labelResId = R.string.nav_library
    ),
    SETTINGS(
        navKey = Settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        labelResId = R.string.nav_settings
    )
}

@Composable
fun MainNavigation() {
    val mainViewModel: MainViewModel = koinViewModel()
    val miniPlayerViewModel: MiniPlayerViewModel = koinViewModel()

    val startDestination = remember {
        mainViewModel.startDestination
    }

    val backStack = rememberNavBackStack(startDestination)

    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, activity) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                mainViewModel.syncGlobalProgress()
                activity?.intent?.let { intent ->
                    if (intent.action == "dev.vikingsen.skald.ACTION_PLAYER") {
                        intent.action = null
                        if (backStack.lastOrNull() != Player) {
                            backStack.add(Player)
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val miniPlayerState by miniPlayerViewModel.uiState.collectAsState()
    val currentKey = backStack.lastOrNull()
    val showMiniPlayer = miniPlayerState != null && currentKey != Login && currentKey != Player

    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn && backStack.lastOrNull() != Login) {
            while (backStack.lastOrNull() != null) {
                backStack.removeLastOrNull()
            }
            backStack.add(Login)
        }
    }

    val isOnLoginScreen = currentKey == Login
    var showAssistantOverlay by remember { mutableStateOf(false) }

    val content: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize()) {
            MiniPlayerLayout(
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
                                    backStack.add(Home)
                                }
                            )
                        }
                        entry<Home> {
                            HomeScreen(
                                onBookClick = { bookId ->
                                    backStack.add(Detail(bookId))
                                }
                            )
                        }
                        entry<Library> {
                            LibraryScreen(
                                onBookClick = { bookId ->
                                    backStack.add(Detail(bookId))
                                },
                                onSeriesClick = { seriesId ->
                                    backStack.add(SeriesDetail(seriesId))
                                },
                                onAuthorClick = { authorId ->
                                    backStack.add(AuthorDetail(authorId))
                                },
                                onCollectionClick = { collectionId ->
                                    backStack.add(CollectionDetail(collectionId))
                                },
                                onPlaylistClick = { playlistId ->
                                    backStack.add(PlaylistDetail(playlistId))
                                },
                                onPlayClick = {
                                    backStack.add(Player)
                                }
                            )
                        }
                        entry<Settings> {
                            SettingsScreen(
                                onLogout = {
                                    // Logout handled by SettingsViewModel;
                                    // the LaunchedEffect(isLoggedIn) will navigate to Login
                                }
                            )
                        }
                        entry<Detail> { key ->
                            DetailScreen(
                                bookId = key.bookId,
                                onBackClick = {
                                    backStack.removeLastOrNull()
                                },
                                onPlayClick = {
                                    backStack.add(Player)
                                },
                                onSeriesClick = { seriesId ->
                                    backStack.add(SeriesDetail(seriesId))
                                },
                                onPlaylistClick = { playlistId ->
                                    backStack.add(PlaylistDetail(playlistId))
                                },
                                onCollectionClick = { collectionId ->
                                    backStack.add(CollectionDetail(collectionId))
                                }
                            )
                        }
                        entry<SeriesDetail> { key ->
                            SeriesDetailScreen(
                                seriesId = key.seriesId,
                                onBackClick = {
                                    backStack.removeLastOrNull()
                                },
                                onBookClick = { bookId ->
                                    backStack.add(Detail(bookId))
                                }
                            )
                        }
                        entry<AuthorDetail> { key ->
                            AuthorDetailScreen(
                                authorId = key.authorId,
                                onBackClick = {
                                    backStack.removeLastOrNull()
                                },
                                onBookClick = { bookId ->
                                    backStack.add(Detail(bookId))
                                }
                            )
                        }
                        entry<CollectionDetail> { key ->
                            CollectionDetailScreen(
                                collectionId = key.collectionId,
                                onBackClick = {
                                    backStack.removeLastOrNull()
                                },
                                onBookClick = { bookId ->
                                    backStack.add(Detail(bookId))
                                }
                            )
                        }
                        entry<PlaylistDetail> { key ->
                            PlaylistDetailScreen(
                                playlistId = key.playlistId,
                                onBackClick = {
                                    backStack.removeLastOrNull()
                                },
                                onBookClick = { bookId ->
                                    backStack.add(Detail(bookId))
                                },
                                onPlayClick = {
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

            val activeKey = backStack.lastOrNull()
            if (activeKey != null && activeKey != Player && activeKey != Login) {
                FloatingActionButton(
                    onClick = { showAssistantOverlay = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = if (showMiniPlayer) 164.dp else 96.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = stringResource(R.string.nav_assistant)
                    )
                }
            }
        }

        if (showAssistantOverlay) {
            AssistantBottomSheet(
                onDismiss = { showAssistantOverlay = false }
            )
        }
    }

    if (isOnLoginScreen) {
        // No navigation shell on the login screen
        content()
    } else {
        // Determine active top-level destination from back stack (checking from most recent entries first)
        val currentTopLevel = backStack.lastOrNull { key ->
            TopLevelDestination.entries.any { it.navKey == key }
        }?.let { key ->
            TopLevelDestination.entries.first { it.navKey == key }
        } ?: TopLevelDestination.HOME

        NavigationSuiteScaffold(
            navigationSuiteItems = {
                TopLevelDestination.entries.forEach { destination ->
                    val isSelected = destination == currentTopLevel
                    item(
                        selected = isSelected,
                        onClick = {
                            if (backStack.contains(destination.navKey)) {
                                // Pop back to this destination
                                while (backStack.lastOrNull() != destination.navKey) {
                                    backStack.removeLastOrNull() ?: break
                                }
                            } else {
                                // Remove any non-top-level entries from the back stack,
                                // then navigate to the selected destination
                                while (backStack.lastOrNull() != null &&
                                    TopLevelDestination.entries.none { it.navKey == backStack.lastOrNull() }
                                ) {
                                    backStack.removeLastOrNull()
                                }
                                backStack.add(destination.navKey)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = stringResource(destination.labelResId)
                            )
                        },
                        label = {
                            Text(text = stringResource(destination.labelResId))
                        }
                    )
                }
            }
        ) {
            content()
        }
    }
}
