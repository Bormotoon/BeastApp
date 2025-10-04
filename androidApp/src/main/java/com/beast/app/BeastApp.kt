package com.beast.app

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.beast.app.dashboard.DashboardViewModel
import com.beast.app.programs.ProgramsViewModel
import com.beast.app.onboarding.OnboardingScreen
import com.beast.app.onboarding.OnboardingViewModel
import com.beast.app.workout.WorkoutDetailScreen
import com.beast.shared.model.WorkoutLog
import com.beast.app.programs.ProgramDetailScreen
import com.beast.app.progress.ProgressScreen as ProgressScreenNew
import com.beast.app.calendar.CalendarScreen as CalendarScreenNew

sealed class Route(val route: String, val label: String, val icon: ImageVector? = null) {
    data object Onboarding : Route("onboarding", "Onboarding", null)
    data object Dashboard : Route("dashboard", "Home", Icons.Filled.Home)
    data object Programs : Route("programs", "Programs", Icons.Filled.List)
    data object Calendar : Route("calendar", "Calendar", Icons.Filled.CalendarMonth)
    data object Progress : Route("progress", "Progress", Icons.Filled.ShowChart)
    data object Profile : Route("profile", "Profile", Icons.Filled.Person)
    data object ProgramDetail : Route("program/{programId}", "Program", null)
    data object WorkoutDetail : Route("workout/{programId}/{dayIndex}", "Workout", null)
    data object ExerciseDetail : Route("exercise/{exerciseId}", "Exercise", null)
    data object AdvancedSettings : Route("advanced_settings", "Advanced Settings", null)
    data object ImportProgram : Route("import_program", "Import Program", null)
}

@Composable
fun BeastApp() {
    val navController = rememberNavController()
    val items = listOf(Route.Dashboard, Route.Programs, Route.Calendar, Route.Progress, Route.Profile)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != Route.Onboarding.route) {
                NavigationBar {
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
                            icon = { item.icon?.let { Icon(it, contentDescription = item.label) } }
                        )
                    }
                }
            }
        }
    ) { padding ->

        val onboardingCompleted by appState.onboardingCompleted.collectAsState(initial = false)
        val startDestination = if (onboardingCompleted) Route.Dashboard.route else Route.Onboarding.route
                            popUpTo(Route.Onboarding.route) { inclusive = true }
                            launchSingleTop = true
                        }
            startDestination = startDestination,
                )
            }
            composable(Route.Dashboard.route) {
                        navController.navigate("workout/$programId/$dayIndex")
                    onOnboardingComplete = {
                        navController.navigate(Route.Dashboard.route) {
                            popUpTo(Route.Onboarding.route) { inclusive = true }
                            launchSingleTop = true
                        }
            composable(Route.Programs.route) {
                ProgramsScreen(
                    onOpen = { id -> navController.navigate("program/$id") },
                    onNavigateToImport = { navController.navigate(Route.ImportProgram.route) }
                )
            }
            composable(Route.Calendar.route) { CalendarScreenNew(onStartWorkout = { programId, dayIndex ->
                navController.navigate("workout/$programId/$dayIndex")
            }) }
            composable(Route.Progress.route) { ProgressScreenNew() }
            composable(Route.Profile.route) {
                ProfileScreen(
                    onNavigateToAdvanced = { navController.navigate(Route.AdvancedSettings.route) }
                )
            }
            composable(Route.WorkoutDetail.route) { backStackEntry ->
                val programId = backStackEntry.arguments?.getString("programId") ?: return@composable
                val dayIndex = backStackEntry.arguments?.getString("dayIndex")?.toIntOrNull() ?: return@composable
                WorkoutDetailScreen(programId = programId, dayIndex = dayIndex, onFinished = { navController.popBackStack() })
            }
            composable(Route.ProgramDetail.route) { backStackEntry ->
                val programId = backStackEntry.arguments?.getString("programId") ?: return@composable
                ProgramDetailScreen(programId = programId)
            }
            composable(Route.ExerciseDetail.route) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: return@composable
                com.beast.app.exercise.ExerciseDetailScreen(
                    exerciseId = exerciseId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Route.AdvancedSettings.route) {
                com.beast.app.settings.AdvancedSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Route.ImportProgram.route) {
                com.beast.app.programs.ImportProgramScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    vm: DashboardViewModel = hiltViewModel(),
    onStartWorkout: (String, Int) -> Unit,
    onOpenPrograms: () -> Unit,
) {
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Button(
                onClick = {
                    val p = state.program
                    val next = state.nextDayIndex
                    if (p != null && next != null) onStartWorkout(p.id, next) else onOpenPrograms()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Начать новую тренировку")
            }
        }
        item { Spacer(Modifier.height(12.dp)) }

        if (state.program == null) {
            item {
                EmptyState(
                    title = "Нет активной программы",
                    subtitle = "Откройте Programs и выберите активную программу"
                )
            }
        } else {
            item {
                Text(
                    text = state.program?.title ?: "",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text("Прогресс: ${state.progressPercent}% (${state.completedDays}/${state.totalDays})")
            }
            item { Spacer(Modifier.height(16.dp)) }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Следующая тренировка", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        val label = buildString {
                            append("Day ")
                            append(state.nextDayIndex ?: "-")
                            state.nextDayTitle?.let { append(": ").append(it) }
                        }
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val p = state.program
                                val next = state.nextDayIndex
                                if (p != null && next != null) onStartWorkout(p.id, next)
                            }) { Text("Start") }
                            OutlinedButton(onClick = { vm.markNextDayDone() }, enabled = state.nextDayIndex != null) {
                                Text("Отметить выполненным")
                            }
                        }
                    }
                }
            }
        }

        if (state.recentLogs.isNotEmpty()) {
            item { Spacer(Modifier.height(24.dp)) }
            item { Text("Последние тренировки", style = MaterialTheme.typography.titleMedium) }
            items(state.recentLogs) { log ->
                RecentLogItem(log, onClick = { onStartWorkout(log.programId, log.dayIndex) })
            }
        }
    }
}

