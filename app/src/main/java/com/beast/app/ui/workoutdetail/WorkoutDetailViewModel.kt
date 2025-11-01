package com.beast.app.ui.workoutdetail

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
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
        val weightUnit = profile?.weightUnit ?: "kg"
        val bestVolume = logs.maxOfOrNull { it.totalVolume } ?: 0.0

        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

        val lastCompletedDate = latestLog?.dateEpochMillis?.let { millis ->
            Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        }
        val lastCompletedLabel = lastCompletedDate?.format(formatter)

        val historyItems = logs.map { log ->
            val logDate = Instant.ofEpochMilli(log.dateEpochMillis).atZone(zone)
            val isBestVolume = log.totalVolume > 0 && log.totalVolume >= bestVolume
            WorkoutHistoryItemUiModel(
                id = log.id,
                dateLabel = logDate.format(DateTimeFormatter.ofPattern("d MMM yyyy, EEE", Locale.getDefault())),
                statusLabel = formatStatus(log.status),
                durationLabel = formatDurationLabel(log.totalDuration),
                volumeLabel = formatVolumeLabel(log.totalVolume, weightUnit),
                repsLabel = formatRepsLabel(log.totalReps),
                caloriesLabel = log.calories?.takeIf { it > 0 }?.let { "$it ккал" },
                ratingLabel = formatRatingLabel(log.rating),
                notesPreview = log.notes?.let(::formatNotesPreview),
                isBestVolume = isBestVolume,
                canRepeat = log.status.equals("COMPLETED", ignoreCase = true)
            )
        }

        val chartPoints = logs
            .sortedBy { it.dateEpochMillis }
            .map { log ->
                val label = Instant.ofEpochMilli(log.dateEpochMillis)
                    .atZone(zone)
                    .format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
                val intensity = if (log.totalDuration > 0 && log.totalVolume > 0.0) {
                    (log.totalVolume / log.totalDuration).toFloat()
                } else {
                    null
                }
                WorkoutTrendPoint(
                    label = label,
                    volume = log.totalVolume.takeIf { it > 0.0 }?.toFloat(),
                    durationMinutes = log.totalDuration.takeIf { it > 0 }?.toFloat(),
                    intensity = intensity
                )
            }

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
            lastCompletedLabel = lastCompletedLabel,
            weightUnit = weightUnit,
            history = historyItems,
            chartPoints = chartPoints
        )
    }

    private fun formatDurationLabel(totalMinutes: Int): String? {
        if (totalMinutes <= 0) return null
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return buildString {
            if (hours > 0) {
                append(hours)
                append(" ч")
            }
            if (minutes > 0) {
                if (isNotEmpty()) append(' ')
                append(minutes)
                append(" мин")
            }
        }.ifBlank { "$totalMinutes мин" }
    }

    private fun formatVolumeLabel(volume: Double, weightUnit: String): String? {
        if (volume <= 0.0) return null
        val formatted = if (volume % 1.0 == 0.0) {
            volume.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", volume)
        }
        val suffix = if (weightUnit.equals("lbs", ignoreCase = true)) " фунтов" else " кг"
        return formatted + suffix
    }

    private fun formatRepsLabel(totalReps: Int): String? {
        if (totalReps <= 0) return null
        return "$totalReps повт."
    }

    private fun formatStatus(rawStatus: String): String {
        return when (rawStatus.uppercase(Locale.getDefault())) {
            "COMPLETED" -> "Завершена"
            "INCOMPLETE" -> "Не завершена"
            else -> rawStatus
        }
    }

    private fun formatRatingLabel(rating: Int?): String? {
        val value = rating ?: return null
        if (value <= 0) return null
        return "$value/5"
    }

    private fun formatNotesPreview(notes: String): String {
        val sanitized = notes.trim().replace('\n', ' ')
        return if (sanitized.length <= 120) sanitized else sanitized.take(117) + "..."
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
    val errorMessage: String? = null,
    val weightUnit: String = "kg",
    val history: List<WorkoutHistoryItemUiModel> = emptyList(),
    val chartPoints: List<WorkoutTrendPoint> = emptyList()
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

data class WorkoutHistoryItemUiModel(
    val id: String,
    val dateLabel: String,
    val statusLabel: String,
    val durationLabel: String?,
    val volumeLabel: String?,
    val repsLabel: String?,
    val caloriesLabel: String?,
    val ratingLabel: String?,
    val notesPreview: String?,
    val isBestVolume: Boolean,
    val canRepeat: Boolean
)

data class WorkoutTrendPoint(
    val label: String,
    val volume: Float?,
    val durationMinutes: Float?,
    val intensity: Float?
)