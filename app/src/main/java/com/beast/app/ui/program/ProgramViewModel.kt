package com.beast.app.ui.program

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.repo.ProfileRepository
import com.beast.app.data.repo.ProgramRepository
import com.beast.app.data.repo.WorkoutRepository
import com.beast.app.utils.DateFormatting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class ProgramViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DatabaseProvider.get(application.applicationContext)
    private val profileRepository = ProfileRepository(database)
    private val programRepository = ProgramRepository(database)
    private val workoutRepository = WorkoutRepository(database)

    private val _uiState = MutableStateFlow(ProgramUiState())
    val uiState: StateFlow<ProgramUiState> = _uiState

    init {
        viewModelScope.launch { loadData() }
    }

    fun refresh() {
        viewModelScope.launch { loadData() }
    }

    private suspend fun loadData() {
        _uiState.value = ProgramUiState(isLoading = true)

        val programs = programRepository.getAllPrograms()

        _uiState.value = ProgramUiState(
            isLoading = false,
            programs = programs
        )
    }
}

data class ProgramUiState(
    val isLoading: Boolean = false,
    val programs: List<ProgramEntity> = emptyList(),
    val errorMessage: String? = null
)

data class PhaseUiModel(
    val name: String,
    val durationWeeks: Int,
    val workoutCount: Int,
    val workouts: List<WorkoutUiModel>
)

data class WorkoutUiModel(
    val id: String,
    val name: String,
    val durationMinutes: Int,
    val exerciseCount: Int,
    val muscleGroups: List<String>,
    val lastCompleted: LocalDate?,
    val lastCompletedLabel: String?
)
