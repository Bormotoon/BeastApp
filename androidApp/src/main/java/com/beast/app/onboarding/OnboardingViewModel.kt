package com.beast.app.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beast.shared.model.Program
import com.beast.shared.model.WorkoutDay
import com.beast.shared.model.Units
import com.beast.shared.repository.ProgramRepository
import com.beast.shared.repository.SettingsRepository
import com.beast.shared.repository.WorkoutDayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val programs: ProgramRepository,
    private val days: WorkoutDayRepository,
) : ViewModel() {

    private val _programs = MutableStateFlow<List<Program>>(emptyList())
    val programsState: StateFlow<List<Program>> = _programs

    fun load() {
        viewModelScope.launch {
            var list = programs.getAll()
            if (list.isEmpty()) {
                val prog = Program(
                    id = "bodybeast-huge",
                    title = "Body Beast: Huge",
                    description = "90-day mass-building program by Sagi Kalev",
                    durationDays = 90,
                    difficulty = "Intermediate/Advanced",
                    author = "Sagi Kalev"
                )
                programs.upsert(prog)
                val week = listOf(
                    "Build: Chest/Triceps",
                    "Build: Back/Biceps",
                    "Build: Legs",
                    "Beast: Cardio",
                    "Build: Shoulders",
                    "Rest",
                    "Total Body"
                )
                (1..90).forEach { idx ->
                    val title = week[(idx - 1) % week.size]
                    days.upsert(
                        WorkoutDay(
                            id = "${prog.id}-day-$idx",
                            programId = prog.id,
                            dayIndex = idx,
                            title = title,
                        )
                    )
                }
                list = programs.getAll()
            }
            _programs.value = list
        }
    }

    fun setUnits(units: Units) {
        viewModelScope.launch { settings.setUnits(units) }
    }

    fun completeOnboarding(units: Units, selectedProgramId: String?) {
        viewModelScope.launch {
            settings.setUnits(units)
            settings.setActiveProgramId(selectedProgramId)
            if (selectedProgramId != null) {
                val startOfDay = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                settings.setActiveProgramStartDate(startOfDay)
            }
            settings.setOnboardingCompleted(true)
        }
    }
}
