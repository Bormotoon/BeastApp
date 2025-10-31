@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.beast.app.ui.activeworkout

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ActiveWorkoutRoute(
    onBack: () -> Unit,
    onWorkoutCompleted: (ActiveWorkoutResult) -> Unit,
    viewModel: ActiveWorkoutViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.completedResult) {
        val result = state.completedResult ?: return@LaunchedEffect
        onWorkoutCompleted(result)
        viewModel.acknowledgeCompletion()
    }

    ActiveWorkoutScreen(
        state = state,
        onBack = onBack,
        onToggleTimer = viewModel::toggleTimer,
        onNextExercise = viewModel::nextExercise,
        onPreviousExercise = viewModel::previousExercise,
        onStartRest = { viewModel.startRestTimer() },
        onExtendRest = viewModel::extendRest,
        onSkipRest = viewModel::skipRest,
        onFinishRequest = viewModel::requestFinish,
        onCancelFinish = viewModel::cancelFinish,
        onConfirmFinish = viewModel::confirmFinish,
        onWeightChange = viewModel::updateWeightInput,
        onAdjustWeight = viewModel::adjustWeight,
        onRepsChange = viewModel::updateRepsInput,
        onAdjustReps = viewModel::adjustReps,
        onToggleSetCompleted = viewModel::toggleSetCompleted,
        onOpenVideo = { url ->
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
        }
    )
}

@Composable
private fun ActiveWorkoutScreen(
    state: ActiveWorkoutUiState,
    onBack: () -> Unit,
    onToggleTimer: () -> Unit,
    onNextExercise: () -> Unit,
    onPreviousExercise: () -> Unit,
    onStartRest: () -> Unit,
    onExtendRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onFinishRequest: () -> Unit,
    onCancelFinish: () -> Unit,
    onConfirmFinish: () -> Unit,
    onWeightChange: (String, Int, String) -> Unit,
    onAdjustWeight: (String, Int, Double) -> Unit,
    onRepsChange: (String, Int, String) -> Unit,
    onAdjustReps: (String, Int, Int) -> Unit,
    onToggleSetCompleted: (String, Int) -> Unit,
    onOpenVideo: (String) -> Unit
) {
    val currentExercise = state.exercises.getOrNull(state.currentExerciseIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.workoutName ?: "Тренировка",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.errorMessage != null -> {
                ErrorState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    message = state.errorMessage
                )
            }
            currentExercise == null -> {
                ErrorState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    message = "Нет упражнений в тренировке"
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        TopSection(
                            state = state,
                            currentExercisePosition = state.currentExerciseIndex + 1,
                            onToggleTimer = onToggleTimer,
                            onFinishRequest = onFinishRequest
                        )
                    }
                    item {
                        ExerciseOverviewCard(
                            exercise = currentExercise,
                            weightUnit = state.weightUnit,
                            onOpenVideo = onOpenVideo
                        )
                    }
                    item {
                        SetsTableHeader()
                    }
                    itemsIndexed(currentExercise.sets, key = { index, set -> "${currentExercise.id}_${set.setNumber}_${index}" }) { index, set ->
                        SetRow(
                            state = state,
                            exerciseId = currentExercise.id,
                            setIndex = index,
                            setState = set,
                            onWeightChange = onWeightChange,
                            onAdjustWeight = onAdjustWeight,
                            onRepsChange = onRepsChange,
                            onAdjustReps = onAdjustReps,
                            onToggleSetCompleted = onToggleSetCompleted
                        )
                    }
                    item {
                        RestTimerCard(
                            restTimer = state.restTimer,
                            onStartRest = onStartRest,
                            onExtendRest = onExtendRest,
                            onSkipRest = onSkipRest
                        )
                    }
                    item {
                        NavigationControls(
                            hasPrevious = state.currentExerciseIndex > 0,
                            hasNext = state.currentExerciseIndex < state.totalExercises - 1,
                            onPrevious = onPreviousExercise,
                            onNext = onNextExercise
                        )
                    }
                }
            }
        }
    }

    if (state.showFinishDialog) {
        AlertDialog(
            onDismissRequest = onCancelFinish,
            title = { Text("Завершить тренировку?") },
            text = {
                Text("Подтвердите завершение. Данные пока не сохраняются — функция в разработке.")
            },
            confirmButton = {
                Button(onClick = onConfirmFinish) {
                    Text("Завершить")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onCancelFinish) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun TopSection(
    state: ActiveWorkoutUiState,
    currentExercisePosition: Int,
    onToggleTimer: () -> Unit,
    onFinishRequest: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.programName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = state.workoutName ?: "Тренировка",
                style = MaterialTheme.typography.headlineSmall
            )
            if (state.workoutMuscleGroups.isNotEmpty()) {
                Text(
                    text = "Фокус: ${state.workoutMuscleGroups.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val progressFraction = if (state.totalExercises == 0) 0f else (currentExercisePosition.toFloat() / state.totalExercises)
            LinearProgressIndicator(progress = { progressFraction })
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Время",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(state.elapsedSeconds),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Упражнение",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currentExercisePosition из ${state.totalExercises}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onToggleTimer, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (state.isTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (state.isTimerRunning) "Пауза" else "Продолжить")
                }
                OutlinedButton(onClick = onFinishRequest, modifier = Modifier.weight(1f)) {
                    Text("Завершить")
                }
            }
        }
    }
}

