package com.beast.app.ui.workoutcompletion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.db.SetLogEntity
import com.beast.app.data.db.WorkoutLogEntity
import com.beast.app.data.repo.WorkoutRepository
import com.beast.app.ui.activeworkout.ActiveWorkoutResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import kotlin.math.max

class WorkoutCompletionViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val database = DatabaseProvider.get(application.applicationContext)
    private val workoutRepository = WorkoutRepository(database)

    private val result: ActiveWorkoutResult = savedStateHandle.get<ActiveWorkoutResult>(KEY_RESULT)
        ?: throw IllegalStateException("Workout result is missing")

    private val _uiState = MutableStateFlow(WorkoutCompletionUiState(result = result))
    val uiState: StateFlow<WorkoutCompletionUiState> = _uiState

    fun saveWorkout() {
        val current = _uiState.value
        if (current.isSaving || current.saveCompleted) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val (log, sets) = buildLogPayload(result, current.notes)
                workoutRepository.insertWorkoutLogWithSets(log, sets)
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Не удалось сохранить тренировку"
                    )
                }
            }
        }
    }

    fun updateNotes(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun buildLogPayload(
        result: ActiveWorkoutResult,
        notes: String?
    ): Pair<WorkoutLogEntity, List<SetLogEntity>> {
        val now = Instant.now()
        val workoutLogId = UUID.randomUUID().toString()
        val durationMinutes = max(1, result.durationSeconds / 60)
        val log = WorkoutLogEntity(
            id = workoutLogId,
            workoutId = result.workoutId,
            dateEpochMillis = now.toEpochMilli(),
            totalDuration = durationMinutes,
            totalVolume = result.totalVolume,
            totalReps = result.totalReps,
            calories = null,
            notes = notes?.takeIf { it.isNotBlank() },
            rating = null,
            status = "COMPLETED"
        )

        val setLogs = result.setResults
            .filter { it.completed && (it.weight != null || it.reps != null) }
            .map { set ->
                SetLogEntity(
                    id = UUID.randomUUID().toString(),
                    workoutLogId = workoutLogId,
                    exerciseId = set.exerciseId,
                    setNumber = set.setNumber,
                    weight = set.weight,
                    reps = set.reps,
                    durationSeconds = null,
                    distance = null,
                    side = "NONE",
                    isCompleted = true,
                    notes = null,
                    rpe = null
                )
            }

        return log to setLogs
    }

    companion object {
        const val KEY_RESULT = "workout_result"
    }
}

data class WorkoutCompletionUiState(
    val result: ActiveWorkoutResult,
    val notes: String? = null,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val errorMessage: String? = null
)
