                        "Ошибка при сохранении шаблона: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun generateTemplateContent(): String {
        return """
            |# Beast App - Шаблон импорта программы тренировок
            |# 
            |# ИНСТРУКЦИИ:
            |# - Строки, начинающиеся с #, являются комментариями и игнорируются
            |# - Первая строка данных должна содержать заголовки столбцов
            |# - Обязательные столбцы: day, title
            |# - Опциональные столбцы: description, duration, exercises, video_url, rest_day, notes
            |# 
            |# ФОРМАТ ДАННЫХ:
            |# - day: порядковый номер дня (1, 2, 3, ...)
            |# - title: название тренировки
            |# - description: описание тренировки (опционально)
            |# - duration: длительность в минутах (опционально)
            |# - exercises: упражнения через точку с запятой (опционально)
            |# - video_url: ссылка на видео (опционально)
            |# - rest_day: true/false - день отдыха (опционально)
            |# - notes: дополнительные заметки (опционально)
            |#
            |# ПРИМЕРЫ УПРАЖНЕНИЙ (через ;):
            |# bench-press;incline-db-press;cable-flyes;tricep-dips
            |#
            |# ДЛЯ ДНЕЙ ОТДЫХА:
            |# Используйте rest_day=true или просто укажите "Rest Day" в названии
            |#
            |# =====================================================
            |# НАЧАЛО ДАННЫХ - Заполните своими тренировками
            |# =====================================================
            |
            |day,title,description,duration,exercises,rest_day,notes
            |1,Chest & Triceps,Build phase - chest and triceps,45,bench-press;incline-db-press;tricep-dips,false,Фокус на технике
            |2,Back & Biceps,Build phase - back and biceps,45,deadlift;bent-over-row;barbell-curl,false,Прогрессивная нагрузка
            |3,Shoulders,Build phase - shoulders,40,military-press;lateral-raise;front-raise,false,Контролируйте вес
            |4,Legs,Build phase - legs,50,squats;leg-press;leg-curl,false,Глубокие приседания
            |5,Chest & Triceps,Build phase - chest and triceps,45,bench-press;cable-flyes;overhead-extension,false,
            |6,Back & Biceps,Build phase - back and biceps,45,pull-ups;bent-over-row;hammer-curl,false,
            |7,Rest Day,День восстановления,0,,true,Лёгкая растяжка
            |8,Chest & Back,Bulk phase - chest and back,50,bench-press;deadlift;pull-ups,false,Суперсеты
            |9,Arms,Bulk phase - biceps and triceps,40,barbell-curl;tricep-dips;hammer-curl,false,
            |10,Legs & Shoulders,Bulk phase - legs and shoulders,55,squats;military-press;lateral-raise,false,
            |
            |# =====================================================
            |# ПРОДОЛЖИТЕ ДОБАВЛЯТЬ ДНИ ПО ЭТОМУ ШАБЛОНУ
            |# Для программы на 90 дней просто продолжайте до дня 90
            |# =====================================================
            |
            |# ПОПУЛЯРНЫЕ ПРОГРАММЫ:
            |#
            |# Body Beast (90 дней):
            |# - Фаза 1: Build (недели 1-3) - базовые упражнения
            |# - Фаза 2: Bulk (недели 4-8) - увеличение объёма
            |# - Фаза 3: Beast (недели 9-13) - максимальная интенсивность
            |#
            |# P90X (90 дней):
            |# - Фаза 1 (недели 1-3) - базовые тренировки
            |# - Неделя восстановления (неделя 4)
            |# - Фаза 2 (недели 5-7) - усложнение
            |# - Неделя восстановления (неделя 8)
            |# - Фаза 3 (недели 9-13) - максимум
            |#
            |# СПИСОК ПОПУЛЯРНЫХ УПРАЖНЕНИЙ:
            |# bench-press, incline-db-press, cable-flyes, tricep-dips,
            |# deadlift, bent-over-row, pull-ups, barbell-curl, hammer-curl,
            |# military-press, lateral-raise, front-raise, rear-delt-flye,
            |# squats, leg-press, leg-curl, leg-extension, calf-raise
            |#
            |# =====================================================
            |# После заполнения импортируйте файл через Beast App
            |# =====================================================
        """.trimMargin()
    }
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
                    item {
                        OutlinedButton(
                            onClick = { viewModel.downloadTemplate(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Скачать шаблон CSV")
                        }
                    }

                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
import android.content.Context
                    Text(
import android.os.Environment
import android.widget.Toast
                        text = "Importing program...",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else if (uiState.importResult != null) {
                // Result state
                ImportResultView(
import androidx.compose.material.icons.filled.Download
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
import kotlinx.coroutines.Dispatchers
                ) {
                    item {
                        Text(
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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


    fun downloadTemplate(context: Context) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val templateContent = generateTemplateContent()
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, "beast_app_program_template.csv")
                    
                    FileOutputStream(file).use { output ->
                        output.write(templateContent.toByteArray())
                    }
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Шаблон сохранён в Downloads: ${file.name}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
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

