package com.example.xdownloader.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    val destinations = listOf(
        Triple("home", "動態", Icons.Default.Home),
        Triple("search", "探索", Icons.Default.Search),
        Triple("favorites", "收藏", Icons.Default.Bookmark),
        Triple("downloads", "下載", Icons.Default.Download),
        Triple("settings", "設定", Icons.Default.Settings)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (route != "login") {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp
                    ) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            destinations.forEach { (destination, title, icon) ->
                                NavigationBarItem(
                                    selected = route == destination,
                                    onClick = {
                                        navController.navigate(destination) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(icon, contentDescription = title) },
                                    label = { Text(title) },
                                    alwaysShowLabel = true
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") { HomeScreen() }
            composable("search") { SearchAuthorScreen() }
            composable("favorites") { FavoritesScreen() }
            composable("downloads") { DownloadsScreen() }
            composable("settings") { SettingsScreen(navController) }
            composable("login") {
                com.example.xdownloader.ui.screens.login.LoginScreen(navController)
            }
        }
    }
}
