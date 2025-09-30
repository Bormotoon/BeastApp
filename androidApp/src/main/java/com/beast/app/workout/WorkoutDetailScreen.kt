package com.beast.app.workout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beast.app.AppStateViewModel
import com.beast.shared.model.Exercise
import com.beast.shared.model.SetLog
import com.beast.shared.model.Units
import com.beast.shared.model.WorkoutLog
import com.beast.shared.repository.ExerciseRepository
import com.beast.shared.repository.WorkoutDayRepository
import com.beast.shared.repository.WorkoutLogRepository
import com.beast.shared.usecase.LogWorkoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val dayRepo: WorkoutDayRepository,
    private val exerciseRepo: ExerciseRepository,
    private val logsRepo: WorkoutLogRepository,
) : ViewModel() {
    data class UiState(
        val title: String = "",
        val currentLog: WorkoutLog? = null,
        val exercises: List<Exercise> = emptyList(),
        val pendingByExercise: Map<String, List<SetLog>> = emptyMap(),
        val nextSetIndexByExercise: Map<String, Int> = emptyMap(),
        val manualMode: Boolean = true,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var currentProgramId: String = ""
    private var currentDayIndex: Int = 0

    fun load(programId: String, dayIndex: Int) {
        currentProgramId = programId
        currentDayIndex = dayIndex
        viewModelScope.launch {
            val day = runCatching { dayRepo.getByProgram(programId) }.getOrNull().orEmpty()
                .firstOrNull { it.dayIndex == dayIndex }
            val title = day?.title ?: "Workout Day $dayIndex"
            val openLog = logsRepo.getLogsForDay(programId, dayIndex).maxByOrNull { it.date }?.takeIf { !it.completed }
            val ids = day?.exercisesOrder.orEmpty()
            val exercises = if (ids.isNotEmpty()) runCatching { exerciseRepo.getByIds(ids) }.getOrNull().orEmpty() else emptyList()
            _state.value = UiState(
                title = title,
                currentLog = openLog,
                exercises = exercises,
                pendingByExercise = emptyMap(),
                nextSetIndexByExercise = emptyMap(),
                manualMode = exercises.isEmpty(),
            )
        }
    }

    fun startSession(programId: String, dayIndex: Int) {
        viewModelScope.launch {
            val log = WorkoutLog(
                id = UUID.randomUUID().toString(),
                programId = programId,
                dayIndex = dayIndex,
                date = System.currentTimeMillis(),
                completed = false,
                notes = null
            )
            LogWorkoutUseCase(logsRepo).invoke(log, emptyList())
            _state.value = _state.value.copy(currentLog = log, pendingByExercise = emptyMap(), nextSetIndexByExercise = emptyMap())
        }
    }

    fun addSet(exerciseId: String?, reps: Int, weight: Double) {
        val log = _state.value.currentLog ?: return
        val exId = exerciseId ?: "manual"
        val key = exId
        val currentIndex = _state.value.nextSetIndexByExercise[key] ?: 1
        val set = SetLog(
            id = UUID.randomUUID().toString(),
            workoutDayId = "${currentProgramId}-day-${currentDayIndex}",
            exerciseId = exId,
            setIndex = currentIndex,
            reps = reps,
            weight = weight,
            rpe = null,
            timestamp = System.currentTimeMillis(),
        )
        val list = _state.value.pendingByExercise[key].orEmpty() + set
        val newPending = _state.value.pendingByExercise.toMutableMap().apply { put(key, list) }
        val newIndexMap = _state.value.nextSetIndexByExercise.toMutableMap().apply { put(key, currentIndex + 1) }
        _state.value = _state.value.copy(pendingByExercise = newPending, nextSetIndexByExercise = newIndexMap)
    }

    fun removeSet(exerciseId: String?, setId: String) {
        val key = exerciseId ?: "manual"
        val list = _state.value.pendingByExercise[key].orEmpty().filterNot { it.id == setId }
        val newPending = _state.value.pendingByExercise.toMutableMap().apply { put(key, list) }
        _state.value = _state.value.copy(pendingByExercise = newPending)
    }

    fun updateSet(exerciseId: String?, setId: String, reps: Int, weight: Double) {
        val key = exerciseId ?: "manual"
        val list = _state.value.pendingByExercise[key].orEmpty()
        val newList = list.map { if (it.id == setId) it.copy(reps = reps, weight = weight) else it }
        val newPending = _state.value.pendingByExercise.toMutableMap().apply { put(key, newList) }
        _state.value = _state.value.copy(pendingByExercise = newPending)
    }

    fun finishSession() {
        val cur = _state.value.currentLog ?: return
        val sets = _state.value.pendingByExercise.values.flatten()
        viewModelScope.launch {
            val completed = cur.copy(completed = true, date = System.currentTimeMillis())
            LogWorkoutUseCase(logsRepo).invoke(completed, sets)
            _state.value = _state.value.copy(currentLog = null, pendingByExercise = emptyMap(), nextSetIndexByExercise = emptyMap())
        }
    }
}

@Composable
private fun ManualEntryBlock(labelWeight: String, onAdd: (Int, Double) -> Unit) {
    var repsInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = repsInput,
            onValueChange = { repsInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("Повторения") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = weightInput,
            onValueChange = { weightInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text(labelWeight) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        Button(onClick = {
            val reps = repsInput.toIntOrNull()
            val weight = weightInput.toDoubleOrNull()
            if (reps != null && weight != null) {
                onAdd(reps, weight)
                repsInput = ""; weightInput = ""
            }
        }) { Text("Добавить сет") }
    }
}

@Composable
private fun ExerciseEntryBlock(
    title: String,
    labelWeight: String,
    onAdd: (Int, Double) -> Unit,
    added: List<SetLog>,
    onDelete: (String) -> Unit,
    onUpdate: (String, Int, Double) -> Unit,
) {
    var repsInput by remember(title) { mutableStateOf("") }
    var weightInput by remember(title) { mutableStateOf("") }
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = repsInput,
            onValueChange = { repsInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("Повторения") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = weightInput,
            onValueChange = { weightInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text(labelWeight) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        Button(onClick = {
            val reps = repsInput.toIntOrNull()
            val weight = weightInput.toDoubleOrNull()
            if (reps != null && weight != null) {
                onAdd(reps, weight)
                repsInput = ""; weightInput = ""
            }
        }) { Text("Добавить сет") }
    }
    if (added.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        SetsList(sets = added, labelWeight = labelWeight, onDelete = onDelete, onUpdate = onUpdate)
    }
}

@Composable
private fun SetsList(
    sets: List<SetLog>,
    labelWeight: String,
    onDelete: (String) -> Unit,
    onUpdate: (String, Int, Double) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        sets.forEach { s ->
            SetRow(set = s, labelWeight = labelWeight, onDelete = onDelete, onUpdate = onUpdate)
        }
    }
}

@Composable
private fun SetRow(
    set: SetLog,
    labelWeight: String,
    onDelete: (String) -> Unit,
    onUpdate: (String, Int, Double) -> Unit,
) {
    var editing by remember(set.id) { mutableStateOf(false) }
    var reps by remember(set.id) { mutableStateOf(set.reps.toString()) }
    var weight by remember(set.id) { mutableStateOf(set.weight.toString()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (!editing) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Сет ${set.setIndex}: ${set.reps} x ${set.weight}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { editing = true }) { Text("Редактировать") }
                        Button(onClick = { onDelete(set.id) }) { Text("Удалить") }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Повторения") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text(labelWeight) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        val r = reps.toIntOrNull(); val w = weight.toDoubleOrNull()
                        if (r != null && w != null) {
                            onUpdate(set.id, r, w)
                            editing = false
                        }
                    }) { Text("Сохранить") }
                }
            }
        }
    }
}

