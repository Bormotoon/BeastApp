@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.beast.app.ui.workoutcompletion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beast.app.ui.activeworkout.CompletedExerciseResult
import com.beast.app.ui.activeworkout.CompletedSetResult

@Composable
fun WorkoutCompletionRoute(
    onBackToHome: () -> Unit,
    onDiscard: () -> Unit,
    viewModel: WorkoutCompletionViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            onBackToHome()
        }
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.dismissError()
    }

    WorkoutCompletionScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onSave = viewModel::saveWorkout,
        onDiscard = onDiscard,
        onBackToHome = onBackToHome,
        onNotesChange = viewModel::updateNotes
    )
}

@Composable
private fun WorkoutCompletionScreen(
    state: WorkoutCompletionUiState,
    snackbarHostState: SnackbarHostState,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onBackToHome: () -> Unit,
    onNotesChange: (String) -> Unit
) {
    val result = state.result
    val records = remember(result) { result.setResults.filter { it.isRecord } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тренировка завершена!", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            CompletionActions(
                isSaving = state.isSaving,
                onSave = onSave,
                onDiscard = onDiscard
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CompletionHero(
                    workoutName = result.workoutName,
                    phaseName = result.phaseName,
                    durationSeconds = result.durationSeconds,
                    completedSets = result.completedSets,
                    totalSets = result.totalSets
                )
            }
            item {
                SummaryStats(result = result)
            }
            item {
                RecordsSection(records = records)
            }
            item {
                ExerciseSummarySection(exercises = result.exerciseSummaries)
            }
            item {
                NotesSection(
                    notes = state.notes.orEmpty(),
                    onNotesChange = onNotesChange
                )
            }
        }
    }
}

@Composable
private fun CompletionHero(
    workoutName: String,
    phaseName: String?,
    durationSeconds: Int,
    completedSets: Int,
    totalSets: Int
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(text = "Отличная работа!", style = MaterialTheme.typography.headlineMedium)
            Text(text = workoutName, style = MaterialTheme.typography.titleLarge)
            phaseName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Длительность: ${formatDuration(durationSeconds)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Подходы: $completedSets из $totalSets",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun SummaryStats(result: com.beast.app.ui.activeworkout.ActiveWorkoutResult) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Статистика", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatChip(label = "Объем", value = "${formatVolume(result.totalVolume)} кг")
                StatChip(label = "Повторы", value = result.totalReps.toString())
                StatChip(label = "Упражнения", value = result.totalExercises.toString())
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecordsSection(records: List<CompletedSetResult>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Достижения", style = MaterialTheme.typography.titleMedium)
            if (records.isEmpty()) {
                Text(
                    text = "Новых личных рекордов в этот раз нет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                records.forEach { record ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = record.exerciseName, style = MaterialTheme.typography.titleSmall)
                            val repsText = record.reps?.let { "$it повт." } ?: ""
                            val weightText = record.weight?.let { "${formatVolume(it)} кг" } ?: ""
                            Text(
                                text = listOf(weightText, repsText)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseSummarySection(exercises: List<CompletedExerciseResult>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Статистика по упражнениям", style = MaterialTheme.typography.titleMedium)
            exercises.forEach { exercise ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = exercise.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Подходы: ${exercise.completedSets}/${exercise.totalSets}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Объем: ${formatVolume(exercise.totalVolume)} кг · Повторы: ${exercise.totalReps}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesSection(notes: String, onNotesChange: (String) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Комментарий к тренировке", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("Как прошла тренировка?") }
            )
        }
    }
}

@Composable
private fun CompletionActions(isSaving: Boolean, onSave: () -> Unit, onDiscard: () -> Unit) {
    Surface(
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text("Сохранить и завершить")
            }
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Вернуться без сохранения")
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return "%d:%02d".format(minutes, remaining)
}

private fun formatVolume(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        "%.1f".format(value)
    }
}
