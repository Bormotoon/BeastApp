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
import com.beast.app.model.SetType
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

    companion object {
        private const val DEFAULT_REST_SECONDS = 60
        private const val SUPER_GIANT_REST_SECONDS = 60
        private const val FORCE_SET_REST_SECONDS = 10
        private const val FORCE_SET_TOTAL_SETS = 5
        private const val PROGRESSIVE_SET_REST_SECONDS = 90
        private val DEFAULT_PROGRESSIVE_PATTERN = listOf("15", "12", "8", "8", "12", "15")
    }

    private data class RestTrigger(val seconds: Int, val showDialog: Boolean)
    private data class GroupInfo(val id: String?, val position: Int, val size: Int)

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
            val order = state.visibleExerciseIndices
            if (order.isEmpty()) return@update state
            val currentPosition = order.indexOf(state.currentExerciseIndex)
            if (currentPosition == -1 || currentPosition >= order.lastIndex) return@update state
            val newIndex = order[currentPosition + 1]
            state.copy(
                currentExerciseIndex = newIndex,
                currentExerciseId = state.exercises.getOrNull(newIndex)?.id
            )
        }
    }

    fun previousExercise() {
        _uiState.update { state ->
            val order = state.visibleExerciseIndices
            if (order.isEmpty()) return@update state
            val currentPosition = order.indexOf(state.currentExerciseIndex)
            if (currentPosition <= 0) return@update state
            val newIndex = order[currentPosition - 1]
            state.copy(
                currentExerciseIndex = newIndex,
                currentExerciseId = state.exercises.getOrNull(newIndex)?.id
            )
        }
    }

    fun startRestTimer(totalSeconds: Int = DEFAULT_REST_SECONDS, showDialog: Boolean = true) {
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
                    isRunning = true,
                    showDialog = showDialog,
                    shouldNotify = false
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
                            restTimer = timer.copy(
                                remainingSeconds = 0,
                                isRunning = false,
                                showDialog = false,
                                shouldNotify = true
                            )
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
                    isRunning = true,
                    showDialog = true,
                    shouldNotify = false
                )
            )
        }
    }

    fun skipRest() {
        restTimerJob?.cancel()
        _uiState.update { it.copy(restTimer = null) }
    }

    fun hideRestDialog() {
        _uiState.update { state ->
            val timer = state.restTimer ?: return@update state
            if (!timer.showDialog) return@update state
            state.copy(restTimer = timer.copy(showDialog = false))
        }
    }

    fun showRestDialog() {
        _uiState.update { state ->
            val timer = state.restTimer ?: return@update state
            if (timer.showDialog) return@update state
            state.copy(restTimer = timer.copy(showDialog = true))
        }
    }

    fun acknowledgeRestTimerAlert() {
        _uiState.update { state ->
            val timer = state.restTimer ?: return@update state
            if (!timer.shouldNotify) return@update state
            state.copy(restTimer = timer.copy(shouldNotify = false))
        }
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
        val currentSet = _uiState.value.exercises
            .firstOrNull { it.id == exerciseId }
            ?.sets
            ?.getOrNull(setIndex)
            ?: return

        val markCompleted = !currentSet.completed
        val targetSetNumber = currentSet.setNumber
        val setType = _uiState.value.exercises.firstOrNull { it.id == exerciseId }?.setType

        _uiState.updateSet(exerciseId, setIndex) { set ->
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

        if (markCompleted) {
            val updatedState = _uiState.value
            val restTrigger = computeRestTrigger(updatedState, exerciseId, targetSetNumber, setType)
            restTrigger?.let { trigger ->
                val shouldShowDialog = trigger.showDialog
                startRestTimer(totalSeconds = trigger.seconds, showDialog = shouldShowDialog)
            }
            maybeAutoAdvanceSpecialSet(_uiState.value, exerciseId)
        }
    }

    private fun computeRestTrigger(
        state: ActiveWorkoutUiState,
        exerciseId: String,
        setNumber: Int,
        setTypeHint: SetType?
    ): RestTrigger? {
        val exercise = state.exercises.firstOrNull { it.id == exerciseId } ?: return null
        val type = setTypeHint ?: exercise.setType
        return when (type) {
            SetType.SUPER, SetType.GIANT -> {
                val groupId = exercise.groupId ?: return null
                val groupMembers = state.exercises.filter { it.groupId == groupId }
                if (groupMembers.size < 2) {
                    val seconds = exercise.restSuggestionSeconds ?: DEFAULT_REST_SECONDS
                    return RestTrigger(seconds = seconds, showDialog = true)
                }
                val allCompletedForSet = groupMembers.all { member ->
                    member.sets.firstOrNull { it.setNumber == setNumber }?.completed == true
                }
                if (allCompletedForSet) {
                    val seconds = exercise.restSuggestionSeconds ?: SUPER_GIANT_REST_SECONDS
                    RestTrigger(seconds = seconds, showDialog = true)
                } else {
                    null
                }
            }
            SetType.FORCE -> {
                val seconds = exercise.restSuggestionSeconds ?: FORCE_SET_REST_SECONDS
                RestTrigger(seconds = seconds, showDialog = seconds >= 20)
            }
            SetType.PROGRESSIVE -> {
                val midpoint = exercise.sets.size / 2
                if (midpoint > 0 && setNumber == midpoint) {
                    RestTrigger(seconds = exercise.restSuggestionSeconds ?: PROGRESSIVE_SET_REST_SECONDS, showDialog = true)
                } else {
                    null
                }
            }
            else -> {
                val seconds = exercise.restSuggestionSeconds ?: DEFAULT_REST_SECONDS
                RestTrigger(seconds = seconds, showDialog = true)
            }
        }
    }

    private fun maybeAutoAdvanceSpecialSet(state: ActiveWorkoutUiState, exerciseId: String) {
        val exerciseIndex = state.exercises.indexOfFirst { it.id == exerciseId }
        if (exerciseIndex == -1) return
        val exercise = state.exercises[exerciseIndex]
        if (exercise.setType != SetType.SUPER && exercise.setType != SetType.GIANT) return
        val groupId = exercise.groupId ?: return
        val groupIndices = state.exercises.mapIndexedNotNull { index, item ->
            if (item.groupId == groupId) index else null
        }
        if (groupIndices.isEmpty()) return
        val groupCompleted = groupIndices.all { index ->
            state.exercises.getOrNull(index)?.sets?.all { it.completed } == true
        }
        if (!groupCompleted) return
        val leaderIndex = groupIndices.minOrNull() ?: return
        if (state.currentExerciseIndex != leaderIndex) return
        val position = state.visibleExerciseIndices.indexOf(leaderIndex)
        if (position == -1 || position >= state.visibleExerciseIndices.lastIndex) return
        nextExercise()
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

        val setTypes = workoutData.mappings.map { parseSetType(it.setType) }
        val groupInfos = computeGroupInfos(setTypes)

        val exercises = workoutData.mappings.mapIndexedNotNull { index, mapping ->
            val exercise = workoutData.exercises.firstOrNull { it.id == mapping.exerciseId } ?: return@mapIndexedNotNull null
            val setType = setTypes[index]
            val rawTargets = mapping.targetReps.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val previousLogs = setLogsByExercise[exercise.id]?.sortedBy { it.setNumber } ?: emptyList()

            val targetRepsList = when (setType) {
                SetType.FORCE -> (0 until FORCE_SET_TOTAL_SETS).map { step ->
                    rawTargets.getOrNull(step)?.takeIf { it.isNotBlank() } ?: "5"
                }
                SetType.PROGRESSIVE -> {
                    if (rawTargets.isEmpty()) {
                        DEFAULT_PROGRESSIVE_PATTERN
                    } else {
                        val merged = DEFAULT_PROGRESSIVE_PATTERN.toMutableList()
                        rawTargets.forEachIndexed { idx, value ->
                            if (idx < merged.size && value.isNotBlank()) {
                                merged[idx] = value
                            }
                        }
                        merged
                    }
                }
                else -> rawTargets
            }

            val totalSets = when (setType) {
                SetType.FORCE -> FORCE_SET_TOTAL_SETS
                SetType.PROGRESSIVE -> targetRepsList.size
                else -> when {
                    targetRepsList.isNotEmpty() -> targetRepsList.size
                    previousLogs.isNotEmpty() -> previousLogs.size
                    else -> 1
                }
            }

            val sets = (0 until totalSets).map { setIdx ->
                val goal = targetRepsList.getOrNull(setIdx)
                val prev = previousLogs.getOrNull(setIdx)
                val prevVolume = if (prev?.weight != null && prev.reps != null) prev.weight * prev.reps else null
                ActiveSetState(
                    setNumber = setIdx + 1,
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

            val groupInfo = groupInfos.getOrNull(index) ?: GroupInfo(null, 0, 1)

            ActiveExerciseState(
                id = exercise.id,
                name = exercise.name,
                setType = setType,
                setTypeLabel = formatSetType(setType),
                notes = mapping.notes,
                instructions = exercise.instructions,
                videoUrl = exercise.videoUrl,
                primaryMuscle = exercise.primaryMuscleGroup,
                equipment = exercise.equipment,
                sets = sets,
                groupId = groupInfo.id,
                groupPosition = groupInfo.position,
                groupSize = groupInfo.size,
                restSuggestionSeconds = restSuggestionFor(setType)
            )
        }

        val visibleIndices = exercises.mapIndexedNotNull { idx, exercise ->
            val info = groupInfos.getOrNull(idx)
            if (info?.id == null || info.position == 0) idx else null
        }
        val displayIndices = if (visibleIndices.isNotEmpty()) visibleIndices else exercises.indices.toList()
        val totalDisplayExercises = if (exercises.isEmpty()) 0 else displayIndices.size
        val initialExerciseIndex = displayIndices.firstOrNull() ?: 0
        val currentExerciseId = exercises.getOrNull(initialExerciseIndex)?.id

        _uiState.value = ActiveWorkoutUiState(
            isLoading = false,
            workoutId = workoutData.workout.id,
            workoutName = workoutData.workout.name,
            programName = summary?.program?.name,
            phaseName = summary?.phaseByWorkout?.get(workoutId),
            weightUnit = profile?.weightUnit?.lowercase(Locale.getDefault()) ?: "kg",
            workoutMuscleGroups = workoutData.workout.targetMuscleGroups,
            exercises = exercises,
            visibleExerciseIndices = displayIndices,
            totalExercises = totalDisplayExercises,
            currentExerciseIndex = initialExerciseIndex,
            currentExerciseId = currentExerciseId,
            isTimerRunning = true
        )
    }

    private fun restSuggestionFor(type: SetType): Int = when (type) {
        SetType.SUPER, SetType.GIANT -> SUPER_GIANT_REST_SECONDS
        SetType.FORCE -> FORCE_SET_REST_SECONDS
        SetType.PROGRESSIVE -> PROGRESSIVE_SET_REST_SECONDS
        else -> DEFAULT_REST_SECONDS
    }

    private fun parseSetType(raw: String?): SetType {
        if (raw.isNullOrBlank()) return SetType.SINGLE
        return runCatching { SetType.valueOf(raw.uppercase(Locale.getDefault())) }.getOrElse { SetType.SINGLE }
    }

    private fun computeGroupInfos(setTypes: List<SetType>): List<GroupInfo> {
        if (setTypes.isEmpty()) return emptyList()
        val result = MutableList(setTypes.size) { GroupInfo(id = null, position = 0, size = 1) }
        var index = 0
        var groupCounter = 0
        while (index < setTypes.size) {
            val type = setTypes[index]
            val expectedSize = when (type) {
                SetType.SUPER -> 2
                SetType.GIANT -> 3
                else -> 1
            }
            val groupIndices = mutableListOf<Int>()
            for (offset in 0 until expectedSize) {
                val candidate = index + offset
                if (candidate >= setTypes.size) break
                if (setTypes[candidate] == type) {
                    groupIndices += candidate
                } else {
                    break
                }
            }
            if (groupIndices.isEmpty()) {
                result[index] = GroupInfo(id = null, position = 0, size = 1)
                index += 1
                continue
            }
            val actualSize = groupIndices.size
            val groupId = if (actualSize > 1) {
                "${type.name.lowercase(Locale.getDefault())}_$groupCounter"
            } else {
                null
            }
            groupIndices.forEachIndexed { position, idx ->
                result[idx] = GroupInfo(id = groupId, position = position, size = actualSize)
            }
            if (groupId != null) groupCounter += 1
            index += actualSize
        }
        return result
    }

    private fun buildCompletionSummary(state: ActiveWorkoutUiState): ActiveWorkoutResult {
        var totalSets = 0
        var completedSets = 0
        var totalVolume = 0.0
        var totalReps = 0
        val setResults = mutableListOf<CompletedSetResult>()

        val exerciseSummaries = state.exercises.map { exercise ->
            var exerciseCompletedSets = 0
            var exerciseVolume = 0.0
            var exerciseReps = 0

            exercise.sets.forEach { set ->
                totalSets += 1
                val weightValue = set.weightInput.toDoubleOrNull()
                    ?: set.previousWeight
                val repsValue = set.repsInput.toIntOrNull()
                    ?: set.goalReps?.toIntOrNull()
                    ?: set.previousReps
                val volume = if (weightValue != null && repsValue != null) weightValue * repsValue else null
                if (set.completed) {
                    completedSets += 1
                    if (volume != null) {
                        totalVolume += volume
                        exerciseVolume += volume
                    }
                    if (repsValue != null) {
                        totalReps += repsValue
                        exerciseReps += repsValue
                    }
                    exerciseCompletedSets += 1
                }
                setResults += CompletedSetResult(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    setNumber = set.setNumber,
                    weight = weightValue,
                    reps = repsValue,
                    goalReps = set.goalReps,
                    completed = set.completed,
                    volume = volume,
                    isRecord = set.isNewRecord
                )
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
            exerciseSummaries = exerciseSummaries,
            setResults = setResults
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

    private fun formatSetType(type: SetType): String = when (type) {
        SetType.SINGLE -> "Обычный сет"
        SetType.SUPER -> "Super Set"
        SetType.GIANT -> "Giant Set"
        SetType.MULTI -> "Multi Set"
        SetType.FORCE -> "Force Set"
        SetType.PROGRESSIVE -> "Progressive Set"
        SetType.COMBO -> "Combo Set"
        SetType.CIRCUIT -> "Circuit"
        SetType.TEMPO -> "Tempo Set"
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
    val visibleExerciseIndices: List<Int> = emptyList(),
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
    val setType: SetType,
    val setTypeLabel: String,
    val notes: String?,
    val instructions: String?,
    val videoUrl: String?,
    val primaryMuscle: String,
    val equipment: List<String>,
    val sets: List<ActiveSetState>,
    val groupId: String? = null,
    val groupPosition: Int = 0,
    val groupSize: Int = 1,
    val restSuggestionSeconds: Int? = null
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
    val isRunning: Boolean,
    val showDialog: Boolean,
    val shouldNotify: Boolean
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
    val exerciseSummaries: List<CompletedExerciseResult>,
    val setResults: List<CompletedSetResult>
) : Serializable

data class CompletedExerciseResult(
    val id: String,
    val name: String,
    val totalSets: Int,
    val completedSets: Int,
    val totalVolume: Double,
    val totalReps: Int
) : Serializable

data class CompletedSetResult(
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val weight: Double?,
    val reps: Int?,
    val goalReps: String?,
    val completed: Boolean,
    val volume: Double?,
    val isRecord: Boolean
) : Serializable