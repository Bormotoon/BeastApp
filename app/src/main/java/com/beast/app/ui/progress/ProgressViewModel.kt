package com.beast.app.ui.progress

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.db.WorkoutEntity
import com.beast.app.data.db.WorkoutLogEntity
import com.beast.app.data.repo.ProgramRepository
import com.beast.app.data.repo.ProfileRepository
import com.beast.app.data.repo.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseProvider.get(application.applicationContext)
    private val workoutRepository = WorkoutRepository(database)
    private val profileRepository = ProfileRepository(database)
    private val programRepository = ProgramRepository(database)

    private val _uiState = MutableStateFlow(ProgressUiState(isLoading = true))
    val uiState: StateFlow<ProgressUiState> = _uiState

    private var loadedData: LoadedData? = null
    private var currentPeriod: ProgressPeriod = ProgressPeriod.PROGRAM

    init {
        viewModelScope.launch { loadProgress() }
    }

    fun refresh() {
        viewModelScope.launch { loadProgress() }
    }

    fun selectPeriod(period: ProgressPeriod) {
        currentPeriod = period
        val data = loadedData
        if (data == null) {
            _uiState.value = _uiState.value.copy(selectedPeriod = period)
        } else {
            emitStateForPeriod(period, data)
        }
    }

    private suspend fun loadProgress() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        try {
            val app = getApplication<Application>()
            val prefs = app.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val profile = profileRepository.getProfile()
            val programId = profile?.currentProgramId ?: prefs.getString("current_program_name", null)
            val programSummary = programId?.let { programRepository.getProgramSummary(it) }

            val logs = workoutRepository.getAllWorkoutLogs().sortedByDescending { it.dateEpochMillis }
            val weightUnit = profile?.weightUnit ?: "kg"
            val startDate = profile?.startDateEpochDay?.let { LocalDate.ofEpochDay(it) }
            val workoutIds = logs.map { it.workoutId }.distinct()
            val workouts = workoutRepository.getWorkoutsByIds(workoutIds).associateBy { it.id }

            val data = LoadedData(
                logs = logs,
                weightUnit = weightUnit,
                programSummary = programSummary,
                startDate = startDate,
                workoutsById = workouts
            )

            loadedData = data

            if (logs.isEmpty()) {
                val currentPhaseName = determineCurrentPhaseName(data)
                val available = buildAvailablePeriods(data, currentPhaseName)
                _uiState.value = ProgressUiState(
                    isLoading = false,
                    stats = null,
                    message = if (programSummary == null) {
                        "Нет данных для отображения. Выберите программу и выполните первую тренировку."
                    } else {
                        "Нет завершённых тренировок. Начните план, чтобы увидеть прогресс."
                    },
                    errorMessage = null,
                    selectedPeriod = currentPeriod,
                    availablePeriods = available
                )
                return
            }

            emitStateForPeriod(currentPeriod, data)
        } catch (t: Throwable) {
            _uiState.value = ProgressUiState(
                isLoading = false,
                stats = null,
                message = null,
                errorMessage = t.message ?: "Не удалось загрузить прогресс"
            )
        }
    }

    private fun emitStateForPeriod(period: ProgressPeriod, data: LoadedData) {
        val currentPhaseName = determineCurrentPhaseName(data)
        val available = buildAvailablePeriods(data, currentPhaseName)
        val effectivePeriod = if (period == ProgressPeriod.BLOCK && !available.contains(ProgressPeriod.BLOCK)) {
            ProgressPeriod.PROGRAM
        } else {
            period
        }
        currentPeriod = effectivePeriod
        val stats = buildStatsForPeriod(effectivePeriod, data, currentPhaseName)
        _uiState.value = ProgressUiState(
            isLoading = false,
            stats = stats,
            message = null,
            errorMessage = null,
            selectedPeriod = effectivePeriod,
            availablePeriods = available
        )
    }

    private fun buildStatsForPeriod(
        period: ProgressPeriod,
        data: LoadedData,
        currentPhaseName: String?
    ): ProgressStats {
        val zone = ZoneId.systemDefault()
        val locale = Locale.getDefault()
        val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
        val dayFormatter = DateTimeFormatter.ofPattern("EEE", locale)

        val filteredLogs = filterLogsForPeriod(period, data, currentPhaseName, zone)
        val sortedLogs = filteredLogs.sortedByDescending { it.dateEpochMillis }
        val completedLogs = sortedLogs.filter { it.status.equals("COMPLETED", ignoreCase = true) }

        val totalWorkouts = sortedLogs.size
        val totalCompleted = completedLogs.size
        val totalMissed = sortedLogs.count { !it.status.equals("COMPLETED", ignoreCase = true) }
        val totalVolume = completedLogs.sumOf { it.totalVolume }
        val totalReps = completedLogs.sumOf { it.totalReps }
        val totalDurationMinutes = completedLogs.sumOf { it.totalDuration }
        val averageDurationMinutes = if (totalCompleted > 0) totalDurationMinutes.toDouble() / totalCompleted else 0.0
        val averageVolume = if (totalCompleted > 0) totalVolume / totalCompleted else 0.0

        val completedDatesAll = data.logs
            .filter { it.status.equals("COMPLETED", ignoreCase = true) }
            .map { Instant.ofEpochMilli(it.dateEpochMillis).atZone(zone).toLocalDate() }
            .toSet()
        val (currentStreak, bestStreak) = calculateStreaks(completedDatesAll)

        val recent = sortedLogs.take(5).map { log ->
            val localDate = Instant.ofEpochMilli(log.dateEpochMillis).atZone(zone).toLocalDate()
            RecentWorkoutSummary(
                logId = log.id,
                workoutId = log.workoutId,
                title = data.workoutsById[log.workoutId]?.name ?: "Неизвестная тренировка",
                subtitle = localDate.format(dayFormatter).replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(locale) else char.toString()
                },
                dateLabel = localDate.format(dateFormatter).replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(locale) else char.toString()
                },
                statusLabel = formatStatus(log.status),
                status = when (log.status.uppercase(locale)) {
                    "COMPLETED" -> RecentWorkoutStatus.COMPLETED
                    "INCOMPLETE" -> RecentWorkoutStatus.MISSED
                    else -> RecentWorkoutStatus.UNKNOWN
                },
                volumeLabel = formatVolume(log.totalVolume, data.weightUnit),
                durationLabel = formatDuration(log.totalDuration)
            )
        }

        val completionPercent = if (totalWorkouts > 0) {
            (totalCompleted * 100.0 / totalWorkouts).roundToInt()
        } else {
            null
        }

        return ProgressStats(
            totalWorkoutsLogged = totalWorkouts,
            totalCompleted = totalCompleted,
            totalMissed = totalMissed,
            completionPercent = completionPercent,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            totalVolume = totalVolume,
            totalReps = totalReps,
            totalDurationMinutes = totalDurationMinutes,
            averageDurationMinutes = averageDurationMinutes,
            averageVolume = averageVolume,
            weightUnit = data.weightUnit,
            programName = data.programSummary?.program?.name,
            recentWorkouts = recent
        )
    }

    private fun filterLogsForPeriod(
        period: ProgressPeriod,
        data: LoadedData,
        currentPhaseName: String?,
        zone: ZoneId
    ): List<WorkoutLogEntity> {
        val today = LocalDate.now(zone)
        return when (period) {
            ProgressPeriod.WEEK -> {
                val threshold = today.minusDays(6)
                data.logs.filter { Instant.ofEpochMilli(it.dateEpochMillis).atZone(zone).toLocalDate() >= threshold }
            }
            ProgressPeriod.MONTH -> {
                val threshold = today.minusDays(29)
                data.logs.filter { Instant.ofEpochMilli(it.dateEpochMillis).atZone(zone).toLocalDate() >= threshold }
            }
            ProgressPeriod.BLOCK -> {
                if (currentPhaseName == null || data.programSummary == null) {
                    data.logs
                } else {
                    data.logs.filter { log ->
                        data.programSummary.phaseByWorkout[log.workoutId] == currentPhaseName
                    }
                }
            }
            ProgressPeriod.PROGRAM -> data.logs
        }
    }

    private fun calculateStreaks(completedDates: Set<LocalDate>): Pair<Int, Int> {
        if (completedDates.isEmpty()) return 0 to 0
        val sorted = completedDates.sorted()
        var best = 1
        var current = 1
        for (i in 1 until sorted.size) {
            val previous = sorted[i - 1]
            val currentDate = sorted[i]
            val diff = ChronoUnit.DAYS.between(previous, currentDate)
            if (diff == 1L) {
                current++
            } else if (diff > 1L) {
                current = 1
            }
            if (current > best) best = current
        }

        val today = LocalDate.now()
        var running = 0
        var pointer = today
        while (completedDates.contains(pointer)) {
            running++
            pointer = pointer.minusDays(1)
        }
        return running to best
    }

    private fun determineCurrentPhaseName(data: LoadedData): String? {
        val summary = data.programSummary ?: return null
        if (summary.schedule.isEmpty()) return null
        val startDate = data.startDate ?: return null
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val daysElapsed = ChronoUnit.DAYS.between(startDate, today).toInt()
        val targetDay = (daysElapsed + 1).coerceIn(1, summary.program.durationDays)
        val scheduleEntry = summary.schedule.firstOrNull { it.dayNumber == targetDay }
            ?: summary.schedule.maxByOrNull { it.dayNumber }
        val workoutId = scheduleEntry?.workoutId ?: return null
        return summary.phaseByWorkout[workoutId]
    }

    private fun buildAvailablePeriods(data: LoadedData, currentPhaseName: String?): List<ProgressPeriod> {
        val base = mutableListOf(ProgressPeriod.WEEK, ProgressPeriod.MONTH)
        if (currentPhaseName != null && data.programSummary?.phaseByWorkout?.values?.contains(currentPhaseName) == true) {
            base += ProgressPeriod.BLOCK
        }
        base += ProgressPeriod.PROGRAM
        return base
    }

    private data class LoadedData(
        val logs: List<WorkoutLogEntity>,
        val weightUnit: String,
        val programSummary: ProgramRepository.ProgramSummary?,
        val startDate: LocalDate?,
        val workoutsById: Map<String, WorkoutEntity>
    )

    private fun formatStatus(rawStatus: String): String {
        return when (rawStatus.uppercase(Locale.getDefault())) {
            "COMPLETED" -> "Завершена"
            "INCOMPLETE" -> "Не завершена"
            else -> rawStatus
        }
    }

    private fun formatVolume(volume: Double, weightUnit: String): String? {
        if (volume <= 0.0) return null
        val formatted = if (volume % 1.0 == 0.0) {
            volume.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", volume)
        }
        val suffix = if (weightUnit.equals("lbs", ignoreCase = true)) " фунтов" else " кг"
        return formatted + suffix
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
}

