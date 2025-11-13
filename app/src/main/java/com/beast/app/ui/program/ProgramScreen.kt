@file:OptIn(ExperimentalMaterial3Api::class)

package com.beast.app.ui.program

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beast.app.R
import com.beast.app.data.db.ProgramEntity
import com.beast.app.ui.import.ProgramImportDialog

@Composable
fun ProgramRoute(
    onBack: () -> Unit,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit,
    onAddProgram: () -> Unit,
    onSelectProgram: (String) -> Unit,
    viewModel: ProgramViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showImportDialog by remember { mutableStateOf(false) }

    ProgramScreen(
        state = state,
        onBack = onBack,
        onStartWorkout = onStartWorkout,
        onViewWorkoutDetails = onViewWorkoutDetails,
        onAddProgram = { showImportDialog = true },
        onSelectProgram = onSelectProgram,
        onRefresh = viewModel::refresh
    )

    if (showImportDialog) {
        ProgramImportDialog(
            onDismiss = { showImportDialog = false },
            onImportSuccess = {
                showImportDialog = false
                viewModel.refresh()
            }
        )
    }
}

@Composable
private fun ProgramScreen(
    state: ProgramUiState,
    onBack: () -> Unit,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit,
    onAddProgram: () -> Unit,
    onSelectProgram: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Программы",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
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
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.programs) { program ->
                            ProgramCard(
                                program = program,
                                onSelect = { onSelectProgram(program.name) }
                            )
                        }
                    }
                    Button(
                        onClick = onAddProgram,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Добавить программу")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramCard(
    program: ProgramEntity,
    onSelect: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = program.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Длительность: ${program.durationDays} дней",
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
