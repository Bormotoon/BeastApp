@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.beast.app.ui.activeworkout

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.window.Dialog
import com.beast.app.model.SetType
import com.beast.app.R

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

    LaunchedEffect(state.restTimer?.shouldNotify) {
        val shouldNotify = state.restTimer?.shouldNotify == true
        if (shouldNotify) {
            triggerRestTimerAlert(context)
            viewModel.acknowledgeRestTimerAlert()
        }
    }

    ActiveWorkoutScreen(
        state = state,
        onBack = onBack,
        onToggleTimer = viewModel::toggleTimer,
        onNextExercise = viewModel::nextExercise,
        onPreviousExercise = viewModel::previousExercise,
        onStartRest = { seconds -> viewModel.startRestTimer(totalSeconds = seconds ?: 60) },
        onRestartRest = { seconds -> viewModel.startRestTimer(totalSeconds = seconds) },
        onExtendRest = viewModel::extendRest,
        onSkipRest = viewModel::skipRest,
        onShowRestDialog = viewModel::showRestDialog,
        onHideRestDialog = viewModel::hideRestDialog,
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
    onStartRest: (Int?) -> Unit,
    onRestartRest: (Int) -> Unit,
    onExtendRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onShowRestDialog: () -> Unit,
    onHideRestDialog: () -> Unit,
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
    val currentGroup = state.currentExerciseGroup()
    val currentLeaderExercise = currentGroup.firstOrNull()
    val currentOrderIndex = state.visibleExerciseIndices.indexOf(state.currentExerciseIndex).takeIf { it >= 0 } ?: 0
    val totalExercises = when {
        state.totalExercises > 0 -> state.totalExercises
        state.visibleExerciseIndices.isNotEmpty() -> state.visibleExerciseIndices.size
        else -> state.exercises.size
    }
    val currentExercisePosition = if (totalExercises == 0) 0 else currentOrderIndex + 1
    val recommendedRestSeconds = currentLeaderExercise?.restSuggestionSeconds
    val hasPreviousExercise = totalExercises > 0 && currentOrderIndex > 0
    val hasNextExercise = totalExercises > 0 && currentOrderIndex < totalExercises - 1

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
            currentGroup.isEmpty() -> {
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
                            currentExercisePosition = currentExercisePosition,
                            onToggleTimer = onToggleTimer,
                            onFinishRequest = onFinishRequest
                        )
                    }
                    item {
                        ExerciseGroupOverviewCard(
                            exercises = currentGroup,
                            weightUnit = state.weightUnit,
                            recommendedRestSeconds = recommendedRestSeconds,
                            onOpenVideo = onOpenVideo
                        )
                    }
                    if (currentLeaderExercise?.setType == SetType.PROGRESSIVE) {
                        item {
                            ProgressivePyramidIndicator(
                                sets = currentLeaderExercise.sets,
                                restSeconds = recommendedRestSeconds
                            )
                        }
                    }
                    currentGroup.forEach { exercise ->
                        item(key = "${exercise.id}_header") {
                            SetsTableHeader(exerciseName = if (currentGroup.size > 1) exercise.name else null)
                        }
                        itemsIndexed(exercise.sets, key = { index, set -> "${exercise.id}_${set.setNumber}_${index}" }) { index, set ->
                            SetRow(
                                state = state,
                                exerciseId = exercise.id,
                                setIndex = index,
                                setState = set,
                                onWeightChange = onWeightChange,
                                onAdjustWeight = onAdjustWeight,
                                onRepsChange = onRepsChange,
                                onAdjustReps = onAdjustReps,
                                onToggleSetCompleted = onToggleSetCompleted
                            )
                        }
                    }
                    item {
                        RestTimerCard(
                            restTimer = state.restTimer,
                            recommendedRestSeconds = recommendedRestSeconds,
                            onStartRest = onStartRest,
                            onRestartRest = onRestartRest,
                            onExtendRest = onExtendRest,
                            onSkipRest = onSkipRest,
                            onShowDialog = onShowRestDialog
                        )
                    }
                    item {
                        NavigationControls(
                            hasPrevious = hasPreviousExercise,
                            hasNext = hasNextExercise,
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

    val restTimer = state.restTimer
    if (restTimer?.showDialog == true) {
        RestTimerDialog(
            restTimer = restTimer,
            onExtendRest = onExtendRest,
            onSkipRest = onSkipRest,
            onHide = onHideRestDialog,
            onRestartRest = onRestartRest
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
private fun ExerciseGroupOverviewCard(
    exercises: List<ActiveExerciseState>,
    weightUnit: String,
    recommendedRestSeconds: Int?,
    onOpenVideo: (String) -> Unit
) {
    val leader = exercises.firstOrNull() ?: return
    val specialHint = specialSetHint(leader.setType, recommendedRestSeconds)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (exercises.size == 1) {
                SingleExerciseOverview(
                    exercise = leader,
                    weightUnit = weightUnit,
                    onOpenVideo = onOpenVideo
                )
                if (specialHint != null || (recommendedRestSeconds != null && recommendedRestSeconds > 0)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        specialHint?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        recommendedRestSeconds?.takeIf { it > 0 }?.let { restSeconds ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "Рекомендованный отдых: ${restSeconds} сек",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = leader.setTypeLabel,
                    style = MaterialTheme.typography.titleLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = leader.setTypeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    recommendedRestSeconds?.takeIf { it > 0 }?.let { restSeconds ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Отдых: ${restSeconds} сек",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                specialHint?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                exercises.forEachIndexed { index, exercise ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            exercise.primaryMuscle.takeIf { it.isNotBlank() }?.let { muscle ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = muscle,
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
                            if (exercise.equipment.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = exercise.equipment.joinToString(", "),
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
                    if (index < exercises.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleExerciseOverview(
    exercise: ActiveExerciseState,
    weightUnit: String,
    onOpenVideo: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            exercise.primaryMuscle.takeIf { it.isNotBlank() }?.let { muscle ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = muscle,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            if (exercise.equipment.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = exercise.equipment.joinToString(", "),
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

@Composable
private fun SetsTableHeader(exerciseName: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        exerciseName?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableHeaderCell(text = "#", weight = 0.6f)
            TableHeaderCell(text = "Прошлое", weight = 1.3f)
            TableHeaderCell(text = "Цель", weight = 0.9f)
            TableHeaderCell(text = "Вес", weight = 1.2f)
            TableHeaderCell(text = "Повторы", weight = 1.0f)
            TableHeaderCell(text = "Действие", weight = 1.3f, textAlign = TextAlign.Center)
        }
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
    val backgroundColor = if (setState.completed) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val border = if (setState.isNewRecord) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
    } else {
        null
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.small,
        color = backgroundColor,
        tonalElevation = if (setState.completed) 2.dp else 0.dp,
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = setState.setNumber.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.6f)
                )
                Text(
                    text = setState.previousSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1.3f)
                )
                Text(
                    text = setState.goalReps ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.9f)
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
                FinishSetButton(
                    completed = setState.completed,
                    onToggle = { onToggleSetCompleted(exerciseId, setIndex) },
                    modifier = Modifier.weight(1.3f)
                )
            }
            if (setState.isNewRecord) {
                RecordBadge()
            }
        }
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
                Icon(
                    Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.cd_decrease_value)
                )
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
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_increase_value)
                )
            }
        }
    }
}

@Composable
private fun FinishSetButton(completed: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    if (completed) {
        OutlinedButton(onClick = onToggle, modifier = modifier) {
            Text("Сбросить")
        }
    } else {
        Button(onClick = onToggle, modifier = modifier) {
            Text("Завершить")
        }
    }
}

@Composable
private fun RecordBadge() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = Icons.Outlined.MilitaryTech, contentDescription = null)
            Text(
                text = "Новый рекорд!",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun RestTimerCard(
    restTimer: RestTimerState?,
    recommendedRestSeconds: Int?,
    onStartRest: (Int?) -> Unit,
    onRestartRest: (Int) -> Unit,
    onExtendRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onShowDialog: () -> Unit
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
                val quickOptions = buildList {
                    recommendedRestSeconds?.let { add(it) }
                    addAll(listOf(30, 45, 60, 90))
                }.distinct()
                Text(
                    text = if (recommendedRestSeconds != null) {
                        "Рекомендованный отдых: ${recommendedRestSeconds} сек"
                    } else {
                        "Выберите длительность отдыха или запустите таймер по умолчанию"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (quickOptions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickOptions.forEach { seconds ->
                            val isRecommended = recommendedRestSeconds != null && seconds == recommendedRestSeconds
                            AssistChip(
                                onClick = { onStartRest(seconds) },
                                label = { Text("${seconds} сек") },
                                colors = if (isRecommended) {
                                    AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    AssistChipDefaults.assistChipColors()
                                }
                            )
                        }
                    }
                }
                val defaultSeconds = recommendedRestSeconds ?: 60
                OutlinedButton(onClick = { onStartRest(recommendedRestSeconds) }) {
                    Text("Старт ${defaultSeconds} сек")
                }
            } else {
                val fraction = if (restTimer.totalSeconds == 0) 0f else restTimer.remainingSeconds.toFloat() / restTimer.totalSeconds
                val isFinished = !restTimer.isRunning && restTimer.remainingSeconds <= 0
                if (isFinished) {
                    Text(
                        text = "Отдых завершён",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Время отдыха истекло — переходите к следующему подходу.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val restartSeconds = restTimer.totalSeconds.takeIf { it > 0 } ?: 60
                        Button(onClick = { onRestartRest(restartSeconds) }, modifier = Modifier.weight(1f)) {
                            Text("Повторить")
                        }
                        OutlinedButton(onClick = onSkipRest, modifier = Modifier.weight(1f)) {
                            Text("Сбросить")
                        }
                        OutlinedButton(onClick = onShowDialog, modifier = Modifier.weight(1f)) {
                            Text("Развернуть")
                        }
                    }
                } else {
                    Text(
                        text = "${formatTime(restTimer.remainingSeconds)} / ${formatTime(restTimer.totalSeconds)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LinearProgressIndicator(progress = { fraction })
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onExtendRest(15) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+15 сек")
                        }
                        OutlinedButton(onClick = onSkipRest, modifier = Modifier.weight(1f)) {
                            Text("Пропустить")
                        }
                        OutlinedButton(onClick = onShowDialog, modifier = Modifier.weight(1f)) {
                            Text("Развернуть")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RestTimerDialog(
    restTimer: RestTimerState,
    onExtendRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onHide: () -> Unit,
    onRestartRest: (Int) -> Unit
) {
    val progress = if (restTimer.totalSeconds == 0) 0f else restTimer.remainingSeconds.toFloat() / restTimer.totalSeconds
    val isFinished = !restTimer.isRunning && restTimer.remainingSeconds <= 0
    Dialog(onDismissRequest = onHide) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(text = "Отдых", style = MaterialTheme.typography.headlineSmall)
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(160.dp),
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                if (isFinished) {
                    Text(
                        text = "Отдых завершён",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Время отдыха истекло — можно продолжать тренировку.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val restartSeconds = restTimer.totalSeconds.takeIf { it > 0 } ?: 60
                        Button(onClick = { onRestartRest(restartSeconds) }, modifier = Modifier.weight(1f)) {
                            Text("Повторить")
                        }
                        OutlinedButton(onClick = onSkipRest, modifier = Modifier.weight(1f)) {
                            Text("Сбросить")
                        }
                    }
                } else {
                    Text(
                        text = "${formatTime(restTimer.remainingSeconds)}",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Text(
                        text = "Всего: ${formatTime(restTimer.totalSeconds)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { onExtendRest(15) }, modifier = Modifier.weight(1f)) {
                            Text("+15 сек")
                        }
                        OutlinedButton(onClick = onSkipRest, modifier = Modifier.weight(1f)) {
                            Text("Пропустить")
                        }
                    }
                }
                OutlinedButton(onClick = onHide, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isFinished) "Закрыть" else "Скрыть")
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

private fun specialSetHint(type: SetType, restSeconds: Int?): String? = when (type) {
    SetType.SUPER -> "Выполняйте упражнения подряд без отдыха. Отдыхайте ${(restSeconds ?: 60)} сек после завершения раунда."
    SetType.GIANT -> "Три упражнения без паузы. Сделайте паузу ${(restSeconds ?: 60)} сек после связки."
    SetType.FORCE -> "Force Set: 5 подходов × 5 повторений. Отдых ${(restSeconds ?: 10)} сек между подходами."
    SetType.PROGRESSIVE -> "Пирамида повторений 15→12→8→8→12→15. Отдых ${(restSeconds ?: 90)} сек в середине."
    else -> null
}

private fun ActiveWorkoutUiState.currentExerciseGroup(): List<ActiveExerciseState> {
    val current = exercises.getOrNull(currentExerciseIndex) ?: return emptyList()
    val groupId = current.groupId
    if (groupId.isNullOrBlank()) return listOf(current)
    val groupMembers = exercises.filter { it.groupId == groupId }
    return if (groupMembers.isEmpty()) listOf(current) else groupMembers
}

private fun triggerRestTimerAlert(context: Context) {
    playRestTimerTone(context)
    vibrateForRestTimer(context)
}

private fun playRestTimerTone(context: Context) {
    runCatching {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone?.play()
    }
}

private fun vibrateForRestTimer(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(VibratorManager::class.java)
        val vibrator = manager?.defaultVibrator
        if (vibrator?.hasVibrator() != true) return
        val effect = VibrationEffect.createOneShot(500L, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        val legacyVibrator = context.getSystemService(Vibrator::class.java)
        if (legacyVibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(500L, VibrationEffect.DEFAULT_AMPLITUDE)
            legacyVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            legacyVibrator.vibrate(500L)
        }
    }
}

@Composable
private fun ProgressivePyramidIndicator(
    sets: List<ActiveSetState>,
    restSeconds: Int?
) {
    val stepLabels = sets.map { it.goalReps ?: it.setNumber.toString() }
    val currentIndex = sets.indexOfFirst { !it.completed }.takeIf { it >= 0 } ?: sets.lastIndex
    val restLabel = restSeconds?.takeIf { it > 0 }?.let { seconds -> "Отдых ${seconds} сек" } ?: "Отдых"

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Прогрессивная пирамида",
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stepLabels.forEachIndexed { index, label ->
                    val isCurrent = index == currentIndex
                    val isCompleted = sets.getOrNull(index)?.completed == true
                    val background = when {
                        isCurrent -> MaterialTheme.colorScheme.primaryContainer
                        isCompleted -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val contentColor = when {
                        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                        isCompleted -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(color = background, contentColor = contentColor, shape = MaterialTheme.shapes.small) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (stepLabels.size >= 4 && index == (stepLabels.size / 2) - 1) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = restLabel,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}