@Composable
private fun ExerciseOverviewCard(
    exercise: ActiveExerciseState,
    weightUnit: String,
    onOpenVideo: (String) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleLarge
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = exercise.setTypeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (exercise.primaryMuscle.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = exercise.primaryMuscle,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (weightUnit.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Вес: ${weightUnit.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            exercise.notes?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            exercise.instructions?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            exercise.videoUrl?.takeIf { it.isNotBlank() }?.let { url ->
                OutlinedButton(onClick = { onOpenVideo(url) }) {
                    Text("Видео-инструкция")
                }
            }
        }
    }
}

@Composable
private fun SetsTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableHeaderCell(text = "#", weight = 0.5f)
        TableHeaderCell(text = "Прошлое", weight = 1.2f)
        TableHeaderCell(text = "Цель", weight = 0.8f)
        TableHeaderCell(text = "Вес", weight = 1.2f)
        TableHeaderCell(text = "Повторы", weight = 1.0f)
        TableHeaderCell(text = "✔", weight = 0.5f, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RowScope.TableHeaderCell(text: String, weight: Float, textAlign: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(weight),
        textAlign = textAlign
    )
}

@Composable
private fun SetRow(
    state: ActiveWorkoutUiState,
    exerciseId: String,
    setIndex: Int,
    setState: ActiveSetState,
    onWeightChange: (String, Int, String) -> Unit,
    onAdjustWeight: (String, Int, Double) -> Unit,
    onRepsChange: (String, Int, String) -> Unit,
    onAdjustReps: (String, Int, Int) -> Unit,
    onToggleSetCompleted: (String, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = setState.setNumber.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = setState.previousSummary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = setState.goalReps ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.8f)
        )
        StepField(
            label = state.weightUnit.uppercase(),
            value = setState.weightInput,
            onValueChange = { onWeightChange(exerciseId, setIndex, it) },
            onIncrement = { onAdjustWeight(exerciseId, setIndex, 1.0) },
            onDecrement = { onAdjustWeight(exerciseId, setIndex, -1.0) },
            modifier = Modifier.weight(1.2f)
        )
        StepField(
            label = "Повт.",
            value = setState.repsInput,
            onValueChange = { onRepsChange(exerciseId, setIndex, it) },
            onIncrement = { onAdjustReps(exerciseId, setIndex, 1) },
            onDecrement = { onAdjustReps(exerciseId, setIndex, -1) },
            modifier = Modifier.weight(1.0f),
            allowDecimal = false
        )
        Checkbox(
            checked = setState.completed,
            onCheckedChange = { onToggleSetCompleted(exerciseId, setIndex) },
            modifier = Modifier.weight(0.5f)
        )
    }
}

@Composable
private fun StepField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier,
    allowDecimal: Boolean = true
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement) {
                Icon(Icons.Filled.Remove, contentDescription = "Уменьшить")
            }
            OutlinedTextField(
                value = value,
                onValueChange = {
                    if (!allowDecimal) onValueChange(it.filter { ch -> ch.isDigit() })
                    else onValueChange(it.replace(',', '.'))
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = if (allowDecimal) {
                    KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal)
                } else {
                    KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                }
            )
            IconButton(onClick = onIncrement) {
                Icon(Icons.Filled.Add, contentDescription = "Увеличить")
            }
        }
    }
}

@Composable
private fun RestTimerCard(
    restTimer: RestTimerState?,
    onStartRest: () -> Unit,
    onExtendRest: (Int) -> Unit,
    onSkipRest: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Отдых",
                style = MaterialTheme.typography.titleMedium
            )
            if (restTimer == null) {
                Text(
                    text = "Запустите таймер отдыха после подхода",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onStartRest) {
                    Text("Старт 60 сек")
                }
            } else {
                val fraction = if (restTimer.totalSeconds == 0) 0f else restTimer.remainingSeconds.toFloat() / restTimer.totalSeconds
                Text(
                    text = "${formatTime(restTimer.remainingSeconds)} / ${formatTime(restTimer.totalSeconds)}",
                    style = MaterialTheme.typography.titleMedium
                )
                LinearProgressIndicator(progress = { fraction })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = { onExtendRest(15) }) {
                        Text("+15 сек")
                    }
                    OutlinedButton(onClick = onSkipRest) {
                        Text("Пропустить")
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationControls(
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onPrevious,
            enabled = hasPrevious,
            modifier = Modifier.weight(1f)
        ) {
            Text("Назад")
        }
        Button(
            onClick = onNext,
            enabled = hasNext,
            modifier = Modifier.weight(1f)
        ) {
            Text("Далее")
        }
    }
}

@Composable
private fun ErrorState(modifier: Modifier, message: String) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}