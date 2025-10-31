package com.beast.app.ui.program

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.repo.ProfileRepository
import com.beast.app.data.repo.ProgramRepository
import com.beast.app.data.repo.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProgramViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DatabaseProvider.get(application.applicationContext)
    private val profileRepository = ProfileRepository(database)
    private val programRepository = ProgramRepository(database)
    private val workoutRepository = WorkoutRepository(database)

    private val _uiState = MutableStateFlow(ProgramUiState())
    val uiState: StateFlow<ProgramUiState> = _uiState

    init {
        viewModelScope.launch { loadData() }
    }

    fun refresh() {
        viewModelScope.launch { loadData() }
    }

    private suspend fun loadData() {
        _uiState.value = ProgramUiState(isLoading = true)

        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val profile = profileRepository.getProfile()
        val preferredProgram = profile?.currentProgramId ?: prefs.getString("current_program_name", null)

        val overview = when {
            !preferredProgram.isNullOrBlank() -> programRepository.getProgramPhaseOverview(preferredProgram)
            else -> {
                val fallback = programRepository.getFirstProgram()
                fallback?.name?.let { programRepository.getProgramPhaseOverview(it) }
            }
        }

        if (overview == null) {
            _uiState.value = ProgramUiState(
                isLoading = false,
                errorMessage = "Нет доступных программ"
            )
            return
        }

        val allWorkoutIds = overview.phases.flatMap { phase -> phase.workouts.map { it.workout.id } }
        val latestLogs = workoutRepository.getLatestLogsForWorkouts(allWorkoutIds)

        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

        val phaseUi = overview.phases.map { phase ->
            val workouts = phase.workouts.map { workout ->
                val log = latestLogs[workout.workout.id]
                val lastCompleted = log?.dateEpochMillis?.let { millis ->
                    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                }
                WorkoutUiModel(
                    id = workout.workout.id,
                    name = workout.workout.name,
                    durationMinutes = workout.workout.durationMinutes,
                    exerciseCount = workout.exerciseCount,
                    muscleGroups = workout.workout.targetMuscleGroups,
                    lastCompleted = lastCompleted,
                    lastCompletedLabel = lastCompleted?.format(formatter)
                )
            }
            PhaseUiModel(
                name = phase.phase.name,
                durationWeeks = phase.phase.durationWeeks,
                workoutCount = workouts.size,
                workouts = workouts
            )
        }

        _uiState.value = ProgramUiState(
            isLoading = false,
            programName = overview.program.name,
            phases = phaseUi
        )
    }
}

data class ProgramUiState(
    val isLoading: Boolean = false,
    val programName: String? = null,
    val phases: List<PhaseUiModel> = emptyList(),
    val errorMessage: String? = null
)

data class PhaseUiModel(
    val name: String,
    val durationWeeks: Int,
    val workoutCount: Int,
    val workouts: List<WorkoutUiModel>
)

data class WorkoutUiModel(
    val id: String,
    val name: String,
    val durationMinutes: Int,
    val exerciseCount: Int,
    val muscleGroups: List<String>,
    val lastCompleted: LocalDate?,
    val lastCompletedLabel: String?
)
