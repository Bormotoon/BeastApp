package com.beast.app.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beast.shared.model.Program
import com.beast.shared.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgramsViewModel @Inject constructor(
    private val repo: ProgramRepository
) : ViewModel() {
    private val _programs = MutableStateFlow<List<Program>>(emptyList())
    val programs: StateFlow<List<Program>> = _programs

    fun load() {
        viewModelScope.launch {
            _programs.value = repo.getAll()
        }
    }
}

