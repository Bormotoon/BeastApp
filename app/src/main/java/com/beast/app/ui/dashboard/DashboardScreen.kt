@file:OptIn(ExperimentalMaterial3Api::class)

package com.beast.app.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beast.app.R

@Composable
fun DashboardRoute(
    onNavigateHome: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenProgram: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state.value,
        onNavigateHome = onNavigateHome,
        onOpenCalendar = onOpenCalendar,
        onOpenProgram = onOpenProgram,
        onOpenProgress = onOpenProgress,
        onOpenProfile = onOpenProfile,
        onOpenSettings = onOpenSettings,
        onStartWorkout = onStartWorkout,
        onViewWorkoutDetails = onViewWorkoutDetails
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onNavigateHome: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenProgram: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit
) {
    val selectedTab = rememberSaveable { mutableStateOf(DashboardNavDestination.HOME) }
    Scaffold(
        topBar = {
            DashboardTopBar(
                topBar = state.topBar,
                onOpenProfile = onOpenProfile,
                onOpenSettings = onOpenSettings
            )
        },
        bottomBar = {
            DashboardBottomBar(
                selected = selectedTab.value,
                onSelect = { destination ->
                    selectedTab.value = destination
                    when (destination) {
                        DashboardNavDestination.HOME -> onNavigateHome()
                        DashboardNavDestination.CALENDAR -> onOpenCalendar()
                        DashboardNavDestination.PROGRAMS -> onOpenProgram()
                        DashboardNavDestination.PROGRESS -> onOpenProgress()
                        DashboardNavDestination.PROFILE -> onOpenProfile()
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.isLoading) {
                item { CircularProgressIndicator() }
            } else {
                if (state.progressCard.isVisible) {
                    item { ProgressCard(state = state.progressCard) }
                }
                if (state.workoutList.phases.isNotEmpty() || state.workoutList.completedWorkouts.isNotEmpty()) {
                    item {
                        WorkoutList(
                            state = state.workoutList,
                            onStartWorkout = onStartWorkout,
                            onViewWorkoutDetails = onViewWorkoutDetails
                        )
                    }
                }
                if (!state.progressCard.isVisible && state.workoutList.phases.isEmpty() && state.workoutList.completedWorkouts.isEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(id = com.beast.app.R.string.no_program_loaded),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { onOpenProgram() }) {
                                Text(text = stringResource(id = com.beast.app.R.string.import_program_button))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(state: ProgressCardState) {
    val expanded = rememberSaveable { mutableStateOf(false) }
    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { expanded.value = !expanded.value }) {
        Column(modifier = Modifier.padding(20.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (expanded.value) {
                Text(
                    text = state.programName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "День ${state.dayNumber} из ${state.totalDays}",
                    style = MaterialTheme.typography.headlineSmall
                )
                LinearProgressIndicator(
                    progress = { state.progressFraction },
                    modifier = Modifier.fillMaxWidth()
                )
                val phase = state.currentPhaseName
                val week = state.currentPhaseWeek
                if (!phase.isNullOrBlank() || week != null) {
                    Text(
                        text = buildString {
                            if (!phase.isNullOrBlank()) {
                                append("Фаза: ")
                                append(phase)
                            }
                            if (week != null) {
                                if (!phase.isNullOrBlank()) append(" · ")
                                append("Неделя ")
                                append(week)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LinearProgressIndicator(
                    progress = { state.progressFraction },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TodayWorkoutCard(
    state: TodayWorkoutCardState,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleLarge
            )
            if (state.subtitle.isNotBlank()) {
                Text(
                    text = state.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val statusColor = when (state.statusType) {
                WorkoutStatus.SCHEDULED -> MaterialTheme.colorScheme.primary
                WorkoutStatus.REST -> MaterialTheme.colorScheme.tertiary
                WorkoutStatus.UPCOMING -> MaterialTheme.colorScheme.secondary
                WorkoutStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                WorkoutStatus.FINISHED -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            if (state.statusText.isNotBlank()) {
                Text(
                    text = state.statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor
                )
            }

            if (state.description.isNotBlank()) {
                Text(
                    text = state.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (state.durationMinutes > 0 || state.exerciseCount > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.durationMinutes > 0) {
                        Text(
                            text = "~${state.durationMinutes} мин",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.exerciseCount > 0) {
                        Text(
                            text = "${state.exerciseCount} упражнений",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.muscleGroups.isNotEmpty()) {
                Text(
                    text = "Целевые мышцы: ${state.muscleGroups.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val workoutId = state.workoutId
            if (workoutId != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { onStartWorkout(workoutId) },
                    enabled = state.startButtonEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Начать тренировку")
                }
                OutlinedButton(
                    onClick = { onViewWorkoutDetails(workoutId) },
                    enabled = state.viewDetailsEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Просмотр деталей")
                }
            }
        }
    }
}

@Composable
private fun WorkoutList(
    state: WorkoutListState,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        state.phases.forEach { phase ->
            PhaseBlock(phase = phase, onStartWorkout = onStartWorkout, onViewWorkoutDetails = onViewWorkoutDetails)
        }
        if (state.completedWorkouts.isNotEmpty()) {
            CompletedWorkoutsBlock(
                workouts = state.completedWorkouts,
                onStartWorkout = onStartWorkout,
                onViewWorkoutDetails = onViewWorkoutDetails
            )
        }
        if (state.remainingWorkouts.isNotEmpty()) {
            RemainingWorkoutsBlock(
                workouts = state.remainingWorkouts,
                onStartWorkout = onStartWorkout,
                onViewWorkoutDetails = onViewWorkoutDetails
            )
        }
    }
}

@Composable
private fun PhaseBlock(
    phase: PhaseState,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = phase.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        phase.workouts.forEach { workout ->
            WorkoutCard(
                workout = workout,
                onStartWorkout = onStartWorkout,
                onViewWorkoutDetails = onViewWorkoutDetails
            )
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: WorkoutItemState,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { onStartWorkout(workout.id) }) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = workout.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (workout.isToday) {
                Text(
                    text = "Сегодня",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { onViewWorkoutDetails(workout.id) }) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Детали"
                )
            }
        }
    }
}

@Composable
private fun CompletedWorkoutsBlock(
    workouts: List<WorkoutItemState>,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit
) {
    val expanded = rememberSaveable { mutableStateOf(false) }
    Column {
        Text(
            text = "Завершённые тренировки (${workouts.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.clickable { expanded.value = !expanded.value },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (expanded.value) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                workouts.forEach { workout ->
                    WorkoutCard(
                        workout = workout,
                        onStartWorkout = onStartWorkout,
                        onViewWorkoutDetails = onViewWorkoutDetails
                    )
                }
            }
        }
    }
}

@Composable
private fun RemainingWorkoutsBlock(
    workouts: List<WorkoutItemState>,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        workouts.forEach { workout ->
            RemainingWorkoutCard(
                workout = workout,
                onStartWorkout = onStartWorkout,
                onViewWorkoutDetails = onViewWorkoutDetails
            )
        }
    }
}

@Composable
private fun RemainingWorkoutCard(
    workout: WorkoutItemState,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { onStartWorkout(workout.id) }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "День ${workout.dayNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = workout.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private enum class DashboardNavDestination { HOME, CALENDAR, PROGRAMS, PROGRESS, PROFILE }

private data class DashboardNavItem(
    val destination: DashboardNavDestination,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun DashboardBottomBar(
    selected: DashboardNavDestination,
    onSelect: (DashboardNavDestination) -> Unit
) {
    val items = listOf(
        DashboardNavItem(DashboardNavDestination.HOME, "Главная", Icons.Outlined.Home),
        DashboardNavItem(DashboardNavDestination.CALENDAR, "Календарь", Icons.Outlined.CalendarMonth),
        DashboardNavItem(DashboardNavDestination.PROGRAMS, "Программа", Icons.AutoMirrored.Outlined.List),
        DashboardNavItem(DashboardNavDestination.PROGRESS, "Прогресс", Icons.Outlined.BarChart),
        DashboardNavItem(DashboardNavDestination.PROFILE, "Профиль", Icons.Outlined.AccountCircle)
    )
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = selected == item.destination,
                onClick = { onSelect(item.destination) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}

@Composable
private fun DashboardTopBar(
    topBar: TopBarState,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = topBar.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (topBar.subtitle.isNotBlank()) {
                    Text(
                        text = topBar.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null
            )
        },
        actions = {
            IconButton(onClick = onOpenProfile) {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = stringResource(R.string.cd_open_profile)
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.cd_open_settings)
                )
            }
        }
    )
}
