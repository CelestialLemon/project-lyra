package com.projectlyra.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.List
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.projectlyra.app.LyraApplication
import com.projectlyra.app.feature.home.HomeRoute
import com.projectlyra.app.feature.home.HomeViewModelFactory
import com.projectlyra.app.feature.mylist.MyListRoute
import com.projectlyra.app.feature.mylist.MyListViewModelFactory
import com.projectlyra.app.feature.search.SearchRoute
import com.projectlyra.app.feature.settings.SettingsRoute
import com.projectlyra.app.feature.settings.SettingsViewModelFactory
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

    val destinations = listOf(
        LyraDestination.HOME,
        LyraDestination.SEARCH,
        LyraDestination.MY_LIST,
        LyraDestination.SETTINGS,
    )

    ProjectLyraTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

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
                                    LyraDestination.MY_LIST -> Icon(Icons.Rounded.List, contentDescription = null)
                                    LyraDestination.SETTINGS -> Icon(Icons.Rounded.Settings, contentDescription = null)
                                }
                            },
                            label = { Text(destination.label) },
                        )
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
                        HomeRoute(factory = homeFactory)
                    }
                }
                composable(LyraDestination.SEARCH.route) {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        SearchRoute()
                    }
                }
                composable(LyraDestination.MY_LIST.route) {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        MyListRoute(factory = myListFactory)
                    }
                }
                composable(LyraDestination.SETTINGS.route) {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        SettingsRoute(factory = settingsFactory)
                    }
                }
            }
        }
    }
}
