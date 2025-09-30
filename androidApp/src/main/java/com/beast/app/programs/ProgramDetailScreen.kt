package com.beast.app.programs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beast.shared.model.Program
import com.beast.shared.model.WorkoutDay
import com.beast.shared.repository.ProgramRepository
import com.beast.shared.repository.SettingsRepository
import com.beast.shared.repository.WorkoutDayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class ProgramDetailViewModel @Inject constructor(
    private val programs: ProgramRepository,
    private val days: WorkoutDayRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    data class UiState(
        val program: Program? = null,
        val days: List<WorkoutDay> = emptyList(),
        val isActive: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun load(programId: String) {
        viewModelScope.launch {
            val p = programs.getById(programId)
            val d = if (p != null) days.getByProgram(p.id) else emptyList()
            val activeId = settings.activeProgramId().first()
            _state.value = UiState(program = p, days = d, isActive = (activeId == programId))
        }
    }

    fun select(programId: String) { viewModelScope.launch { settings.setActiveProgramId(programId) } }
}

@Composable
fun ProgramDetailScreen(programId: String, vm: ProgramDetailViewModel = hiltViewModel()) {
    LaunchedEffect(programId) { vm.load(programId) }
    val state by vm.state.collectAsState()

    val program = state.program
    if (program == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Программа не найдена", style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(program.title, style = MaterialTheme.typography.titleLarge)
            if (!program.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(program.description, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            if (state.isActive) {
                Text("Текущая активная программа", color = MaterialTheme.colorScheme.primary)
            } else {
                Button(onClick = { vm.select(program.id) }) { Text("Выбрать как активную") }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
        item { Text("Дни", style = MaterialTheme.typography.titleMedium) }
        items(state.days) { day ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Day ${day.dayIndex}: ${day.title}")
                }
            }
        }
    }
}
