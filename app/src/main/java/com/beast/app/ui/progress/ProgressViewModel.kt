package com.beast.app.ui.progress

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.db.ExerciseEntity
import com.beast.app.data.db.PersonalRecordWithExercise
import com.beast.app.data.db.WorkoutEntity
import com.beast.app.data.db.WorkoutLogEntity
import com.beast.app.data.db.WorkoutLogExerciseAggregate
import com.beast.app.data.repo.ProgramRepository
import com.beast.app.data.repo.ProfileRepository
import com.beast.app.data.repo.WorkoutRepository
import com.beast.app.utils.DateFormatting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

private const val UNKNOWN_MUSCLE_GROUP = "Прочие"

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

            val exerciseAggregates = workoutRepository.getExerciseVolumeAggregates(logs.map { it.id })
            val exerciseIds = exerciseAggregates.map { it.exerciseId }.distinct()
            val exercises = workoutRepository.getExercisesByIds(exerciseIds).associateBy { it.id }
            val aggregatesByLog = exerciseAggregates.groupBy { it.workoutLogId }
            val personalRecords = profileRepository.getTopPersonalRecords(30)

            val data = LoadedData(
                logs = logs,
                weightUnit = weightUnit,
                programSummary = programSummary,
                startDate = startDate,
                workoutsById = workouts,
                exerciseAggregatesByLog = aggregatesByLog,
                exercisesById = exercises,
                personalRecords = personalRecords
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
        val dateFormatter = DateFormatting.dateFormatter(locale, "yMMMMd")
        val dayFormatter = DateFormatting.dateFormatter(locale, "EEE")

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

        val muscleDistribution = buildMuscleDistribution(completedLogs, data.workoutsById)
        val muscleVolume = buildMuscleVolume(completedLogs, data.workoutsById)
        val exerciseVolume = buildExerciseVolume(completedLogs, data)
        val heatmap = buildHeatmap(completedLogs, zone)
        val volumeSeries = buildVolumeSeries(completedLogs, zone)
        val records = buildRecordSummaries(
            period = period,
            filteredLogs = filteredLogs,
            data = data,
            zone = zone,
            dateFormatter = dateFormatter,
            locale = locale
        )

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
            recentWorkouts = recent,
            muscleDistribution = muscleDistribution,
            heatmap = heatmap,
            volumeSeries = volumeSeries,
            muscleVolume = muscleVolume,
            exerciseVolume = exerciseVolume,
            records = records
        )
    }

    private fun buildMuscleDistribution(
        logs: List<WorkoutLogEntity>,
        workouts: Map<String, WorkoutEntity>
    ): List<MuscleGroupShare> {
        if (logs.isEmpty()) return emptyList()
        val counts = linkedMapOf<String, Int>()
        logs.forEach { log ->
            val groups = workouts[log.workoutId]?.targetMuscleGroups.orEmpty()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            if (groups.isEmpty()) {
                counts.merge(UNKNOWN_MUSCLE_GROUP, 1, Int::plus)
            } else {
                groups.forEach { group -> counts.merge(group, 1, Int::plus) }
            }
        }
        val total = counts.values.sum()
        if (total == 0) return emptyList()
        return counts.entries
            .sortedByDescending { it.value }
            .map { (group, count) ->
                MuscleGroupShare(
                    group = group,
                    occurrences = count,
                    percentage = count * 100.0 / total
                )
            }
    }

    private fun buildMuscleVolume(
        logs: List<WorkoutLogEntity>,
        workouts: Map<String, WorkoutEntity>
    ): List<VolumeBreakdownEntry> {
        if (logs.isEmpty()) return emptyList()
        val totals = mutableMapOf<String, Double>()
        logs.forEach { log ->
            val totalVolume = log.totalVolume
            if (totalVolume <= 0.0) return@forEach
            val groups = workouts[log.workoutId]?.targetMuscleGroups.orEmpty()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            if (groups.isEmpty()) {
                totals.merge(UNKNOWN_MUSCLE_GROUP, totalVolume, Double::plus)
            } else {
                val share = totalVolume / groups.size
                groups.forEach { group -> totals.merge(group, share, Double::plus) }
            }
        }
        return totals.entries
            .sortedByDescending { it.value }
            .map { (group, volume) ->
                VolumeBreakdownEntry(label = group, volume = volume)
            }
    }

    private fun buildExerciseVolume(
        logs: List<WorkoutLogEntity>,
        data: LoadedData
    ): List<VolumeBreakdownEntry> {
        if (logs.isEmpty()) return emptyList()
        val totals = mutableMapOf<String, Double>()
        logs.forEach { log ->
            val aggregates = data.exerciseAggregatesByLog[log.id] ?: return@forEach
            aggregates.forEach { aggregate ->
                if (aggregate.totalVolume > 0.0) {
                    totals.merge(aggregate.exerciseId, aggregate.totalVolume, Double::plus)
                }
            }
        }
        if (totals.isEmpty()) return emptyList()
        return totals.entries
            .sortedByDescending { it.value }
            .map { (exerciseId, volume) ->
                val label = data.exercisesById[exerciseId]?.name ?: exerciseId
                VolumeBreakdownEntry(label = label, volume = volume)
            }
    }

    private fun buildHeatmap(
        logs: List<WorkoutLogEntity>,
        zone: ZoneId
    ): List<HeatmapDay> {
        if (logs.isEmpty()) return emptyList()
        val completed = logs
            .filter { it.status.equals("COMPLETED", ignoreCase = true) }
            .groupBy { Instant.ofEpochMilli(it.dateEpochMillis).atZone(zone).toLocalDate() }
            .mapValues { entry -> entry.value.sumOf { log -> log.totalVolume } }

        if (completed.isEmpty()) return emptyList()

        val weeksToShow = 12
        val maxDate = completed.keys.maxOrNull() ?: LocalDate.now(zone)
        val alignedEnd = maxDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val alignedStart = alignedEnd.minusWeeks(weeksToShow.toLong() - 1)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val maxVolume = completed.values.maxOrNull()?.takeIf { it > 0.0 } ?: return emptyList()

        val days = mutableListOf<HeatmapDay>()
        var cursor = alignedStart
        val totalDays = weeksToShow * 7
        repeat(totalDays) {
            val amount = completed[cursor] ?: 0.0
            val intensity = calculateIntensity(amount, maxVolume)
            days += HeatmapDay(date = cursor, amount = amount, intensity = intensity)
            cursor = cursor.plusDays(1)
        }
        return days
    }

    private fun buildVolumeSeries(
        logs: List<WorkoutLogEntity>,
        zone: ZoneId
    ): List<VolumePoint> {
        if (logs.isEmpty()) return emptyList()
        val grouped = logs.filter { it.status.equals("COMPLETED", ignoreCase = true) }
            .groupBy { log ->
                Instant.ofEpochMilli(log.dateEpochMillis).atZone(zone).toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            }
        if (grouped.isEmpty()) return emptyList()
        return grouped.entries
            .sortedBy { it.key }
            .map { (weekStart, items) ->
                VolumePoint(
                    weekStart = weekStart,
                    totalVolume = items.sumOf { it.totalVolume },
                    workouts = items.size
                )
            }
    }

    private fun buildRecordSummaries(
        period: ProgressPeriod,
        filteredLogs: List<WorkoutLogEntity>,
        data: LoadedData,
        zone: ZoneId,
        dateFormatter: DateTimeFormatter,
        locale: Locale
    ): List<RecordSummary> {
        if (data.personalRecords.isEmpty()) return emptyList()
        val today = LocalDate.now(zone)
        val periodStart = when (period) {
            ProgressPeriod.WEEK -> today.minusDays(6)
            ProgressPeriod.MONTH -> today.minusDays(29)
            ProgressPeriod.BLOCK -> filteredLogs.minOfOrNull {
                Instant.ofEpochMilli(it.dateEpochMillis).atZone(zone).toLocalDate()
            }
            ProgressPeriod.PROGRAM -> today.minusDays(30)
        }
        val orderedRecords = data.personalRecords.sortedByDescending { it.estimatedOneRm }
        return orderedRecords.map { record ->
            val recordDate = LocalDate.ofEpochDay(record.dateEpochDay)
            val label = recordDate.format(dateFormatter).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(locale) else char.toString()
            }
            val exerciseName = when {
                !record.exerciseName.isNullOrBlank() -> record.exerciseName
                else -> data.exercisesById[record.exerciseId]?.name ?: record.exerciseId
            }
            val isRecent = periodStart?.let { !recordDate.isBefore(it) } ?: false
            RecordSummary(
                exerciseId = record.exerciseId,
                exerciseName = exerciseName,
                weight = record.weight,
                weightUnit = data.weightUnit,
                reps = record.reps,
                estimatedOneRm = record.estimatedOneRm,
                lastAchieved = recordDate,
                lastAchievedLabel = label,
                isRecent = isRecent
            )
        }
    }

    private fun calculateIntensity(amount: Double, max: Double): Int {
        if (amount <= 0.0 || max <= 0.0) return 0
        val ratio = amount / max
        return when {
            ratio >= 0.75 -> 4
            ratio >= 0.5 -> 3
            ratio >= 0.25 -> 2
            else -> 1
        }
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
        val workoutsById: Map<String, WorkoutEntity>,
        val exerciseAggregatesByLog: Map<String, List<WorkoutLogExerciseAggregate>>,
        val exercisesById: Map<String, ExerciseEntity>,
        val personalRecords: List<PersonalRecordWithExercise>
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
    val recentWorkouts: List<RecentWorkoutSummary>,
    val muscleDistribution: List<MuscleGroupShare>,
    val heatmap: List<HeatmapDay>,
    val volumeSeries: List<VolumePoint>,
    val muscleVolume: List<VolumeBreakdownEntry>,
    val exerciseVolume: List<VolumeBreakdownEntry>,
    val records: List<RecordSummary>
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

data class MuscleGroupShare(
    val group: String,
    val occurrences: Int,
    val percentage: Double
)

data class HeatmapDay(
    val date: LocalDate,
    val amount: Double,
    val intensity: Int
)

data class VolumePoint(
    val weekStart: LocalDate,
    val totalVolume: Double,
    val workouts: Int
)

data class VolumeBreakdownEntry(
    val label: String,
    val volume: Double
)

data class RecordSummary(
    val exerciseId: String,
    val exerciseName: String,
    val weight: Double,
    val weightUnit: String,
    val reps: Int,
    val estimatedOneRm: Double,
    val lastAchieved: LocalDate,
    val lastAchievedLabel: String,
    val isRecent: Boolean
)
