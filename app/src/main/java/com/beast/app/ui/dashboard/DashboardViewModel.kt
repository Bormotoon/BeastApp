package com.beast.app.ui.dashboard

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beast.app.R
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.db.UserProfileEntity
import com.beast.app.data.repo.ProfileRepository
import com.beast.app.data.repo.ProgramRepository
import com.beast.app.data.repo.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DatabaseProvider.get(application.applicationContext)
    private val profileRepository = ProfileRepository(database)
    private val programRepository = ProgramRepository(database)
    private val workoutRepository = WorkoutRepository(database)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { loadState() }
    }

    private suspend fun loadState() {
        val locale = Locale.getDefault()
        val today = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)
        val formattedDate = today.format(dateFormatter).replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString()
        }

        val profile = profileRepository.getProfile()
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val programName = profile?.currentProgramId ?: prefs.getString("current_program_name", null)
        val summary = if (!programName.isNullOrBlank()) {
            programRepository.getProgramSummary(programName)
        } else {
            null
        }
        val todayContext = computeTodayContext(profile, summary, today)

        val profileName = profile?.name?.takeIf { it.isNotBlank() }
        val initials = profileName?.parseInitials()
        val title = app.getString(R.string.app_name)

        val progressCard = buildProgressCard(todayContext)
        val todayWorkout = buildTodayWorkoutCard(todayContext)

        _uiState.value = DashboardUiState(
            isLoading = false,
            topBar = TopBarState(
                title = title,
                subtitle = formattedDate,
                profileName = profileName,
                profileInitials = initials
            ),
            progressCard = progressCard,
            todayWorkout = todayWorkout
        )
    }

    private fun String.parseInitials(): String {
        val parts = trim().split(' ').filter { it.isNotBlank() }
        if (parts.isEmpty()) return first().uppercase()
        return parts.take(2).joinToString(separator = "") { it.first().uppercase() }
    }

    private fun computeTodayContext(
        profile: UserProfileEntity?,
        summary: ProgramRepository.ProgramSummary?,
        today: LocalDate
    ): TodayContext? {
        if (profile == null || summary == null) return null
        if (profile.startDateEpochDay <= 0L) return null

        val totalDays = summary.program.durationDays
        if (totalDays <= 0) return null

        val startDate = LocalDate.ofEpochDay(profile.startDateEpochDay)
        val rawDayNumber = (today.toEpochDay() - profile.startDateEpochDay).toInt() + 1
        val clampedDay = rawDayNumber.coerceIn(1, totalDays)

        val orderedSchedule = summary.schedule.sortedBy { it.dayNumber }
        val scheduleEntry = orderedSchedule.firstOrNull { it.dayNumber == clampedDay }
        val workoutId = scheduleEntry?.workoutId
        val phaseName = workoutId?.let { summary.phaseByWorkout[it] }
            ?: summary.phases.firstOrNull()?.name

        val phaseWeek = if (!phaseName.isNullOrBlank()) {
            val phaseDays = orderedSchedule.filter { summary.phaseByWorkout[it.workoutId] == phaseName }
            val indexInPhase = phaseDays.indexOfFirst { it.dayNumber == clampedDay }
            if (indexInPhase >= 0) indexInPhase / 7 + 1 else (clampedDay - 1) / 7 + 1
        } else {
            null
        }

        return TodayContext(
            programName = summary.program.name,
            totalDays = totalDays,
            dayNumber = clampedDay,
            rawDayNumber = rawDayNumber,
            workoutId = workoutId,
            phaseName = phaseName,
            phaseWeek = phaseWeek,
            today = today,
            startDate = startDate
        )
    }

    private fun buildProgressCard(context: TodayContext?): ProgressCardState {
        if (context == null) return ProgressCardState()

        val displayDay = when {
            context.rawDayNumber < 1 -> 1
            context.rawDayNumber > context.totalDays -> context.totalDays
            else -> context.dayNumber
        }

        val progressFraction = when {
            context.rawDayNumber <= 0 -> 0f
            context.rawDayNumber >= context.totalDays -> 1f
            else -> (context.dayNumber.toFloat() / context.totalDays.toFloat()).coerceIn(0f, 1f)
        }

        return ProgressCardState(
            isVisible = true,
            programName = context.programName,
            dayNumber = displayDay,
            totalDays = context.totalDays,
            progressFraction = progressFraction,
            currentPhaseName = context.phaseName,
            currentPhaseWeek = context.phaseWeek
        )
    }

    private suspend fun buildTodayWorkoutCard(context: TodayContext?): TodayWorkoutCardState {
        if (context == null) return TodayWorkoutCardState()

        if (context.isBeforeStart) {
            val daysUntilStart = (context.startDate.toEpochDay() - context.today.toEpochDay()).toInt()
            val statusText = when {
                daysUntilStart > 1 -> "Старт через $daysUntilStart дней"
                daysUntilStart == 1 -> "Старт завтра"
                else -> "Старт сегодня"
            }
            val dateText = context.startDate.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
            return TodayWorkoutCardState(
                isVisible = true,
                title = context.programName,
                subtitle = "Старт программы $dateText",
                description = "Подготовьтесь к программе и настройте расписание.",
                statusText = statusText,
                statusType = WorkoutStatus.UPCOMING
            )
        }

        if (context.isAfterFinish) {
            return TodayWorkoutCardState(
                isVisible = true,
                title = context.programName,
                subtitle = "Программа завершена",
                description = "Вы прошли ${context.totalDays} дней программы. Отличная работа!",
                statusText = "Программа завершена",
                statusType = WorkoutStatus.FINISHED
            )
        }

        if (context.isRestDay) {
            val subtitleParts = mutableListOf<String>()
            subtitleParts += "День ${context.dayNumber} из ${context.totalDays}"
            if (!context.phaseName.isNullOrBlank()) subtitleParts += "Фаза: ${context.phaseName}"
            context.phaseWeek?.let { subtitleParts += "Неделя $it" }

            return TodayWorkoutCardState(
                isVisible = true,
                title = "День отдыха",
                subtitle = subtitleParts.joinToString(" · "),
                description = "Сегодня запланирован отдых. Используйте время для восстановления.",
                statusText = "Плановый отдых",
                statusType = WorkoutStatus.REST
            )
        }

        val workoutId = context.workoutId ?: return TodayWorkoutCardState()
        val workout = workoutRepository.getWorkoutWithExercises(workoutId) ?: return TodayWorkoutCardState(
            isVisible = true,
            title = "Тренировка недоступна",
            subtitle = "Не удалось загрузить данные тренировки",
            statusText = "Недоступно",
            statusType = WorkoutStatus.REST
        )

        val subtitleParts = mutableListOf<String>()
        subtitleParts += "День ${context.dayNumber} из ${context.totalDays}"
        if (!context.phaseName.isNullOrBlank()) subtitleParts += "Фаза: ${context.phaseName}"
        context.phaseWeek?.let { subtitleParts += "Неделя $it" }

        val muscleGroups = workout.workout.targetMuscleGroups

        return TodayWorkoutCardState(
            isVisible = true,
            title = workout.workout.name,
            subtitle = subtitleParts.joinToString(" · "),
            description = "",
            durationMinutes = workout.workout.durationMinutes,
            exerciseCount = workout.mappings.size,
            muscleGroups = muscleGroups,
            statusText = "Запланирована",
            statusType = WorkoutStatus.SCHEDULED,
            startButtonEnabled = true,
            viewDetailsEnabled = true,
            workoutId = workout.workout.id
        )
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val topBar: TopBarState = TopBarState(),
    val progressCard: ProgressCardState = ProgressCardState(),
    val todayWorkout: TodayWorkoutCardState = TodayWorkoutCardState()
)

