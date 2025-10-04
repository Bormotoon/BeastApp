package com.beast.app.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beast.shared.model.Program
import com.beast.shared.model.Units
import com.beast.shared.repository.ProgramRepository
import com.beast.shared.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadPrograms()
    }

    private fun loadPrograms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val programs = programRepository.getAll()
                _uiState.value = _uiState.value.copy(
                    programs = programs,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun selectUnits(units: UnitsPreference) {
        _uiState.value = _uiState.value.copy(selectedUnits = units)
    }

    fun selectProgram(program: Program) {
        _uiState.value = _uiState.value.copy(selectedProgram = program)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val state = _uiState.value
            state.selectedUnits?.let { userPrefsRepository.setUnitsPreference(it) }
            state.selectedProgram?.let { userPrefsRepository.setSelectedProgram(it.id) }
            userPrefsRepository.setOnboardingCompleted(true)
            _uiState.value = _uiState.value.copy(onboardingCompleted = true)
        }
    }

    fun nextStep() {
        _uiState.value = _uiState.value.copy(
            currentStep = _uiState.value.currentStep + 1
        )
    }

    fun previousStep() {
        _uiState.value = _uiState.value.copy(
            currentStep = maxOf(0, _uiState.value.currentStep - 1)
        )
    }
}

data class OnboardingUiState(
    val currentStep: Int = 0,
    val programs: List<Program> = emptyList(),
    val selectedUnits: UnitsPreference? = null,
    val selectedProgram: Program? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val onboardingCompleted: Boolean = false
)

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.onboardingCompleted) {
        if (uiState.onboardingCompleted) {
            onOnboardingComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to Beast App") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (uiState.currentStep + 1) / 3f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState.currentStep) {
                0 -> WelcomeStep(
                    onNext = { viewModel.nextStep() }
                )
                1 -> UnitsSelectionStep(
                    selectedUnits = uiState.selectedUnits,
                    onSelectUnits = { viewModel.selectUnits(it) },
                    onNext = { viewModel.nextStep() },
                    onBack = { viewModel.previousStep() }
                )
                2 -> ProgramSelectionStep(
                    programs = uiState.programs,
                    selectedProgram = uiState.selectedProgram,
                    onSelectProgram = { viewModel.selectProgram(it) },
                    onComplete = { viewModel.completeOnboarding() },
                    onBack = { viewModel.previousStep() },
                    isLoading = uiState.isLoading
                )
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Beast App",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your ultimate workout tracker for structured programs like Body Beast.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Let's get you set up!",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}

@Composable
fun UnitsSelectionStep(
    selectedUnits: UnitsPreference?,
    onSelectUnits: (UnitsPreference) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Choose Your Units",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "How would you like to track your weights?",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            UnitsPreference.entries.forEach { units ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .selectable(
                            selected = selectedUnits == units,
                            onClick = { onSelectUnits(units) }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedUnits == units)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedUnits == units,
                            onClick = { onSelectUnits(units) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = when (units) {
                                    Units.KG -> "Kilograms (kg)"
                                    Units.LBS -> "Pounds (lbs)"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = when (units) {
                                    Units.KG -> "Used in most countries"
                                    Units.LBS -> "Used in the US"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                enabled = selectedUnits != null
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
fun ProgramSelectionStep(
    programs: List<Program>,
    selectedProgram: Program?,
    onSelectProgram: (Program) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Choose Your Program",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Select a workout program to start with. You can change it later.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(programs) { program ->
                        ProgramCard(
                            program = program,
                            isSelected = selectedProgram == program,
                            onSelect = { onSelectProgram(program) }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f),
                enabled = selectedProgram != null
            ) {
                Text("Complete")
            }
        }
    }
}

@Composable
fun ProgramCard(
    program: Program,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = program.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (program.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = program.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${program.phases.size} phases • ${program.totalDays} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
package com.beast.app.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beast.shared.model.Program
import com.beast.shared.model.Units
import com.beast.shared.repository.ProgramRepository
import com.beast.shared.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadPrograms()
    }

    private fun loadPrograms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val programs = programRepository.getAll()
                _uiState.value = _uiState.value.copy(
                    programs = programs,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun selectUnits(units: UnitsPreference) {
        _uiState.value = _uiState.value.copy(selectedUnits = units)
    }

    fun selectProgram(program: Program) {
        _uiState.value = _uiState.value.copy(selectedProgram = program)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val state = _uiState.value
            state.selectedUnits?.let { userPrefsRepository.setUnitsPreference(it) }
            state.selectedProgram?.let { userPrefsRepository.setSelectedProgram(it.id) }
            userPrefsRepository.setOnboardingCompleted(true)
            _uiState.value = _uiState.value.copy(onboardingCompleted = true)
        }
    }

    fun nextStep() {
        _uiState.value = _uiState.value.copy(
            currentStep = _uiState.value.currentStep + 1
        )
    }

    fun previousStep() {
        _uiState.value = _uiState.value.copy(
            currentStep = maxOf(0, _uiState.value.currentStep - 1)
        )
    }
}

data class OnboardingUiState(
    val currentStep: Int = 0,
    val programs: List<Program> = emptyList(),
    val selectedUnits: UnitsPreference? = null,
    val selectedProgram: Program? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val onboardingCompleted: Boolean = false
)

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.onboardingCompleted) {
        if (uiState.onboardingCompleted) {
            onOnboardingComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to Beast App") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (uiState.currentStep + 1) / 3f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState.currentStep) {
                0 -> WelcomeStep(
                    onNext = { viewModel.nextStep() }
                )
                1 -> UnitsSelectionStep(
                    selectedUnits = uiState.selectedUnits,
                    onSelectUnits = { viewModel.selectUnits(it) },
                    onNext = { viewModel.nextStep() },
                    onBack = { viewModel.previousStep() }
                )
                2 -> ProgramSelectionStep(
                    programs = uiState.programs,
                    selectedProgram = uiState.selectedProgram,
                    onSelectProgram = { viewModel.selectProgram(it) },
                    onComplete = { viewModel.completeOnboarding() },
                    onBack = { viewModel.previousStep() },
                    isLoading = uiState.isLoading
                )
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Beast App",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your ultimate workout tracker for structured programs like Body Beast.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Let's get you set up!",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}

@Composable
fun UnitsSelectionStep(
    selectedUnits: Units?,
    onSelectUnits: (Units) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Choose Your Units",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "How would you like to track your weights?",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            Units.entries.forEach { units ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .selectable(
                            selected = selectedUnits == units,
                            onClick = { onSelectUnits(units) }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedUnits == units)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedUnits == units,
                            onClick = { onSelectUnits(units) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = when (units) {
                                    UnitsPreference.METRIC -> "Kilograms (kg)"
                                    UnitsPreference.IMPERIAL -> "Pounds (lbs)"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = when (units) {
                                    UnitsPreference.METRIC -> "Used in most countries"
                                    UnitsPreference.IMPERIAL -> "Used in the US"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                enabled = selectedUnits != null
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
fun ProgramSelectionStep(
    programs: List<Program>,
    selectedProgram: Program?,
    onSelectProgram: (Program) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Choose Your Program",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Select a workout program to start with. You can change it later.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(programs) { program ->
                        ProgramCard(
                            program = program,
                            isSelected = selectedProgram == program,
                            onSelect = { onSelectProgram(program) }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f),
                enabled = selectedProgram != null
            ) {
                Text("Complete")
            }
        }
    }
}

@Composable
fun ProgramCard(
    program: Program,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = program.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (program.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = program.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${program.phases.size} phases • ${program.totalDays} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
package com.beast.app.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beast.shared.model.Program
import com.beast.shared.model.Units
import com.beast.shared.repository.ProgramRepository
import com.beast.shared.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadPrograms()
    }

    private fun loadPrograms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val programs = programRepository.getAll()
                _uiState.value = _uiState.value.copy(
                    programs = programs,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun selectUnits(units: UnitsPreference) {
        _uiState.value = _uiState.value.copy(selectedUnits = units)
    }

    fun selectProgram(program: Program) {
        _uiState.value = _uiState.value.copy(selectedProgram = program)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val state = _uiState.value
            state.selectedUnits?.let { userPrefsRepository.setUnitsPreference(it) }
            state.selectedProgram?.let { userPrefsRepository.setSelectedProgram(it.id) }
            userPrefsRepository.setOnboardingCompleted(true)
            _uiState.value = _uiState.value.copy(onboardingCompleted = true)
        }
    }

    fun nextStep() {
        _uiState.value = _uiState.value.copy(
            currentStep = _uiState.value.currentStep + 1
        )
    }

    fun previousStep() {
        _uiState.value = _uiState.value.copy(
            currentStep = maxOf(0, _uiState.value.currentStep - 1)
        )
    }
}

data class OnboardingUiState(
    val currentStep: Int = 0,
    val programs: List<Program> = emptyList(),
    val selectedUnits: Units? = null,
    val selectedProgram: Program? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val onboardingCompleted: Boolean = false
)

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.onboardingCompleted) {
        if (uiState.onboardingCompleted) {
            onOnboardingComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to Beast App") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (uiState.currentStep + 1) / 3f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState.currentStep) {
                0 -> WelcomeStep(
                    onNext = { viewModel.nextStep() }
                )
                1 -> UnitsSelectionStep(
                    selectedUnits = uiState.selectedUnits,
                    onSelectUnits = { viewModel.selectUnits(it) },
                    onNext = { viewModel.nextStep() },
                    onBack = { viewModel.previousStep() }
                )
                2 -> ProgramSelectionStep(
                    programs = uiState.programs,
                    selectedProgram = uiState.selectedProgram,
                    onSelectProgram = { viewModel.selectProgram(it) },
                    onComplete = { viewModel.completeOnboarding() },
                    onBack = { viewModel.previousStep() },
                    isLoading = uiState.isLoading
                )
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Beast App",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your ultimate workout tracker for structured programs like Body Beast.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Let's get you set up!",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}

@Composable
fun UnitsSelectionStep(
    selectedUnits: UnitsPreference?,
    onSelectUnits: (UnitsPreference) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Choose Your Units",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "How would you like to track your weights?",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            UnitsPreference.entries.forEach { units ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .selectable(
                            selected = selectedUnits == units,
                            onClick = { onSelectUnits(units) }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedUnits == units)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedUnits == units,
                            onClick = { onSelectUnits(units) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = when (units) {
                                    UnitsPreference.METRIC -> "Kilograms (kg)"
                                    UnitsPreference.IMPERIAL -> "Pounds (lbs)"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = when (units) {
                                    UnitsPreference.METRIC -> "Used in most countries"
                                    UnitsPreference.IMPERIAL -> "Used in the US"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                enabled = selectedUnits != null
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
fun ProgramSelectionStep(
    programs: List<Program>,
    selectedProgram: Program?,
    onSelectProgram: (Program) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Choose Your Program",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Select a workout program to start with. You can change it later.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(programs) { program ->
                        ProgramCard(
                            program = program,
                            isSelected = selectedProgram == program,
                            onSelect = { onSelectProgram(program) }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f),
                enabled = selectedProgram != null
            ) {
                Text("Complete")
            }
        }
    }
}

@Composable
fun ProgramCard(
    program: Program,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = program.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (program.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = program.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${program.phases.size} phases • ${program.totalDays} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
package com.beast.app.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beast.shared.model.Program
import com.beast.shared.model.Units
import com.beast.shared.repository.ProgramRepository
import com.beast.shared.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadPrograms()
    }

    private fun loadPrograms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val programs = programRepository.getAll()
                _uiState.value = _uiState.value.copy(
                    programs = programs,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun selectUnits(units: UnitsPreference) {
        _uiState.value = _uiState.value.copy(selectedUnits = units)
    }

    fun selectProgram(program: Program) {
        _uiState.value = _uiState.value.copy(selectedProgram = program)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val state = _uiState.value
            state.selectedUnits?.let { settingsRepository.setUnits(it) }
            state.selectedProgram?.let {
                settingsRepository.setActiveProgramId(it.id)
                // Set start date to today at midnight
                val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000) * (24 * 60 * 60 * 1000)
                settingsRepository.setActiveProgramStartDate(today)
            }
            settingsRepository.setOnboardingCompleted(true)
            _uiState.value = _uiState.value.copy(onboardingCompleted = true)
        }
    }

    fun nextStep() {
        _uiState.value = _uiState.value.copy(
            currentStep = _uiState.value.currentStep + 1
        )
    }

    fun previousStep() {
        _uiState.value = _uiState.value.copy(
            currentStep = maxOf(0, _uiState.value.currentStep - 1)
        )
    }
}

