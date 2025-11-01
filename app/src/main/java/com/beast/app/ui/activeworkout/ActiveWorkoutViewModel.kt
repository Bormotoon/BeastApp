package com.beast.app.ui.activeworkout

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.db.SetLogEntity
import com.beast.app.data.repo.ProfileRepository
import com.beast.app.data.repo.ProgramRepository
import com.beast.app.data.repo.WorkoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.Serializable
import java.util.Locale
import kotlin.math.max
import kotlin.math.round

class ActiveWorkoutViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val workoutId: String = savedStateHandle.get<String>("workoutId")
        ?: throw IllegalArgumentException("workoutId is required")

    private val database = DatabaseProvider.get(application.applicationContext)
    private val profileRepository = ProfileRepository(database)
    private val programRepository = ProgramRepository(database)
    private val workoutRepository = WorkoutRepository(database)

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState(isLoading = true))
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState

    private var workoutTimerJob: Job? = null
    private var restTimerJob: Job? = null

    init {
        viewModelScope.launch { loadData() }
        startWorkoutTimer()
    }

    fun toggleTimer() {
        _uiState.update { it.copy(isTimerRunning = !it.isTimerRunning) }
    }

    fun requestFinish() {
        _uiState.update { it.copy(showFinishDialog = true) }
    }

    fun cancelFinish() {
        _uiState.update { it.copy(showFinishDialog = false) }
    }

    fun confirmFinish() {
        workoutTimerJob?.cancel()
        restTimerJob?.cancel()

        val summary = buildCompletionSummary(_uiState.value)

        _uiState.update {
            it.copy(
                showFinishDialog = false,
                isCompleted = true,
                isTimerRunning = false,
                restTimer = null,
                completedResult = summary
            )
        }
    }

    fun acknowledgeCompletion() {
        _uiState.update { it.copy(completedResult = null) }
    }

    fun nextExercise() {
        _uiState.update { state ->
            if (state.currentExerciseIndex >= state.exercises.lastIndex) return@update state
            val newIndex = state.currentExerciseIndex + 1
            state.copy(
                currentExerciseIndex = newIndex,
                currentExerciseId = state.exercises.getOrNull(newIndex)?.id
            )
        }
    }

    fun previousExercise() {
        _uiState.update { state ->
            if (state.currentExerciseIndex <= 0) return@update state
            val newIndex = state.currentExerciseIndex - 1
            state.copy(
                currentExerciseIndex = newIndex,
                currentExerciseId = state.exercises.getOrNull(newIndex)?.id
            )
        }
    }

    fun startRestTimer(totalSeconds: Int = 60) {
        restTimerJob?.cancel()
        if (totalSeconds <= 0) {
            _uiState.update { it.copy(restTimer = null) }
            return
        }
        _uiState.update {
            it.copy(
                restTimer = RestTimerState(
                    totalSeconds = totalSeconds,
                    remainingSeconds = totalSeconds,
                    isRunning = true
                )
            )
        }
        restTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                var shouldStop = false
                _uiState.update { state ->
                    val timer = state.restTimer ?: return@update state
                    if (!timer.isRunning) return@update state
                    val nextValue = timer.remainingSeconds - 1
                    if (nextValue <= 0) {
                        shouldStop = true
                        state.copy(
                            restTimer = timer.copy(remainingSeconds = 0, isRunning = false)
                        )
                    } else {
                        state.copy(
                            restTimer = timer.copy(remainingSeconds = nextValue)
                        )
                    }
                }
                if (shouldStop) break
            }
        }
    }

    fun extendRest(extraSeconds: Int = 15) {
        _uiState.update { state ->
            val timer = state.restTimer ?: return@update state
            state.copy(
                restTimer = timer.copy(
                    totalSeconds = timer.totalSeconds + extraSeconds,
                    remainingSeconds = timer.remainingSeconds + extraSeconds,
                    isRunning = true
                )
            )
        }
    }

    fun skipRest() {
        restTimerJob?.cancel()
        _uiState.update { it.copy(restTimer = null) }
    }

    fun updateWeightInput(exerciseId: String, setIndex: Int, rawValue: String) {
        val sanitized = rawValue.replace(',', '.').filter { it.isDigit() || it == '.' }
        _uiState.updateSet(exerciseId, setIndex) { set ->
            set.copy(weightInput = sanitized)
        }
    }

    fun adjustWeight(exerciseId: String, setIndex: Int, delta: Double) {
        val unit = _uiState.value.weightUnit
        val step = if (unit.equals("lbs", ignoreCase = true)) 5.0 else 2.5
        val appliedDelta = when {
            delta > 0 -> step
            delta < 0 -> -step
            else -> step
        }
        _uiState.updateSet(exerciseId, setIndex) { set ->
            val base = set.weightInput.toDoubleOrNull()
                ?: set.previousWeight
                ?: 0.0
            val updated = max(0.0, base + appliedDelta)
            set.copy(weightInput = formatWeight(updated))
        }
    }

    fun updateRepsInput(exerciseId: String, setIndex: Int, rawValue: String) {
        val sanitized = rawValue.filter { it.isDigit() }
        _uiState.updateSet(exerciseId, setIndex) { set ->
            set.copy(repsInput = sanitized)
        }
    }

    fun adjustReps(exerciseId: String, setIndex: Int, delta: Int) {
        val applied = when {
            delta > 0 -> 1
            delta < 0 -> -1
            else -> 1
        }
        _uiState.updateSet(exerciseId, setIndex) { set ->
            val base = set.repsInput.toIntOrNull()
                ?: set.goalReps?.toIntOrNull()
                ?: set.previousReps
                ?: 0
            val updated = max(0, base + applied)
            set.copy(repsInput = updated.toString())
        }
    }

    fun toggleSetCompleted(exerciseId: String, setIndex: Int) {
        _uiState.updateSet(exerciseId, setIndex) { set ->
            val markCompleted = !set.completed
            val filledWeight = if (markCompleted && set.weightInput.isBlank() && set.previousWeight != null) {
                formatWeight(set.previousWeight)
            } else {
                set.weightInput
            }
            val filledReps = if (markCompleted && set.repsInput.isBlank()) {
                set.goalReps ?: set.previousReps?.toString() ?: ""
            } else {
                set.repsInput
            }
            set.copy(
                completed = markCompleted,
                weightInput = filledWeight,
                repsInput = filledReps
            )
        }
    }

    private fun MutableStateFlow<ActiveWorkoutUiState>.updateSet(
        exerciseId: String,
        setIndex: Int,
        transform: (ActiveSetState) -> ActiveSetState
    ) {
        update { state ->
            val exerciseIdx = state.exercises.indexOfFirst { it.id == exerciseId }
            if (exerciseIdx == -1) return@update state
            val exercise = state.exercises[exerciseIdx]
            if (setIndex !in exercise.sets.indices) return@update state
            val updatedExercise = exercise.copy(
                sets = exercise.sets.mapIndexed { idx, set ->
                    if (idx == setIndex) recalcRecord(transform(set)) else set
                }
            )
            state.copy(
                exercises = state.exercises.toMutableList().also { list ->
                    list[exerciseIdx] = updatedExercise
                }
            )
        }
    }

    private fun recalcRecord(set: ActiveSetState): ActiveSetState {
        val currentVolume = computeCurrentVolume(set)
        val baseline = set.previousVolume ?: 0.0
        val isRecord = set.completed && currentVolume != null && currentVolume > baseline
        return if (set.isNewRecord == isRecord) set else set.copy(isNewRecord = isRecord)
    }

    private fun computeCurrentVolume(set: ActiveSetState): Double? {
        val weight = set.weightInput.toDoubleOrNull() ?: set.previousWeight
        val reps = set.repsInput.toIntOrNull()
            ?: set.goalReps?.toIntOrNull()
            ?: set.previousReps
        if (weight == null || reps == null) return null
        if (weight <= 0.0 || reps <= 0) return null
        return weight * reps
    }

    private fun startWorkoutTimer() {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _uiState.update { state ->
                    if (state.isLoading || !state.isTimerRunning) state
                    else state.copy(elapsedSeconds = state.elapsedSeconds + 1)
                }
            }
        }
    }

    private suspend fun loadData() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val profile = profileRepository.getProfile()
        val preferredProgram = profile?.currentProgramId ?: prefs.getString("current_program_name", null)

        val workoutData = workoutRepository.getWorkoutWithExercises(workoutId)
        if (workoutData == null) {
            _uiState.value = ActiveWorkoutUiState(
                isLoading = false,
                errorMessage = "Тренировка не найдена"
            )
            return
        }

        val summary = when {
            !preferredProgram.isNullOrBlank() -> programRepository.getProgramSummary(preferredProgram)
            else -> {
                val fallback = programRepository.getFirstProgram()
                fallback?.name?.let { programRepository.getProgramSummary(it) }
            }
        }

        val latestLog = workoutRepository.getLogsForWorkout(workoutId).firstOrNull()
        val setLogsByExercise: Map<String, List<SetLogEntity>> = if (latestLog != null) {
            workoutRepository.getSetLogsForWorkoutLog(latestLog.id).groupBy { it.exerciseId }
        } else emptyMap()

        val exercises = workoutData.mappings.mapNotNull { mapping ->
            val exercise = workoutData.exercises.firstOrNull { it.id == mapping.exerciseId } ?: return@mapNotNull null
            val targetRepsList = mapping.targetReps.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val previousLogs = setLogsByExercise[exercise.id]?.sortedBy { it.setNumber } ?: emptyList()

            val totalSets = when {
                targetRepsList.isNotEmpty() -> targetRepsList.size
                previousLogs.isNotEmpty() -> previousLogs.size
                else -> 1
            }

            val sets = (0 until totalSets).map { index ->
                val goal = targetRepsList.getOrNull(index)
                val prev = previousLogs.getOrNull(index)
                val prevVolume = if (prev?.weight != null && prev.reps != null) prev.weight * prev.reps else null
                ActiveSetState(
                    setNumber = index + 1,
                    previousWeight = prev?.weight,
                    previousReps = prev?.reps,
                    previousSummary = buildPreviousSummary(prev),
                    goalReps = goal,
                    weightInput = prev?.weight?.let { formatWeight(it) } ?: "",
                    repsInput = goal ?: prev?.reps?.toString() ?: "",
                    completed = false,
                    previousVolume = prevVolume,
                    isNewRecord = false
                )
            }

            ActiveExerciseState(
                id = exercise.id,
                name = exercise.name,
                setType = mapping.setType,
                setTypeLabel = formatSetType(mapping.setType),
                notes = mapping.notes,
                instructions = exercise.instructions,
                videoUrl = exercise.videoUrl,
                primaryMuscle = exercise.primaryMuscleGroup,
                equipment = exercise.equipment,
                sets = sets
            )
        }

        _uiState.value = ActiveWorkoutUiState(
            isLoading = false,
            workoutId = workoutData.workout.id,
            workoutName = workoutData.workout.name,
            programName = summary?.program?.name,
            phaseName = summary?.phaseByWorkout?.get(workoutId),
            weightUnit = profile?.weightUnit?.lowercase(Locale.getDefault()) ?: "kg",
            workoutMuscleGroups = workoutData.workout.targetMuscleGroups,
            exercises = exercises,
            currentExerciseIndex = 0,
            currentExerciseId = exercises.firstOrNull()?.id,
            totalExercises = exercises.size,
            isTimerRunning = true
        )
    }

    private fun buildCompletionSummary(state: ActiveWorkoutUiState): ActiveWorkoutResult {
        var totalSets = 0
        var completedSets = 0
        var totalVolume = 0.0
        var totalReps = 0

        val exerciseSummaries = state.exercises.map { exercise ->
            var exerciseCompletedSets = 0
            var exerciseVolume = 0.0
            var exerciseReps = 0

            exercise.sets.forEach { set ->
                totalSets += 1
                if (set.completed) {
                    completedSets += 1
                    val weightValue = set.weightInput.toDoubleOrNull()
                        ?: set.previousWeight
                        ?: 0.0
                    val repsValue = set.repsInput.toIntOrNull()
                        ?: set.previousReps
                        ?: set.goalReps?.toIntOrNull()
                        ?: 0
                    val volume = weightValue * repsValue
                    totalVolume += volume
                    totalReps += repsValue
                    exerciseVolume += volume
                    exerciseReps += repsValue
                    exerciseCompletedSets += 1
                }
            }

            CompletedExerciseResult(
                id = exercise.id,
                name = exercise.name,
                totalSets = exercise.sets.size,
                completedSets = exerciseCompletedSets,
                totalVolume = exerciseVolume,
                totalReps = exerciseReps
            )
        }

        return ActiveWorkoutResult(
            workoutId = state.workoutId ?: workoutId,
            workoutName = state.workoutName ?: "",
            programName = state.programName,
            phaseName = state.phaseName,
            durationSeconds = state.elapsedSeconds,
            totalExercises = state.totalExercises,
            totalSets = totalSets,
            completedSets = completedSets,
            totalVolume = totalVolume,
            totalReps = totalReps,
            exerciseSummaries = exerciseSummaries
        )
    }

    private fun buildPreviousSummary(setLog: SetLogEntity?): String {
        if (setLog == null) return "—"
        val weight = setLog.weight
        val reps = setLog.reps
        return when {
            weight != null && reps != null -> "${formatWeight(weight)} × ${reps}"
            weight != null -> formatWeight(weight)
            reps != null -> reps.toString()
            else -> "—"
        }
    }

    private fun formatSetType(raw: String?): String {
        if (raw.isNullOrBlank()) return "Обычный"
        return raw.lowercase(Locale.getDefault())
            .replace('_', ' ')
            .replace('-', ' ')
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

    private fun formatWeight(weight: Double): String {
        return if (weight % 1.0 == 0.0) {
            weight.toInt().toString()
        } else {
            val rounded = round(weight * 10) / 10.0
            if (rounded % 1.0 == 0.0) rounded.toInt().toString() else String.format(Locale.getDefault(), "%.1f", rounded)
        }
    }
}

