@file:OptIn(ExperimentalMaterial3Api::class)

package com.beast.app.ui.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProgramRoute(
    onBack: () -> Unit,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit,
    viewModel: ProgramViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgramScreen(
        state = state,
        onBack = onBack,
        onStartWorkout = onStartWorkout,
        onViewWorkoutDetails = onViewWorkoutDetails,
        onRefresh = viewModel::refresh
    )
}

@Composable
private fun ProgramScreen(
    state: ProgramUiState,
    onBack: () -> Unit,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var selectedTab by rememberSaveable(state.phases.size) { mutableIntStateOf(0) }
    LaunchedEffect(state.phases.size) {
        if (state.phases.isNotEmpty() && selectedTab !in state.phases.indices) {
            selectedTab = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.programName ?: "Программа",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.programName != null && state.phases.isNotEmpty()) {
                            Text(
                                text = "Фаз: ${state.phases.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
            state.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRefresh) { Text("Обновить") }
                }
            }
            state.phases.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Для программы пока нет фаз",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                val phases = state.phases
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 16.dp,
                        divider = { Divider(thickness = 0.dp) }
                    ) {
                        phases.forEachIndexed { index, phase ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(phase.name) }
                            )
                        }
                    }

                    val selectedPhase = phases.getOrNull(selectedTab)
                    if (selectedPhase != null) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item { PhaseInfoCard(phase = selectedPhase) }
                            items(selectedPhase.workouts) { workout ->
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
        }
    }
}

@Composable
private fun PhaseInfoCard(phase: PhaseUiModel) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = phase.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Длительность: ${phase.durationWeeks} нед.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Тренировок в фазе: ${phase.workoutCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: WorkoutUiModel,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = workout.name, style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (workout.durationMinutes > 0) {
                    Text(
                        text = "~${workout.durationMinutes} мин",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${workout.exerciseCount} упражнений",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (workout.muscleGroups.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = workout.muscleGroups.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val lastCompleted = workout.lastCompletedLabel
            Text(
                text = lastCompleted?.let { "Последний раз: $it" } ?: "Ещё не выполнялась",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onStartWorkout(workout.id) }) {
                    Text("Начать тренировку")
                }
                OutlinedButton(onClick = { onViewWorkoutDetails(workout.id) }) {
                    Text("Просмотр упражнений")
                }
            }
        }
    }
}
