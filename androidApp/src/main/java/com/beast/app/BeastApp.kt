package com.beast.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.beast.app.programs.ProgramsViewModel

sealed class Route(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Route("dashboard", "Home", Icons.Filled.Home)
    data object Programs : Route("programs", "Programs", Icons.Filled.List)
    data object Calendar : Route("calendar", "Calendar", Icons.Filled.CalendarMonth)
    data object Progress : Route("progress", "Progress", Icons.Filled.ShowChart)
    data object Profile : Route("profile", "Profile", Icons.Filled.Person)
}

@Composable
fun BeastApp() {
    val navController = rememberNavController()
    val items = listOf(Route.Dashboard, Route.Programs, Route.Calendar, Route.Progress, Route.Profile)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(item.label) },
                        icon = { Icon(item.icon, contentDescription = item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Route.Dashboard.route) { DashboardScreen() }
            composable(Route.Programs.route) { ProgramsScreen() }
            composable(Route.Calendar.route) { CalendarScreen() }
            composable(Route.Progress.route) { ProgressScreen() }
            composable(Route.Profile.route) { ProfileScreen() }
        }
    }
}

@Composable
fun DashboardScreen() { CenteredText("Dashboard") }

@Composable
fun ProgramsScreen(vm: ProgramsViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.load() }
    val programs by vm.programs.collectAsState()
    if (programs.isEmpty()) {
        EmptyState(
            title = "Нет программ",
            subtitle = "Импортируйте 90-дневную программу или создайте свою"
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(programs) { p ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(p.title, style = MaterialTheme.typography.titleMedium)
                        Text(p.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarScreen() { CenteredText("Calendar") }

@Composable
fun ProgressScreen() { CenteredText("Progress") }

@Composable
fun ProfileScreen() { CenteredText("Profile & Settings") }

@Composable
private fun CenteredText(text: String) {
    Surface { Text(text) }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