data class OnboardingUiState(
    val currentStep: Int = 0,
    val programs: List<Program> = emptyList(),
    val selectedUnits: UnitsPreference? = null,
    val selectedProgram: Program? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val onboardingCompleted: Boolean = false
)

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.onboardingCompleted) {
        if (uiState.onboardingCompleted) {
            onOnboardingComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to Beast App") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (uiState.currentStep + 1) / 3f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState.currentStep) {
                0 -> WelcomeStep(
                    onNext = { viewModel.nextStep() }
                )
                1 -> UnitsSelectionStep(
                    selectedUnits = uiState.selectedUnits,
                    onSelectUnits = { viewModel.selectUnits(it) },
                    onNext = { viewModel.nextStep() },
                    onBack = { viewModel.previousStep() }
                )
                2 -> ProgramSelectionStep(
                    programs = uiState.programs,
                    selectedProgram = uiState.selectedProgram,
                    onSelectProgram = { viewModel.selectProgram(it) },
                    onComplete = { viewModel.completeOnboarding() },
                    onBack = { viewModel.previousStep() },
                    isLoading = uiState.isLoading
                )
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Beast App",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your ultimate workout tracker for structured programs like Body Beast.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Let's get you set up!",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}

@Composable
fun UnitsSelectionStep(
    selectedUnits: UnitsPreference?,
    onSelectUnits: (UnitsPreference) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Choose Your Units",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "How would you like to track your weights?",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            UnitsPreference.entries.forEach { units ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .selectable(
                            selected = selectedUnits == units,
                            onClick = { onSelectUnits(units) }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedUnits == units)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedUnits == units,
                            onClick = { onSelectUnits(units) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = when (units) {
                                    UnitsPreference.METRIC -> "Kilograms (kg)"
                                    UnitsPreference.IMPERIAL -> "Pounds (lbs)"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = when (units) {
                                    UnitsPreference.METRIC -> "Used in most countries"
                                    UnitsPreference.IMPERIAL -> "Used in the US"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                enabled = selectedUnits != null
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
fun ProgramSelectionStep(
    programs: List<Program>,
    selectedProgram: Program?,
    onSelectProgram: (Program) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Choose Your Program",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Select a workout program to start with. You can change it later.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(programs) { program ->
                        ProgramCard(
                            program = program,
                            isSelected = selectedProgram == program,
                            onSelect = { onSelectProgram(program) }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f),
                enabled = selectedProgram != null
            ) {
                Text("Complete")
            }
        }
    }
}

@Composable
fun ProgramCard(
    program: Program,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = program.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (program.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = program.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${program.phases.size} phases • ${program.totalDays} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
package com.beast.app.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beast.shared.model.Program
import com.beast.shared.model.Units
import com.beast.shared.repository.ProgramRepository
import com.beast.shared.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadPrograms()
    }

    private fun loadPrograms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val programs = programRepository.getAll()
                _uiState.value = _uiState.value.copy(
                    programs = programs,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun selectUnits(units: Units) {
        _uiState.value = _uiState.value.copy(selectedUnits = units)
    }

    fun selectProgram(program: Program) {
        _uiState.value = _uiState.value.copy(selectedProgram = program)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val state = _uiState.value
            state.selectedUnits?.let { userPrefsRepository.setUnitsPreference(it) }
            state.selectedProgram?.let { userPrefsRepository.setSelectedProgram(it.id) }
            userPrefsRepository.setOnboardingCompleted(true)
            _uiState.value = _uiState.value.copy(onboardingCompleted = true)
        }
    }

    fun nextStep() {
        _uiState.value = _uiState.value.copy(
            currentStep = _uiState.value.currentStep + 1
        )
    }

    fun previousStep() {
        _uiState.value = _uiState.value.copy(
            currentStep = maxOf(0, _uiState.value.currentStep - 1)
        )
    }
}

data class OnboardingUiState(
    val currentStep: Int = 0,
    val programs: List<Program> = emptyList(),
    val selectedUnits: UnitsPreference? = null,
    val selectedProgram: Program? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val onboardingCompleted: Boolean = false
)

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.onboardingCompleted) {
        if (uiState.onboardingCompleted) {
            onOnboardingComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to Beast App") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (uiState.currentStep + 1) / 3f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState.currentStep) {
                0 -> WelcomeStep(
                    onNext = { viewModel.nextStep() }
                )
                1 -> UnitsSelectionStep(
                    selectedUnits = uiState.selectedUnits,
                    onSelectUnits = { viewModel.selectUnits(it) },
                    onNext = { viewModel.nextStep() },
                    onBack = { viewModel.previousStep() }
                )
                2 -> ProgramSelectionStep(
                    programs = uiState.programs,
                    selectedProgram = uiState.selectedProgram,
                    onSelectProgram = { viewModel.selectProgram(it) },
                    onComplete = { viewModel.completeOnboarding() },
                    onBack = { viewModel.previousStep() },
                    isLoading = uiState.isLoading
                )
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Beast App",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your ultimate workout tracker for structured programs like Body Beast.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Let's get you set up!",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}

@Composable
fun UnitsSelectionStep(
    selectedUnits: UnitsPreference?,
    onSelectUnits: (UnitsPreference) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Choose Your Units",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "How would you like to track your weights?",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            UnitsPreference.entries.forEach { units ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .selectable(
                            selected = selectedUnits == units,
                            onClick = { onSelectUnits(units) }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedUnits == units)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedUnits == units,
                            onClick = { onSelectUnits(units) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = when (units) {
                                    UnitsPreference.METRIC -> "Kilograms (kg)"
                                    UnitsPreference.IMPERIAL -> "Pounds (lbs)"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = when (units) {
                                    UnitsPreference.METRIC -> "Used in most countries"
                                    UnitsPreference.IMPERIAL -> "Used in the US"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                enabled = selectedUnits != null
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
fun ProgramSelectionStep(
    programs: List<Program>,
    selectedProgram: Program?,
    onSelectProgram: (Program) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Choose Your Program",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Select a workout program to start with. You can change it later.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(programs) { program ->
                        ProgramCard(
                            program = program,
                            isSelected = selectedProgram == program,
                            onSelect = { onSelectProgram(program) }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f),
                enabled = selectedProgram != null
            ) {
                Text("Complete")
            }
        }
    }
}

@Composable
fun ProgramCard(
    program: Program,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = program.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (program.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = program.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${program.phases.size} phases • ${program.totalDays} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

