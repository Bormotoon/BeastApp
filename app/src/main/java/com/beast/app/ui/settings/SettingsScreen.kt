@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.beast.app.ui.settings

import android.app.Application
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
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
import com.beast.app.data.preferences.NotificationPreferencesRepository
import com.beast.app.data.preferences.TrainingReminderPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    SettingsScreen(
        state = state,
        onBack = onBack,
        onChangeProgram = onChangeProgram,
        onToggleTrainingReminder = viewModel::setTrainingReminderEnabled,
        onSelectTrainingTime = viewModel::setTrainingReminderTime,
        onToggleTrainingDay = viewModel::toggleTrainingReminderDay
    )
}

@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onChangeProgram: () -> Unit,
    onToggleTrainingReminder: (Boolean) -> Unit,
    onSelectTrainingTime: (LocalTime) -> Unit,
    onToggleTrainingDay: (DayOfWeek) -> Unit
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
        }
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

    val uiState: StateFlow<SettingsUiState> = repository.preferencesFlow
        .map { preferences ->
            SettingsUiState(trainingReminder = TrainingReminderUiState.from(preferences.trainingReminder))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

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
        val current = uiState.value.trainingReminder.days
        val updated = if (current.contains(day)) {
            if (current.size == 1) current else current - day
        } else {
            current + day
        }
        viewModelScope.launch {
            repository.setTrainingReminderDays(updated)
        }
    }
}

data class SettingsUiState(
    val trainingReminder: TrainingReminderUiState = TrainingReminderUiState()
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