data class TopBarState(
    val title: String = "",
    val subtitle: String = "",
    val profileName: String? = null,
    val profileInitials: String? = null
)

data class ProgressCardState(
    val isVisible: Boolean = false,
    val programName: String = "",
    val dayNumber: Int = 0,
    val totalDays: Int = 0,
    val progressFraction: Float = 0f,
    val currentPhaseName: String? = null,
    val currentPhaseWeek: Int? = null
)

data class TodayWorkoutCardState(
    val isVisible: Boolean = false,
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    val durationMinutes: Int = 0,
    val exerciseCount: Int = 0,
    val muscleGroups: List<String> = emptyList(),
    val statusText: String = "",
    val statusType: WorkoutStatus = WorkoutStatus.SCHEDULED,
    val startButtonEnabled: Boolean = false,
    val viewDetailsEnabled: Boolean = false,
    val workoutId: String? = null
)

enum class WorkoutStatus {
    UPCOMING,
    REST,
    SCHEDULED,
    COMPLETED,
    FINISHED
}

private data class TodayContext(
    val programName: String,
    val totalDays: Int,
    val dayNumber: Int,
    val rawDayNumber: Int,
    val workoutId: String?,
    val phaseName: String?,
    val phaseWeek: Int?,
    val today: LocalDate,
    val startDate: LocalDate
) {
    val isBeforeStart: Boolean get() = rawDayNumber < 1
    val isAfterFinish: Boolean get() = rawDayNumber > totalDays
    val isRestDay: Boolean get() = workoutId.isNullOrBlank()
}
