@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.beast.app.ui.settings

import android.app.Application
import android.net.Uri
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beast.app.data.backup.BackupExporter
import com.beast.app.data.backup.DataExportFormat
import com.beast.app.data.backup.DataExportFormat.CSV_ARCHIVE
import com.beast.app.data.backup.DataExportFormat.JSON
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.preferences.NotificationPreferencesRepository
import com.beast.app.data.preferences.TrainingReminderPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onChangeProgram: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON.mimeType)) { uri: Uri? ->
        if (uri != null) {
            viewModel.export(JSON, uri)
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CSV_ARCHIVE.mimeType)) { uri: Uri? ->
        if (uri != null) {
            viewModel.export(CSV_ARCHIVE, uri)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SettingsEvent.ExportSuccess -> snackbarHostState.showSnackbar("Экспорт завершен: ${event.format.displayName}")
                is SettingsEvent.ExportFailure -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    SettingsScreen(
        state = state,
        onBack = onBack,
        onChangeProgram = onChangeProgram,
        onToggleTrainingReminder = viewModel::setTrainingReminderEnabled,
        onSelectTrainingTime = viewModel::setTrainingReminderTime,
        onToggleTrainingDay = viewModel::toggleTrainingReminderDay,
        snackbarHostState = snackbarHostState,
        onExportRequested = { format ->
            val fileName = viewModel.suggestedFileName(format)
            when (format) {
                JSON -> jsonLauncher.launch(fileName)
                CSV_ARCHIVE -> csvLauncher.launch(fileName)
            }
        }
    )
}

@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onChangeProgram: () -> Unit,
    onToggleTrainingReminder: (Boolean) -> Unit,
    onSelectTrainingTime: (LocalTime) -> Unit,
    onToggleTrainingDay: (DayOfWeek) -> Unit,
    snackbarHostState: SnackbarHostState,
    onExportRequested: (DataExportFormat) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProgramCard(onChangeProgram = onChangeProgram)
            }
            item {
                TrainingReminderCard(
                    state = state.trainingReminder,
                    onToggleReminder = onToggleTrainingReminder,
                    onSelectTime = onSelectTrainingTime,
                    onToggleDay = onToggleTrainingDay
                )
            }
            item {
                BackupCard(state = state.backup, onExportClick = onExportRequested)
            }
        }
    }
}

@Composable
private fun ProgramCard(onChangeProgram: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Программа", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Выбор и изменение активной программы",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onChangeProgram, modifier = Modifier.align(Alignment.End)) {
                Text("Изменить программу")
            }
        }
    }
}

@Composable
private fun BackupCard(
    state: BackupUiState,
    onExportClick: (DataExportFormat) -> Unit
) {
    val busy = state.isExportingJson || state.isExportingCsv
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Резервное копирование", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Экспортируйте данные приложения для сохранения на устройстве или в облаке.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { onExportClick(JSON) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isExportingJson) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Экспорт в JSON")
                }
            }
            OutlinedButton(
                onClick = { onExportClick(CSV_ARCHIVE) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isExportingCsv) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Экспорт в CSV (ZIP)")
                }
            }
            Text(
                text = "CSV экспортируется в виде ZIP-архива с отдельными таблицами.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrainingReminderCard(
    state: TrainingReminderUiState,
    onToggleReminder: (Boolean) -> Unit,
    onSelectTime: (LocalTime) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit
) {
    val context = LocalContext.current
    val locale = remember(context) {
        context.resources.configuration.locales[0] ?: Locale.getDefault()
    }
    val timePattern = remember(locale) {
        DateFormat.getBestDateTimePattern(locale, if (DateFormat.is24HourFormat(context)) "Hm" else "hm")
    }
    val timeFormatter = remember(timePattern, locale) {
        DateTimeFormatter.ofPattern(timePattern, locale)
    }
    val timeLabel = remember(state.time, timeFormatter) {
        state.time.format(timeFormatter)
    }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Напоминание о тренировке", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Получайте уведомления к выбранному времени",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RowWithLabel(
                label = "Включено",
                content = {
                    Switch(checked = state.enabled, onCheckedChange = onToggleReminder)
                }
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Время напоминания",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = { showTimePicker = true }, enabled = state.enabled) {
                    Text(timeLabel, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Дни недели",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (day in DayOfWeek.values()) {
                        val selected = state.days.contains(day)
                        val label = day.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.titlecase(locale) }
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected && state.days.size == 1) {
                                    return@FilterChip
                                }
                                onToggleDay(day)
                            },
                            enabled = state.enabled,
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = state.time.hour,
            initialMinute = state.time.minute,
            is24Hour = DateFormat.is24HourFormat(context)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onSelectTime(LocalTime.of(pickerState.hour, pickerState.minute))
                    showTimePicker = false
                }) {
                    Text("Готово")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Отмена")
                }
            },
            title = { Text("Выберите время") },
            text = {
                TimePicker(state = pickerState)
            }
        )
    }
}

