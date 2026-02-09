package com.projectlyra.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.projectlyra.app.LyraApplication
import com.projectlyra.app.feature.details.DetailsRoute
import com.projectlyra.app.feature.details.DetailsViewModelFactory
import com.projectlyra.app.feature.home.HomeRoute
import com.projectlyra.app.feature.home.HomeViewModelFactory
import com.projectlyra.app.feature.mylist.MyListRoute
import com.projectlyra.app.feature.mylist.MyListViewModelFactory
import com.projectlyra.app.feature.search.SearchRoute
import com.projectlyra.app.feature.search.SearchViewModelFactory
import com.projectlyra.app.feature.settings.SettingsRoute
import com.projectlyra.app.feature.settings.SettingsViewModelFactory
import com.projectlyra.app.ui.navigation.DetailsDestination
import com.projectlyra.app.ui.navigation.LyraDestination
import com.projectlyra.app.ui.theme.ProjectLyraTheme

@Composable
fun LyraApp() {
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as LyraApplication

    val homeFactory = remember(application) {
        HomeViewModelFactory(
            libraryRepository = application.container.libraryRepository,
            settingsStore = application.container.settingsStore,
        )
    }
    val myListFactory = remember(application) {
        MyListViewModelFactory(
            libraryRepository = application.container.libraryRepository,
        )
    }
    val settingsFactory = remember(application) {
        SettingsViewModelFactory(
            settingsStore = application.container.settingsStore,
        )
    }
    val searchFactory = remember(application) {
        SearchViewModelFactory(
            libraryRepository = application.container.libraryRepository,
            settingsStore = application.container.settingsStore,
        )
    }

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

    ProjectLyraTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        destinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
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
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = LyraDestination.HOME.route,
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                composable(LyraDestination.HOME.route) {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        HomeRoute(
                            factory = homeFactory,
                            onOpenDetails = { item ->
                                navController.navigate(
                                    DetailsDestination.route(
                                        mediaType = item.mediaType,
                                        tmdbId = item.tmdbId,
                                    )
                                )
                            },
                        )
                    }
                }
                composable(LyraDestination.SEARCH.route) {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        SearchRoute(
                            factory = searchFactory,
                            onOpenDetails = { item ->
                                navController.navigate(
                                    DetailsDestination.route(
                                        mediaType = item.mediaType,
                                        tmdbId = item.tmdbId,
                                    )
                                )
                            },
                        )
                    }
                }
                composable(LyraDestination.MY_LIST.route) {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        MyListRoute(
                            factory = myListFactory,
                            onOpenDetails = { item ->
                                navController.navigate(
                                    DetailsDestination.route(
                                        mediaType = item.mediaType,
                                        tmdbId = item.tmdbId,
                                    )
                                )
                            },
                        )
                    }
                }
                composable(LyraDestination.SETTINGS.route) {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        SettingsRoute(factory = settingsFactory)
                    }
                }
                composable(
                    route = DetailsDestination.ROUTE_PATTERN,
                    arguments = listOf(
                        navArgument(DetailsDestination.ARG_MEDIA_TYPE) { type = NavType.StringType },
                        navArgument(DetailsDestination.ARG_TMDB_ID) { type = NavType.IntType },
                    ),
                ) { backStackEntry ->
                    val tmdbId = backStackEntry.arguments?.getInt(DetailsDestination.ARG_TMDB_ID) ?: -1
                    val mediaType = DetailsDestination.parseMediaType(
                        backStackEntry.arguments?.getString(DetailsDestination.ARG_MEDIA_TYPE)
                    )

                    if (tmdbId <= 0 || mediaType == null) {
                        Box(modifier = Modifier.padding(innerPadding)) {
                            Text(
                                text = "Unable to open this title.",
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    } else {
                        val detailsFactory = remember(application, tmdbId, mediaType) {
                            DetailsViewModelFactory(
                                tmdbId = tmdbId,
                                mediaType = mediaType,
                                libraryRepository = application.container.libraryRepository,
                                settingsStore = application.container.settingsStore,
                            )
                        }
                        Box(modifier = Modifier.padding(innerPadding)) {
                            DetailsRoute(
                                factory = detailsFactory,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
