package com.projectlyra.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.projectlyra.app.LyraApplication
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.data.settings.AppSettings
import com.projectlyra.app.data.settings.SettingsStore
import com.projectlyra.app.feature.home.HomeViewModelFactory
import com.projectlyra.app.feature.mylist.MyListViewModelFactory
import com.projectlyra.app.feature.search.SearchViewModelFactory
import com.projectlyra.app.feature.settings.SettingsViewModelFactory
import com.projectlyra.app.ui.navigation.LyraDestination
import com.projectlyra.app.ui.theme.ProjectLyraTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal data class LyraAppDependencies(
    val homeFactory: HomeViewModelFactory,
    val myListFactory: MyListViewModelFactory,
    val settingsFactory: SettingsViewModelFactory,
    val searchFactory: SearchViewModelFactory,
    val libraryRepository: LibraryRepository,
    val settingsStore: SettingsStore,
    val dynamicAccentEnabledFlow: Flow<Boolean>,
)

@Composable
fun LyraApp() {
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as LyraApplication
    val dependencies = rememberLyraAppDependencies(application)

    val dynamicAccentEnabled by dependencies.dynamicAccentEnabledFlow.collectAsState(
        initial = AppSettings.dynamicAccentSupported,
    )

    val destinations = listOf(
        LyraDestination.HOME,
        LyraDestination.SEARCH,
        LyraDestination.MY_LIST,
        LyraDestination.SETTINGS,
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = destinations.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }

    ProjectLyraTheme(dynamicAccentEnabled = dynamicAccentEnabled) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    LyraBottomBar(
                        destinations = destinations,
                        currentDestination = currentDestination,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            LyraNavGraph(
                navController = navController,
                innerPadding = innerPadding,
                dependencies = dependencies,
            )
        }
    }
}

@Composable
private fun LyraBottomBar(
    destinations: List<LyraDestination>,
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                onClick = { onNavigate(destination.route) },
                icon = {
                    when (destination) {
                        LyraDestination.HOME -> Icon(Icons.Rounded.Home, contentDescription = null)
                        LyraDestination.SEARCH -> Icon(Icons.Rounded.Search, contentDescription = null)
                        LyraDestination.MY_LIST -> Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null)
                        LyraDestination.SETTINGS -> Icon(Icons.Rounded.Settings, contentDescription = null)
                    }
                },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
private fun rememberLyraAppDependencies(application: LyraApplication): LyraAppDependencies {
    return remember(application) {
        val container = application.container
        LyraAppDependencies(
            homeFactory = HomeViewModelFactory(
                libraryRepository = container.libraryRepository,
                settingsStore = container.settingsStore,
            ),
            myListFactory = MyListViewModelFactory(
                libraryRepository = container.libraryRepository,
            ),
            settingsFactory = SettingsViewModelFactory(
                settingsStore = container.settingsStore,
                backupService = container.backupService,
            ),
            searchFactory = SearchViewModelFactory(
                libraryRepository = container.libraryRepository,
                settingsStore = container.settingsStore,
            ),
            libraryRepository = container.libraryRepository,
            settingsStore = container.settingsStore,
            dynamicAccentEnabledFlow = container.settingsStore.settings.map { it.dynamicAccentEnabled },
        )
    }
}
