package com.beast.app.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beast.shared.model.Program
import com.beast.shared.repository.ProgramRepository
import com.beast.shared.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgramsViewModel @Inject constructor(
    private val repo: ProgramRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _programs = MutableStateFlow<List<Program>>(emptyList())
    val programs: StateFlow<List<Program>> = _programs

    private val _activeProgramId = MutableStateFlow<String?>(null)
    val activeProgramId = _activeProgramId.asStateFlow()

    init {
        viewModelScope.launch { settings.activeProgramId().collect { _activeProgramId.value = it } }
    }

    fun load() {
        viewModelScope.launch { _programs.value = repo.getAll() }
    }

    fun select(programId: String) {
        viewModelScope.launch {
            settings.setActiveProgramId(programId)
            val startOfDay = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            settings.setActiveProgramStartDate(startOfDay)
        }
    }
}
