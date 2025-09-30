package com.beast.app.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beast.shared.model.Program
import com.beast.shared.model.WorkoutLog
import com.beast.shared.repository.ProgramRepository
import com.beast.shared.repository.SettingsRepository
import com.beast.shared.repository.WorkoutDayRepository
import com.beast.shared.repository.WorkoutLogRepository
import com.beast.shared.usecase.CalculateProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val programRepo: ProgramRepository,
    private val dayRepo: WorkoutDayRepository,
    private val logsRepo: WorkoutLogRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    data class UiState(
        val program: Program? = null,
        val progressPercent: Int = 0,
        val completedDays: Int = 0,
        val totalDays: Int = 0,
        val nextDayIndex: Int? = null,
        val nextDayTitle: String? = null,
        val recentLogs: List<WorkoutLog> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            settings.activeProgramId().collectLatest { id ->
                refresh(id)
            }
        }
    }

    fun load() {
        viewModelScope.launch { refresh(settings.activeProgramId().first()) }
    }

    private suspend fun refresh(activeId: String?) {
        if (activeId.isNullOrBlank()) {
            _state.value = UiState(); return
        }
        val program = programRepo.getById(activeId)
        if (program == null) { _state.value = UiState(); return }
        val calc = CalculateProgressUseCase(logsRepo)
        val res = calc(program.id, program.durationDays)
        val nextIndex = (res.completedDays + 1).takeIf { it <= program.durationDays }
        var nextTitle: String? = null
        if (nextIndex != null) {
            runCatching { dayRepo.getByProgram(program.id) }
                .onSuccess { days -> nextTitle = days.firstOrNull { it.dayIndex == nextIndex }?.title }
        }
        val recent = logsRepo.getLogsForProgram(program.id).sortedByDescending { it.date }.take(5)
        _state.value = UiState(
            program = program,
            progressPercent = res.percent,
            completedDays = res.completedDays,
            totalDays = res.totalDays,
            nextDayIndex = nextIndex,
            nextDayTitle = nextTitle,
            recentLogs = recent,
        )
    }

    fun markNextDayDone() {
        val s = _state.value
        val program = s.program ?: return
        val next = s.nextDayIndex ?: return
        viewModelScope.launch {
            val log = WorkoutLog(
                id = UUID.randomUUID().toString(),
                programId = program.id,
                dayIndex = next,
                date = System.currentTimeMillis(),
                completed = true,
                notes = null
            )
            logsRepo.upsert(log, emptyList())
            refresh(program.id)
        }
    }
}
