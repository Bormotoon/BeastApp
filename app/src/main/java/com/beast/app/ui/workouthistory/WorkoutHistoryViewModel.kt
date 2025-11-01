package com.beast.app.ui.workouthistory

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
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

class WorkoutHistoryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database = DatabaseProvider.get(application.applicationContext)
    private val workoutRepository = WorkoutRepository(database)
    private val profileRepository = ProfileRepository(database)
    private val programRepository = ProgramRepository(database)

    private val _uiState = MutableStateFlow(WorkoutHistoryUiState(isLoading = true))
    val uiState: StateFlow<WorkoutHistoryUiState> = _uiState

    init {
        viewModelScope.launch { loadHistory() }
    }

    fun refresh() {
        viewModelScope.launch { loadHistory() }
    }

    private suspend fun loadHistory() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, message = null)
        try {
            val app = getApplication<Application>()
            val prefs = app.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val profile = profileRepository.getProfile()
            val activeProgram = profile?.currentProgramId ?: prefs.getString("current_program_name", null)
            val summary = activeProgram?.let { programRepository.getProgramSummary(it) }

            val logs = workoutRepository.getAllWorkoutLogs()
            val weightUnit = profile?.weightUnit ?: "kg"

            if (logs.isEmpty()) {
                _uiState.value = WorkoutHistoryUiState(
                    isLoading = false,
                    entries = emptyList(),
                    weightUnit = weightUnit,
                    hasActiveProgram = !activeProgram.isNullOrBlank(),
                    message = if (activeProgram.isNullOrBlank()) {
                        "Сначала выберите программу"
                    } else {
                        "Нет завершённых тренировок"
                    }
                )
                return
            }

            val zone = ZoneId.systemDefault()
            val locale = Locale.getDefault()
            val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
            val dayFormatter = DateTimeFormatter.ofPattern("EEE", locale)

            val workoutIds = logs.map { it.workoutId }.distinct()
            val workouts = workoutRepository.getWorkoutsByIds(workoutIds).associateBy { it.id }
            val logIds = logs.map { it.id }
            val aggregates = workoutRepository.getSetAggregates(logIds)

            val epochDaySet = logs
                .map { Instant.ofEpochMilli(it.dateEpochMillis).atZone(zone).toLocalDate().toEpochDay() }
                .toSet()
            val recordDays = profileRepository.getPersonalRecordDates(epochDaySet.toList())

            val entries = logs.map { log ->
                val workout = workouts[log.workoutId]
                val logDate = Instant.ofEpochMilli(log.dateEpochMillis).atZone(zone).toLocalDate()
                val aggregate = aggregates[log.id]
                WorkoutHistoryItem(
                    logId = log.id,
                    workoutId = log.workoutId,
                    dateLabel = logDate.format(dateFormatter).replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(locale) else char.toString()
                    },
                    dayOfWeekLabel = logDate.format(dayFormatter).replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(locale) else char.toString()
                    },
                    workoutName = workout?.name ?: "Неизвестная тренировка",
                    phaseName = summary?.phaseByWorkout?.get(log.workoutId),
                    status = log.status.toHistoryStatus(),
                    statusLabel = formatStatus(log.status),
                    durationLabel = formatDuration(log.totalDuration),
                    volumeLabel = formatVolume(log.totalVolume, weightUnit),
                    setsCount = aggregate?.setCount ?: 0,
                    exercisesCount = aggregate?.exerciseCount ?: 0,
                    rating = log.rating?.takeIf { it in 1..5 },
                    notesPreview = log.notes?.let(::formatNotesPreview),
                    hasRecord = recordDays.contains(logDate.toEpochDay())
                )
            }

            _uiState.value = WorkoutHistoryUiState(
                isLoading = false,
                entries = entries,
                weightUnit = weightUnit,
                hasActiveProgram = !activeProgram.isNullOrBlank(),
                message = null,
                errorMessage = null
            )
        } catch (t: Throwable) {
            _uiState.value = WorkoutHistoryUiState(
                isLoading = false,
                entries = emptyList(),
                weightUnit = _uiState.value.weightUnit,
                hasActiveProgram = _uiState.value.hasActiveProgram,
                message = null,
                errorMessage = t.message ?: "Не удалось загрузить историю"
            )
        }
    }

    private fun formatStatus(rawStatus: String): String {
        return when (rawStatus.uppercase(Locale.getDefault())) {
            "COMPLETED" -> "Завершена"
            "INCOMPLETE" -> "Не завершена"
            else -> rawStatus
        }
    }

    private fun formatDuration(totalMinutes: Int): String? {
        if (totalMinutes <= 0) return null
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours == 0 -> "$minutes мин"
            minutes == 0 -> "$hours ч"
            else -> "$hours ч $minutes мин"
        }
    }

    private fun formatVolume(totalVolume: Double, weightUnit: String): String? {
        if (totalVolume <= 0.0) return null
        val formatted = if (totalVolume % 1.0 == 0.0) {
            totalVolume.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", totalVolume)
        }
        val suffix = if (weightUnit.equals("lbs", ignoreCase = true)) " фунтов" else " кг"
        return formatted + suffix
    }

    private fun formatNotesPreview(text: String): String {
        val sanitized = text.trim().replace('\n', ' ')
        return if (sanitized.length <= 120) sanitized else sanitized.take(117) + "..."
    }
}

data class WorkoutHistoryUiState(
    val isLoading: Boolean = false,
    val entries: List<WorkoutHistoryItem> = emptyList(),
    val weightUnit: String = "kg",
    val hasActiveProgram: Boolean = true,
    val message: String? = null,
    val errorMessage: String? = null
)

data class WorkoutHistoryItem(
    val logId: String,
    val workoutId: String,
    val dateLabel: String,
    val dayOfWeekLabel: String,
    val workoutName: String,
    val phaseName: String?,
    val status: WorkoutHistoryStatus,
    val statusLabel: String,
    val durationLabel: String?,
    val volumeLabel: String?,
    val setsCount: Int,
    val exercisesCount: Int,
    val rating: Int?,
    val notesPreview: String?,
    val hasRecord: Boolean
)

enum class WorkoutHistoryStatus {
    COMPLETED,
    INCOMPLETE,
    UNKNOWN
}

private fun String.toHistoryStatus(): WorkoutHistoryStatus {
    return when (uppercase(Locale.getDefault())) {
        "COMPLETED" -> WorkoutHistoryStatus.COMPLETED
        "INCOMPLETE" -> WorkoutHistoryStatus.INCOMPLETE
        else -> WorkoutHistoryStatus.UNKNOWN
    }
}