data class ProgressUiState(
    val isLoading: Boolean = false,
    val stats: ProgressStats? = null,
    val message: String? = null,
    val errorMessage: String? = null,
    val selectedPeriod: ProgressPeriod = ProgressPeriod.PROGRAM,
    val availablePeriods: List<ProgressPeriod> = listOf(
        ProgressPeriod.WEEK,
        ProgressPeriod.MONTH,
        ProgressPeriod.PROGRAM
    )
)

data class ProgressStats(
    val totalWorkoutsLogged: Int,
    val totalCompleted: Int,
    val totalMissed: Int,
    val completionPercent: Int?,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalVolume: Double,
    val totalReps: Int,
    val totalDurationMinutes: Int,
    val averageDurationMinutes: Double,
    val averageVolume: Double,
    val weightUnit: String,
    val programName: String?,
    val recentWorkouts: List<RecentWorkoutSummary>
)

data class RecentWorkoutSummary(
    val logId: String,
    val workoutId: String,
    val title: String,
    val subtitle: String,
    val dateLabel: String,
    val statusLabel: String,
    val status: RecentWorkoutStatus,
    val volumeLabel: String?,
    val durationLabel: String?
)

enum class RecentWorkoutStatus { COMPLETED, MISSED, UNKNOWN }

enum class ProgressPeriod(val displayName: String) {
    WEEK("Неделя"),
    MONTH("Месяц"),
    BLOCK("Блок"),
    PROGRAM("Вся программа")
}
