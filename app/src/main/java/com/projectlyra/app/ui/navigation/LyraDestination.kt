package com.projectlyra.app.ui.navigation

enum class LyraDestination(
    val route: String,
    val label: String,
) {
    HOME("home", "Home"),
    SEARCH("search", "Search"),
    MY_LIST("my_list", "My List"),
    SETTINGS("settings", "Settings"),
}
