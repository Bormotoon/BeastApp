package com.beast.app.programs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beast.shared.repository.ExerciseRepository
import com.beast.shared.repository.ProgramRepository
import com.beast.shared.repository.WorkoutDayRepository
import com.beast.shared.usecase.ImportProgramUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportProgramViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val workoutDayRepository: WorkoutDayRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportProgramUiState())
    val uiState: StateFlow<ImportProgramUiState> = _uiState

    fun importProgram(uri: Uri, contentResolver: android.content.ContentResolver) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImporting = true,
                importResult = null
            )

            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importResult = ImportProgramUseCase.ImportResult(
                            success = false,
                            programId = null,
                            programName = null,
                            workoutDaysCount = 0,
                            errors = listOf("Failed to open file"),
                            warnings = emptyList()
                        )
                    )
                    return@launch
                }

                val useCase = ImportProgramUseCase(
                    programRepository,
                    workoutDayRepository,
                    exerciseRepository
                )

                val result = useCase.execute(inputStream)
                inputStream.close()

                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importResult = result
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importResult = ImportProgramUseCase.ImportResult(
                        success = false,
                        programId = null,
                        programName = null,
                        workoutDaysCount = 0,
                        errors = listOf("Import failed: ${e.message}"),
                        warnings = emptyList()
                    )
                )
            }
        }
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(importResult = null)
    }
}

data class ImportProgramUiState(
    val isImporting: Boolean = false,
    val importResult: ImportProgramUseCase.ImportResult? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportProgramScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportProgramViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importProgram(it, context.contentResolver)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Program") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isImporting) {
                // Loading state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Importing program...",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else if (uiState.importResult != null) {
                // Result state
                ImportResultView(
                    result = uiState.importResult!!,
                    onDismiss = {
                        viewModel.clearResult()
                        if (uiState.importResult?.success == true) {
                            onNavigateBack()
                        }
                    }
                )
            } else {
                // Initial state
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Import Fitness Program",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }

                    item {
                        Text(
                            text = "Import a custom workout program from a CSV file. The file should contain a schedule of workouts for each day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "📋 CSV Format Requirements",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "• Required columns: day, title\n" +
                                            "• Optional: description, duration, exercises\n" +
                                            "• Exercises separated by semicolons\n" +
                                            "• Day numbers must be sequential (1, 2, 3...)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "📄 Example CSV",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "day,title,description,duration\n" +
                                            "1,Chest & Triceps,Build phase,45\n" +
                                            "2,Back & Biceps,Build phase,45\n" +
                                            "3,Shoulders,Build phase,40\n" +
                                            "...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { filePickerLauncher.launch("text/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select CSV File")
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "💡 Tips",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "• Test with a small program first (7-14 days)\n" +
                                            "• Check the full documentation for metadata options\n" +
                                            "• Exercise IDs must match existing exercises\n" +
                                            "• Maximum program length: 365 days",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportResultView(
    result: ImportProgramUseCase.ImportResult,
    onDismiss: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.success)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (result.success) "✅ Import Successful!" else "❌ Import Failed",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (result.success)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )

                    if (result.success) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result.programName ?: "Unknown Program",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${result.workoutDaysCount} workout days imported",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Errors
        if (result.errors.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚠️ Errors",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        result.errors.forEach { error ->
                            Text(
                                text = "• $error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Warnings
        if (result.warnings.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚠️ Warnings",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        result.warnings.forEach { warning ->
                            Text(
                                text = "• $warning",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (result.success) "Go to Programs" else "Try Again")
            }
        }
    }
}

