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
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)
        val today = LocalDate.now()
        val formattedDate = today.format(dateFormatter).replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString()
        }

        val profile = profileRepository.getProfile()
        val progressCard = buildProgressCard(profile)
        val profileName = profile?.name?.takeIf { it.isNotBlank() }
        val initials = profileName?.parseInitials()
        val title = getApplication<Application>().getString(R.string.app_name)

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            topBar = TopBarState(
                title = title,
                subtitle = formattedDate,
                profileName = profileName,
                profileInitials = initials
            ),
            progressCard = progressCard
        )
    }

    private fun String.parseInitials(): String {
        val parts = trim().split(' ').filter { it.isNotBlank() }
        if (parts.isEmpty()) return first().uppercase()
        return parts.take(2).joinToString(separator = "") { it.first().uppercase() }
    }

    private suspend fun buildProgressCard(profile: UserProfileEntity?): ProgressCardState {
        if (profile == null || profile.startDateEpochDay <= 0L) return ProgressCardState()

        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val programName = profile.currentProgramId ?: prefs.getString("current_program_name", null)
        if (programName.isNullOrBlank()) return ProgressCardState()

        val summary = programRepository.getProgramSummary(programName) ?: return ProgressCardState()
        val totalDays = summary.program.durationDays
        if (totalDays <= 0) return ProgressCardState()

        val today = LocalDate.now()
        val dayNumberRaw = (today.toEpochDay() - profile.startDateEpochDay).toInt() + 1
        val clampedDay = dayNumberRaw.coerceIn(1, totalDays)
        val progressFraction = (clampedDay.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)

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

        return ProgressCardState(
            isVisible = true,
            programName = summary.program.name,
            dayNumber = clampedDay,
            totalDays = totalDays,
            progressFraction = progressFraction,
            currentPhaseName = phaseName,
            currentPhaseWeek = phaseWeek
        )
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val topBar: TopBarState = TopBarState(),
    val progressCard: ProgressCardState = ProgressCardState()
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
