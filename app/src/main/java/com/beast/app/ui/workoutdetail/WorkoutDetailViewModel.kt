package com.beast.app.ui.workoutdetail

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.repo.ProgramRepository
import com.beast.app.data.repo.ProfileRepository
import com.beast.app.data.repo.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class WorkoutDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val workoutId: String = savedStateHandle.get<String>("workoutId")
        ?: throw IllegalArgumentException("workoutId is required")

    private val database = DatabaseProvider.get(application.applicationContext)
    private val profileRepository = ProfileRepository(database)
    private val programRepository = ProgramRepository(database)
    private val workoutRepository = WorkoutRepository(database)

    private val _uiState = MutableStateFlow(WorkoutDetailUiState(isLoading = true))
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState

    init {
        viewModelScope.launch { loadData() }
    }

    fun refresh() {
        viewModelScope.launch { loadData() }
    }

    private suspend fun loadData() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val profile = profileRepository.getProfile()
        val preferredProgram = profile?.currentProgramId ?: prefs.getString("current_program_name", null)

        val summary = when {
            !preferredProgram.isNullOrBlank() -> programRepository.getProgramSummary(preferredProgram)
            else -> {
                val fallback = programRepository.getFirstProgram()
                fallback?.name?.let { programRepository.getProgramSummary(it) }
            }
        }

        val workoutWithExercises = workoutRepository.getWorkoutWithExercises(workoutId)

        if (workoutWithExercises == null) {
            _uiState.value = WorkoutDetailUiState(
                isLoading = false,
                errorMessage = "Тренировка не найдена"
            )
            return
        }

        val phaseName = summary?.phaseByWorkout?.get(workoutId)
        val programName = summary?.program?.name

        val logs = workoutRepository.getLogsForWorkout(workoutId)
        val latestLog = logs.firstOrNull()

        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

        val lastCompletedDate = latestLog?.dateEpochMillis?.let { millis ->
            Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        }
        val lastCompletedLabel = lastCompletedDate?.format(formatter)

        val exercises = workoutWithExercises.mappings.sortedBy { it.orderIndex }.mapNotNull { mapping ->
            val exercise = workoutWithExercises.exercises.firstOrNull { it.id == mapping.exerciseId }
                ?: return@mapNotNull null
            WorkoutExerciseUiModel(
                id = exercise.id,
                name = exercise.name,
                order = mapping.orderIndex + 1,
                setType = mapping.setType,
                setTypeLabel = mapping.setType.replace('_', ' ').replace('-', ' ').lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) },
                targetReps = mapping.targetReps,
                notes = mapping.notes,
                primaryMuscle = exercise.primaryMuscleGroup,
                equipment = exercise.equipment,
                instructions = exercise.instructions,
                videoUrl = exercise.videoUrl
            )
        }

        _uiState.value = WorkoutDetailUiState(
            isLoading = false,
            programName = programName,
            phaseName = phaseName,
            workoutId = workoutWithExercises.workout.id,
            workoutName = workoutWithExercises.workout.name,
            durationMinutes = workoutWithExercises.workout.durationMinutes,
            muscleGroups = workoutWithExercises.workout.targetMuscleGroups,
            exerciseCount = exercises.size,
            exercises = exercises,
            lastCompletedLabel = lastCompletedLabel
        )
    }
}

data class WorkoutDetailUiState(
    val isLoading: Boolean = false,
    val programName: String? = null,
    val phaseName: String? = null,
    val workoutId: String? = null,
    val workoutName: String? = null,
    val durationMinutes: Int = 0,
    val muscleGroups: List<String> = emptyList(),
    val exerciseCount: Int = 0,
    val lastCompletedLabel: String? = null,
    val exercises: List<WorkoutExerciseUiModel> = emptyList(),
    val errorMessage: String? = null
)

data class WorkoutExerciseUiModel(
    val id: String,
    val name: String,
    val order: Int,
    val setType: String,
    val setTypeLabel: String,
    val targetReps: String,
    val notes: String?,
    val primaryMuscle: String,
    val equipment: List<String>,
    val instructions: String?,
    val videoUrl: String?
)