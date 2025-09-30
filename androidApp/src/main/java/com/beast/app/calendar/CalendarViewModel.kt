package com.beast.app.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beast.shared.model.WorkoutLog
import com.beast.shared.repository.ProgramRepository
import com.beast.shared.repository.SettingsRepository
import com.beast.shared.repository.WorkoutDayRepository
import com.beast.shared.repository.WorkoutLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val programs: ProgramRepository,
    private val settings: SettingsRepository,
    private val daysRepo: WorkoutDayRepository,
    private val logsRepo: WorkoutLogRepository,
) : ViewModel() {

    enum class DayStatus { None, Rest, Planned, Done }

    data class DayCell(
        val date: LocalDate,
        val dayIndex: Int?,
        val title: String? = null,
        val status: DayStatus = DayStatus.None,
    )

    data class UiState(
        val month: LocalDate = LocalDate.now().withDayOfMonth(1),
        val grid: List<List<DayCell>> = emptyList(),
        val activeProgramId: String? = null,
        val selected: DayCell? = null,
        val durationDays: Int = 0,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun load() { viewModelScope.launch { rebuild(_state.value.month) } }

    fun prevMonth() { viewModelScope.launch { rebuild(_state.value.month.minusMonths(1)) } }
    fun nextMonth() { viewModelScope.launch { rebuild(_state.value.month.plusMonths(1)) } }

    fun select(cell: DayCell) { _state.value = _state.value.copy(selected = cell) }

    fun markSelectedDone() {
        val s = _state.value
        val programId = s.activeProgramId ?: return
        val idx = s.selected?.dayIndex ?: return
        viewModelScope.launch {
            val log = WorkoutLog(
                id = UUID.randomUUID().toString(),
                programId = programId,
                dayIndex = idx,
                date = System.currentTimeMillis(),
                completed = true,
                notes = null
            )
            logsRepo.upsert(log, emptyList())
            rebuild(s.month)
        }
    }

    private suspend fun rebuild(monthStart: LocalDate) {
        val activeId = settings.activeProgramId().first()
        val startMillis = settings.activeProgramStartDate().first()
        if (activeId.isNullOrBlank() || startMillis == null) {
            _state.value = UiState(month = monthStart)
            return
        }
        val program = programs.getById(activeId) ?: run {
            _state.value = UiState(month = monthStart)
            return
        }
        val startDate = LocalDate.ofEpochDay(startMillis / 86_400_000L)
        val logs = logsRepo.getLogsForProgram(activeId)
        val days = daysRepo.getByProgram(activeId)
        val titlesByIndex = days.associateBy({ it.dayIndex }, { it.title })

        // Build month grid (weeks x 7)
        val firstOfMonth = monthStart.withDayOfMonth(1)
        val firstDow = firstOfMonth.dayOfWeek.value % 7 // make Sunday = 0
        val monthLength = monthStart.lengthOfMonth()
        val totalCells = ((firstDow + monthLength + 6) / 7) * 7
        val cells = mutableListOf<DayCell>()
        for (i in 0 until totalCells) {
            val dayOfMonth = i - firstDow + 1
            if (dayOfMonth in 1..monthLength) {
                val date = firstOfMonth.withDayOfMonth(dayOfMonth)
                val diff = ChronoUnit.DAYS.between(startDate, date).toInt() + 1
                val inRange = diff in 1..program.durationDays
                val idx = if (inRange) diff else null
                val title = idx?.let { titlesByIndex[it] }
                val status = when {
                    idx == null -> DayStatus.None
                    logs.any { it.dayIndex == idx && it.completed } -> DayStatus.Done
                    title?.equals("Rest", ignoreCase = true) == true -> DayStatus.Rest
                    else -> DayStatus.Planned
                }
                cells += DayCell(date, idx, title, status)
            } else {
                val date = firstOfMonth.withDayOfMonth(1).plusDays((i - firstDow).toLong())
                cells += DayCell(date, null, null, DayStatus.None)
            }
        }
        val grid = cells.chunked(7)
        _state.value = UiState(month = monthStart, grid = grid, activeProgramId = activeId, durationDays = program.durationDays)
    }
}

