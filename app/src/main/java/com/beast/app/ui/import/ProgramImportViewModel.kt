package com.beast.app.ui.import

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beast.app.data.repo.ProgramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ImportState {
    object Idle : ImportState
    object InProgress : ImportState
    data class Success(val result: ProgramRepository.ImportResult) : ImportState
    data class Error(val message: String) : ImportState
}

class ProgramImportViewModel(
    private val repo: ProgramRepository
) : ViewModel() {
    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    fun importFromJson(json: String) {
        viewModelScope.launch {
            _state.value = ImportState.InProgress
            try {
                val res = repo.importFromJson(json)
                _state.value = ImportState.Success(res)
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