@Composable
private fun RowWithLabel(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NotificationPreferencesRepository(application)
    private val backupExporter = BackupExporter(DatabaseProvider.get(application))

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.preferencesFlow.collect { preferences ->
                _uiState.update { current ->
                    current.copy(
                        trainingReminder = TrainingReminderUiState.from(preferences.trainingReminder)
                    )
                }
            }
        }
    }

    fun suggestedFileName(format: DataExportFormat): String = format.defaultFileName()

    fun export(format: DataExportFormat, uri: Uri) {
        viewModelScope.launch {
            setExportLoading(format, true)
            try {
                val resolver = getApplication<Application>().contentResolver
                when (format) {
                    JSON -> backupExporter.exportJson(resolver, uri)
                    CSV_ARCHIVE -> backupExporter.exportCsvArchive(resolver, uri)
                }
                _events.emit(SettingsEvent.ExportSuccess(format))
            } catch (t: Throwable) {
                val fallback = "Ошибка экспорта"
                val message = t.localizedMessage?.takeIf { it.isNotBlank() } ?: fallback
                _events.emit(SettingsEvent.ExportFailure(format, message))
            } finally {
                setExportLoading(format, false)
            }
        }
    }

    private fun setExportLoading(format: DataExportFormat, isLoading: Boolean) {
        _uiState.update { current ->
            val updatedBackup = when (format) {
                JSON -> current.backup.copy(isExportingJson = isLoading)
                CSV_ARCHIVE -> current.backup.copy(isExportingCsv = isLoading)
            }
            current.copy(backup = updatedBackup)
        }
    }

    fun setTrainingReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setTrainingReminderEnabled(enabled)
        }
    }

    fun setTrainingReminderTime(time: LocalTime) {
        viewModelScope.launch {
            repository.setTrainingReminderTime(time)
        }
    }

    fun toggleTrainingReminderDay(day: DayOfWeek) {
        val currentDays = uiState.value.trainingReminder.days
        val updated = if (currentDays.contains(day)) {
            if (currentDays.size == 1) currentDays else currentDays - day
        } else {
            currentDays + day
        }
        viewModelScope.launch {
            repository.setTrainingReminderDays(updated)
        }
    }
}

sealed interface SettingsEvent {
    data class ExportSuccess(val format: DataExportFormat) : SettingsEvent
    data class ExportFailure(val format: DataExportFormat, val message: String) : SettingsEvent
}

data class SettingsUiState(
    val trainingReminder: TrainingReminderUiState = TrainingReminderUiState(),
    val backup: BackupUiState = BackupUiState()
)

data class BackupUiState(
    val isExportingJson: Boolean = false,
    val isExportingCsv: Boolean = false
)

data class TrainingReminderUiState(
    val enabled: Boolean = false,
    val time: LocalTime = LocalTime.of(7, 0),
    val days: Set<DayOfWeek> = DayOfWeek.values().toSet()
) {
    companion object {
        fun from(preferences: TrainingReminderPreferences): TrainingReminderUiState {
            return TrainingReminderUiState(
                enabled = preferences.enabled,
                time = preferences.time,
                days = preferences.days
            )
        }
    }
}
