package com.beast.app.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beast.shared.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdvancedSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedSettingsUiState())
    val uiState: StateFlow<AdvancedSettingsUiState> = _uiState

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.advancedMode().collect { advancedMode ->
                _uiState.value = _uiState.value.copy(advancedMode = advancedMode)
            }
        }
        viewModelScope.launch {
            settingsRepository.defaultReps().collect { reps ->
                _uiState.value = _uiState.value.copy(defaultReps = reps)
            }
        }
        viewModelScope.launch {
            settingsRepository.defaultSets().collect { sets ->
                _uiState.value = _uiState.value.copy(defaultSets = sets)
            }
        }
        viewModelScope.launch {
            settingsRepository.defaultRest().collect { rest ->
                _uiState.value = _uiState.value.copy(defaultRest = rest)
            }
        }
        viewModelScope.launch {
            settingsRepository.defaultWeight().collect { weight ->
                _uiState.value = _uiState.value.copy(defaultWeight = weight)
            }
        }
        viewModelScope.launch {
            settingsRepository.workoutNotesEnabled().collect { enabled ->
                _uiState.value = _uiState.value.copy(workoutNotesEnabled = enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.setNotesEnabled().collect { enabled ->
                _uiState.value = _uiState.value.copy(setNotesEnabled = enabled)
            }
        }

        // Load screen visibility settings
        val screens = listOf("Dashboard", "Programs", "Calendar", "Progress", "Profile")
        screens.forEach { screen ->
            viewModelScope.launch {
                settingsRepository.screenVisibility(screen).collect { visible ->
                    _uiState.value = _uiState.value.copy(
                        screenVisibility = _uiState.value.screenVisibility.toMutableMap().apply {
                            put(screen, visible)
                        }
                    )
                }
            }
        }
    }

    fun toggleAdvancedMode() {
        viewModelScope.launch {
            val newValue = !_uiState.value.advancedMode
            settingsRepository.setAdvancedMode(newValue)
        }
    }

    fun updateDefaultReps(reps: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultReps(reps)
        }
    }

    fun updateDefaultSets(sets: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultSets(sets)
        }
    }

    fun updateDefaultRest(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultRest(seconds)
        }
    }

    fun updateDefaultWeight(weight: Double) {
        viewModelScope.launch {
            settingsRepository.setDefaultWeight(weight)
        }
    }

    fun toggleWorkoutNotes() {
        viewModelScope.launch {
            val newValue = !_uiState.value.workoutNotesEnabled
            settingsRepository.setWorkoutNotesEnabled(newValue)
        }
    }

    fun toggleSetNotes() {
        viewModelScope.launch {
            val newValue = !_uiState.value.setNotesEnabled
            settingsRepository.setSetNotesEnabled(newValue)
        }
    }

    fun toggleScreenVisibility(screenName: String) {
        viewModelScope.launch {
            val currentValue = _uiState.value.screenVisibility[screenName] ?: true
            settingsRepository.setScreenVisibility(screenName, !currentValue)
        }
    }
}

data class AdvancedSettingsUiState(
    val advancedMode: Boolean = false,
    val defaultReps: Int = 10,
    val defaultSets: Int = 3,
    val defaultRest: Int = 60,
    val defaultWeight: Double = 0.0,
    val workoutNotesEnabled: Boolean = true,
    val setNotesEnabled: Boolean = false,
    val screenVisibility: Map<String, Boolean> = emptyMap()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdvancedSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Advanced Mode Toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.advancedMode)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔧 Advanced Mode",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Unlock full customization capabilities",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.advancedMode,
                            onCheckedChange = { viewModel.toggleAdvancedMode() }
                        )
                    }
                }
            }

            if (uiState.advancedMode) {
                // Screen Visibility Section
                item {
                    Text(
                        text = "Screen Visibility",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Choose which screens to show in navigation",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            listOf("Dashboard", "Programs", "Calendar", "Progress", "Profile").forEach { screen ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(screen)
                                    Switch(
                                        checked = uiState.screenVisibility[screen] ?: true,
                                        onCheckedChange = { viewModel.toggleScreenVisibility(screen) }
                                    )
                                }
                                if (screen != "Profile") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                // Default Workout Values Section
                item {
                    Text(
                        text = "Default Workout Values",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Default Reps
                            Text("Default Reps", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.updateDefaultReps((uiState.defaultReps - 1).coerceAtLeast(1)) }
                                ) {
                                    Text("-")
                                }
                                OutlinedTextField(
                                    value = uiState.defaultReps.toString(),
                                    onValueChange = {
                                        it.toIntOrNull()?.let { reps -> viewModel.updateDefaultReps(reps) }
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedButton(
                                    onClick = { viewModel.updateDefaultReps(uiState.defaultReps + 1) }
                                ) {
                                    Text("+")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Default Sets
                            Text("Default Sets", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.updateDefaultSets((uiState.defaultSets - 1).coerceAtLeast(1)) }
                                ) {
                                    Text("-")
                                }
                                OutlinedTextField(
                                    value = uiState.defaultSets.toString(),
                                    onValueChange = {
                                        it.toIntOrNull()?.let { sets -> viewModel.updateDefaultSets(sets) }
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedButton(
                                    onClick = { viewModel.updateDefaultSets(uiState.defaultSets + 1) }
                                ) {
                                    Text("+")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Default Rest
                            Text("Default Rest (seconds)", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.updateDefaultRest((uiState.defaultRest - 10).coerceAtLeast(0)) }
                                ) {
                                    Text("-10")
                                }
                                OutlinedTextField(
                                    value = uiState.defaultRest.toString(),
                                    onValueChange = {
                                        it.toIntOrNull()?.let { rest -> viewModel.updateDefaultRest(rest) }
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedButton(
                                    onClick = { viewModel.updateDefaultRest(uiState.defaultRest + 10) }
                                ) {
                                    Text("+10")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Default Weight
                            Text("Default Weight", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = if (uiState.defaultWeight == 0.0) "" else uiState.defaultWeight.toString(),
                                onValueChange = {
                                    it.toDoubleOrNull()?.let { weight -> viewModel.updateDefaultWeight(weight) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Enter default weight") },
                                singleLine = true
                            )
                        }
                    }
                }

                // Notes Settings Section
                item {
                    Text(
                        text = "Notes Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Workout Notes", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Add notes to entire workout sessions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.workoutNotesEnabled,
                                    onCheckedChange = { viewModel.toggleWorkoutNotes() }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Set Notes", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Add notes to individual sets",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.setNotesEnabled,
                                    onCheckedChange = { viewModel.toggleSetNotes() }
                                )
                            }
                        }
                    }
                }

                // Warning Note
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "⚠️ Note",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Advanced mode gives you full control. Be careful when modifying core settings as they may affect your workout tracking.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            } else {
                // Show message when Advanced Mode is off
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "💡 Enable Advanced Mode",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Turn on Advanced Mode above to unlock full customization options including screen visibility, default workout values, notes settings, and more.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