@Composable
private fun RecentLogItem(log: com.beast.shared.model.WorkoutLog, onClick: () -> Unit = {}) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)
        .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("День ${log.dayIndex}")
            Text(if (log.completed) "Завершено" else "В процессе")
        }
    }
}

@Composable
fun ProgramsScreen(
    vm: ProgramsViewModel = hiltViewModel(),
    onOpen: (String) -> Unit = {},
    onNavigateToImport: () -> Unit = {}
) {
    LaunchedEffect(Unit) { vm.load() }
    val programs by vm.programs.collectAsState()
    val activeId by vm.activeProgramId.collectAsState(initial = null)

    Column(modifier = Modifier.fillMaxSize()) {
        // Import button at the top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onNavigateToImport) {
                Text("⬆️ Import Program")
            }
        }

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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.title, style = MaterialTheme.typography.titleMedium)
                                Text(p.description, style = MaterialTheme.typography.bodyMedium)
                            }
                            val isActive = p.id == activeId
                            if (isActive) {
                                Text("Выбрано", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { onOpen(p.id) }) { Text("Открыть") }
                                    Button(onClick = { vm.select(p.id) }) { Text("Выбрать") }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun ProfileScreen(app: AppStateViewModel = hiltViewModel(), onNavigateToAdvanced: () -> Unit = {}) {
    val accent by app.accentColor.collectAsState(initial = "#2E7D32")
    var custom by remember(accent) { mutableStateOf(TextFieldValue(accent)) }
    val presets = listOf(
        "#2E7D32" to "Green",
        "#1E88E5" to "Blue",
        "#D81B60" to "Pink",
        "#F57C00" to "Orange",
        "#43A047" to "Green 600",
    )
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Настройки", style = MaterialTheme.typography.titleLarge) }
        item { Spacer(Modifier.height(12.dp)) }

        // Advanced Settings button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Button(
                    onClick = onNavigateToAdvanced,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text("🔧 Advanced Settings")
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
        item { Text("Акцентный цвет", style = MaterialTheme.typography.titleMedium) }
        item { Spacer(Modifier.height(8.dp)) }
        items(presets) { (hex, name) ->
            ListItem(
                headlineContent = { Text(name) },
                supportingContent = { Text(hex) },
                trailingContent = {
                    val selected = accent.equals(hex, ignoreCase = true)
                    RadioButton(selected = selected, onClick = { app.setAccent(hex) })
                },
                modifier = Modifier.fillMaxWidth()
            )
            Divider()
        }
        item { Spacer(Modifier.height(12.dp)) }
        item { Text("Свой HEX", style = MaterialTheme.typography.titleMedium) }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    label = { Text("#RRGGBB или #AARRGGBB") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { app.setAccent(custom.text) }) { Text("Применить") }
            }
        }
    }
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
