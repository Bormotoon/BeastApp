package com.beast.app.ui.calendar

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DatabaseProvider.get(application.applicationContext)
    private val profileRepository = ProfileRepository(database)
    private val programRepository = ProgramRepository(database)
    private val workoutRepository = WorkoutRepository(database)

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { loadState() }
    }

    private suspend fun loadState() {
        _uiState.update { it.copy(isLoading = true) }

        val today = LocalDate.now()
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val profile = profileRepository.getProfile()
        val programName = profile?.currentProgramId ?: prefs.getString("current_program_name", null)
        if (profile == null || programName.isNullOrBlank()) {
            _uiState.value = CalendarUiState(
                isLoading = false,
                hasActiveProgram = false,
                today = today,
                message = "Не выбрана активная программа"
            )
            return
        }

        val summary = programRepository.getProgramSummary(programName)
        if (summary == null || summary.program.durationDays <= 0) {
            _uiState.value = CalendarUiState(
                isLoading = false,
                hasActiveProgram = false,
                programName = programName,
                today = today,
                message = "Нет данных расписания для программы"
            )
            return
        }

        val startDate = LocalDate.ofEpochDay(profile.startDateEpochDay)
        val durationDays = summary.program.durationDays
        val endDate = startDate.plusDays(durationDays.toLong() - 1)
        val startMonth = YearMonth.from(startDate).minusMonths(1)
        val endMonth = YearMonth.from(endDate).plusMonths(1)
        val initialMonth = when {
            today.isBefore(startDate) -> YearMonth.from(startDate)
            today.isAfter(endDate) -> YearMonth.from(endDate)
            else -> YearMonth.from(today)
        }

        val scheduleByDay = summary.schedule.associateBy { it.dayNumber }
        val workoutIds = scheduleByDay.values.map { it.workoutId }.distinct()
        val workoutsById = workoutRepository.getWorkoutsByIds(workoutIds).associateBy { it.id }

        val zone = ZoneId.systemDefault()
        val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = endDate.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1).toEpochMilli()
        val logs = workoutRepository.getLogsBetween(startMillis, endMillis)
        val logsByDate = logs
            .sortedBy { it.dateEpochMillis }
            .associateBy { log -> Instant.ofEpochMilli(log.dateEpochMillis).atZone(zone).toLocalDate() }

        val summaries = buildMap {
            for (day in 1..durationDays) {
                val date = startDate.plusDays((day - 1).toLong())
                val scheduled = scheduleByDay[day]
                val log = logsByDate[date]
                val workout = scheduled?.let { workoutsById[it.workoutId] }
                val status = when {
                    log != null -> CalendarDayStatus.COMPLETED
                    scheduled == null -> CalendarDayStatus.REST
                    date.isEqual(today) -> CalendarDayStatus.CURRENT
                    date.isBefore(today) -> CalendarDayStatus.MISSED
                    else -> CalendarDayStatus.UPCOMING
                }
                val summaryEntry = if (scheduled != null) {
                    CalendarDaySummary(
                        date = date,
                        dayNumber = day,
                        status = status,
                        workoutId = scheduled.workoutId,
                        workoutName = workout?.name,
                        plannedDurationMinutes = workout?.durationMinutes?.takeIf { it > 0 },
                        completedDurationMinutes = log?.totalDuration,
                        totalVolume = log?.totalVolume,
                        totalReps = log?.totalReps,
                        isUnscheduled = false
                    )
                } else if (log != null) {
                    val logWorkout = workoutsById[log.workoutId]
                    CalendarDaySummary(
                        date = date,
                        dayNumber = day,
                        status = CalendarDayStatus.COMPLETED,
                        workoutId = log.workoutId,
                        workoutName = logWorkout?.name,
                        plannedDurationMinutes = logWorkout?.durationMinutes?.takeIf { it > 0 },
                        completedDurationMinutes = log.totalDuration,
                        totalVolume = log.totalVolume,
                        totalReps = log.totalReps,
                        isUnscheduled = true
                    )
                } else {
                    CalendarDaySummary(
                        date = date,
                        dayNumber = day,
                        status = CalendarDayStatus.REST,
                        workoutId = null,
                        workoutName = null,
                        plannedDurationMinutes = null,
                        completedDurationMinutes = null,
                        totalVolume = null,
                        totalReps = null,
                        isUnscheduled = false
                    )
                }
                put(date, summaryEntry)
            }
        }

        _uiState.value = CalendarUiState(
            isLoading = false,
            hasActiveProgram = true,
            programName = programName,
            today = today,
            startDate = startDate,
            endDate = endDate,
            startMonth = startMonth,
            endMonth = endMonth,
            initialMonth = initialMonth,
            daySummaries = summaries
        )
    }
}

data class CalendarUiState(
    val isLoading: Boolean = true,
    val hasActiveProgram: Boolean = false,
    val programName: String? = null,
    val today: LocalDate = LocalDate.now(),
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val startMonth: YearMonth = YearMonth.now().minusMonths(1),
    val endMonth: YearMonth = YearMonth.now().plusMonths(1),
    val initialMonth: YearMonth = YearMonth.now(),
    val daySummaries: Map<LocalDate, CalendarDaySummary> = emptyMap(),
    val message: String? = null
)

data class CalendarDaySummary(
    val date: LocalDate,
    val dayNumber: Int?,
    val status: CalendarDayStatus,
    val workoutId: String?,
    val workoutName: String?,
    val plannedDurationMinutes: Int?,
    val completedDurationMinutes: Int?,
    val totalVolume: Double?,
    val totalReps: Int?,
    val isUnscheduled: Boolean
)

enum class CalendarDayStatus {
    COMPLETED,
    MISSED,
    CURRENT,
    UPCOMING,
    REST
}