@Composable
fun WorkoutDetailScreen(
    programId: String,
    dayIndex: Int,
    vm: WorkoutDetailViewModel = hiltViewModel(),
    onFinished: () -> Unit = {}
) {
    LaunchedEffect(programId, dayIndex) { vm.load(programId, dayIndex) }
    val state by vm.state.collectAsState()
    val appState: AppStateViewModel = hiltViewModel()
    val units by appState.units.collectAsState(initial = Units.Metric)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(state.title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (state.currentLog == null) {
                    Text("Сессия не начата")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { vm.startSession(programId, dayIndex) }) { Text("Начать сессию") }
                } else {
                    Text("Сессия в процессе…")
                    Spacer(Modifier.height(12.dp))

                    val weightLabel = if (units == Units.Imperial) "Вес (lbs)" else "Вес (кг)"

                    if (state.manualMode) {
                        ManualEntryBlock(labelWeight = weightLabel, onAdd = { reps, weight -> vm.addSet(null, reps, weight) })
                        val sets = state.pendingByExercise["manual"].orEmpty()
                        if (sets.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            SetsList(
                                sets = sets,
                                labelWeight = weightLabel,
                                onDelete = { setId -> vm.removeSet(null, setId) },
                                onUpdate = { setId, r, w -> vm.updateSet(null, setId, r, w) }
                            )
                        }
                    } else {
                        state.exercises.forEach { ex ->
                            ExerciseEntryBlock(
                                title = ex.name,
                                labelWeight = weightLabel,
                                onAdd = { reps, weight -> vm.addSet(ex.id, reps, weight) },
                                added = state.pendingByExercise[ex.id].orEmpty(),
                                onDelete = { setId -> vm.removeSet(ex.id, setId) },
                                onUpdate = { setId, r, w -> vm.updateSet(ex.id, setId, r, w) }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    val totalSets = state.pendingByExercise.values.sumOf { it.size }
                    if (totalSets > 0) {
                        Text("Добавленные сеты: $totalSets")
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(onClick = { vm.finishSession(); onFinished() }) { Text("Завершить сессию") }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Экран тренировки (WIP)")
    }
}