data class ActiveWorkoutUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val workoutId: String? = null,
    val workoutName: String? = null,
    val programName: String? = null,
    val phaseName: String? = null,
    val workoutMuscleGroups: List<String> = emptyList(),
    val weightUnit: String = "kg",
    val exercises: List<ActiveExerciseState> = emptyList(),
    val totalExercises: Int = 0,
    val currentExerciseIndex: Int = 0,
    val currentExerciseId: String? = null,
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = true,
    val restTimer: RestTimerState? = null,
    val showFinishDialog: Boolean = false,
    val isCompleted: Boolean = false,
    val completedResult: ActiveWorkoutResult? = null
)

data class ActiveExerciseState(
    val id: String,
    val name: String,
    val setType: String?,
    val setTypeLabel: String,
    val notes: String?,
    val instructions: String?,
    val videoUrl: String?,
    val primaryMuscle: String,
    val equipment: List<String>,
    val sets: List<ActiveSetState>
)

data class ActiveSetState(
    val setNumber: Int,
    val previousWeight: Double?,
    val previousReps: Int?,
    val previousSummary: String,
    val goalReps: String?,
    val weightInput: String,
    val repsInput: String,
    val completed: Boolean,
    val previousVolume: Double?,
    val isNewRecord: Boolean
)

data class RestTimerState(
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean
)

data class ActiveWorkoutResult(
    val workoutId: String,
    val workoutName: String,
    val programName: String?,
    val phaseName: String?,
    val durationSeconds: Int,
    val totalExercises: Int,
    val totalSets: Int,
    val completedSets: Int,
    val totalVolume: Double,
    val totalReps: Int,
    val exerciseSummaries: List<CompletedExerciseResult>
) : Serializable

data class CompletedExerciseResult(
    val id: String,
    val name: String,
    val totalSets: Int,
    val completedSets: Int,
    val totalVolume: Double,
    val totalReps: Int
) : Serializable