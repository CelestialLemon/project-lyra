package com.projectlyra.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.projectlyra.app.feature.details.DetailsRoute
import com.projectlyra.app.feature.details.DetailsViewModelFactory
import com.projectlyra.app.feature.home.HomeRoute
import com.projectlyra.app.feature.mylist.MyListRoute
import com.projectlyra.app.feature.search.SearchRoute
import com.projectlyra.app.feature.settings.SettingsRoute
import com.projectlyra.app.ui.navigation.DetailsDestination
import com.projectlyra.app.ui.navigation.LyraDestination

@Composable
internal fun LyraNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    dependencies: LyraAppDependencies,
) {
    NavHost(
        navController = navController,
        startDestination = LyraDestination.HOME.route,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(LyraDestination.HOME.route) {
            Box(modifier = Modifier.padding(innerPadding)) {
                HomeRoute(
                    factory = dependencies.homeFactory,
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
                    factory = dependencies.searchFactory,
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
                    factory = dependencies.myListFactory,
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
                SettingsRoute(factory = dependencies.settingsFactory)
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
                val detailsFactory = remember(tmdbId, mediaType, dependencies) {
                    DetailsViewModelFactory(
                        tmdbId = tmdbId,
                        mediaType = mediaType,
                        libraryRepository = dependencies.libraryRepository,
                        settingsStore = dependencies.settingsStore,
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
