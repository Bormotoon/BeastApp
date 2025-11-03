package com.beast.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beast.app.R
import com.beast.app.utils.DateFormatting
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@Composable
fun CalendarRoute(
    onBack: () -> Unit,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit,
    viewModel: CalendarViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onStartWorkout = onStartWorkout,
        onViewWorkoutDetails = onViewWorkoutDetails
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val defaultSelectedDate = remember(state.daySummaries, state.today) {
        when {
            state.daySummaries.containsKey(state.today) -> state.today
            state.daySummaries.isNotEmpty() -> state.daySummaries.keys.minOrNull()
            else -> null
        }
    }

    LaunchedEffect(state.daySummaries, defaultSelectedDate) {
        val current = selectedDate
        when {
            state.daySummaries.isEmpty() -> selectedDate = null
            current == null && defaultSelectedDate != null -> selectedDate = defaultSelectedDate
            current != null && !state.daySummaries.containsKey(current) && defaultSelectedDate != null -> selectedDate = defaultSelectedDate
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        val initialDate = selectedDate ?: defaultSelectedDate ?: state.today
        val selectableDates = remember(state.startDate, state.endDate, state.daySummaries) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = utcTimeMillis.toLocalDate(zone)
                    val afterStart = state.startDate?.let { !date.isBefore(it) } ?: true
                    val beforeEnd = state.endDate?.let { !date.isAfter(it) } ?: true
                    return afterStart && beforeEnd && state.daySummaries.containsKey(date)
                }
            }
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.toEpochMillis(zone),
            selectableDates = selectableDates
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        val picked = millis?.toLocalDate(zone)
                        if (picked != null && state.daySummaries.containsKey(picked)) {
                            selectedDate = picked
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Выбрать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Календарь", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            !state.hasActiveProgram -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message ?: "Нет данных для отображения",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                CalendarContent(
                    state = state,
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    selectedDate = selectedDate,
                    onShowDatePicker = { showDatePicker = true },
                    onSelectDate = { selectedDate = it },
                    onStartWorkout = onStartWorkout,
                    onViewWorkoutDetails = onViewWorkoutDetails
                )
            }
        }
    }
}

@Composable
private fun CalendarContent(
    state: CalendarUiState,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate?,
    onShowDatePicker: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit
) {
    val summaries = state.daySummaries
    val sortedDates = remember(summaries) { summaries.keys.sorted() }
    val selectedSummary = selectedDate?.let { summaries[it] }
    val hasToday = summaries.containsKey(state.today)
    val currentIndex = selectedDate?.let { sortedDates.indexOf(it) } ?: -1
    val previousDate = if (currentIndex > 0) sortedDates[currentIndex - 1] else null
    val nextDate = if (currentIndex != -1 && currentIndex < sortedDates.lastIndex) sortedDates[currentIndex + 1] else null

    Column(modifier = modifier.fillMaxWidth()) {
        state.programName?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        ProgramProgressRow(summaries = summaries)

        Spacer(modifier = Modifier.height(16.dp))

        DateSelectionRow(
            selectedDate = selectedDate,
            today = state.today,
            hasToday = hasToday,
            onSelectToday = { if (hasToday) onSelectDate(state.today) },
            onShowPicker = onShowDatePicker,
            onSelectPrevious = previousDate?.let { { onSelectDate(it) } },
            onSelectNext = nextDate?.let { { onSelectDate(it) } }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedSummary != null) {
            DaySummaryCard(
                summary = selectedSummary,
                onStartWorkout = onStartWorkout,
                onViewWorkoutDetails = onViewWorkoutDetails
            )
        } else {
            Text(
                text = "Выберите дату, чтобы увидеть детали",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Legend()
    }
}

@Composable
private fun ProgramProgressRow(summaries: Map<LocalDate, CalendarDaySummary>) {
    if (summaries.isEmpty()) return
    val totalDays = summaries.size
    val completedDays = summaries.values.count { it.status == CalendarDayStatus.COMPLETED }
    val progress = completedDays.toFloat() / totalDays.toFloat()
    Column {
        Text(text = "Прогресс программы", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
        val progressValue = progress.coerceIn(0f, 1f)
        LinearProgressIndicator(progress = { progressValue }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$completedDays из $totalDays дней выполнено",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DateSelectionRow(
    selectedDate: LocalDate?,
    today: LocalDate,
    hasToday: Boolean,
    onSelectToday: () -> Unit,
    onShowPicker: () -> Unit,
    onSelectPrevious: (() -> Unit)?,
    onSelectNext: (() -> Unit)?
) {
    val locale = Locale.getDefault()
    val formatter = remember(locale) { DateFormatting.dateFormatter(locale, "yMMMMd") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onSelectPrevious?.invoke() }, enabled = onSelectPrevious != null) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Предыдущий день")
        }

        FilledTonalButton(onClick = onShowPicker) {
            val label = selectedDate?.format(formatter)?.let { DateFormatting.capitalize(it, locale) } ?: "Выбрать дату"
            Text(label, maxLines = 1)
        }

        IconButton(onClick = { onSelectNext?.invoke() }, enabled = onSelectNext != null) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Следующий день")
        }
    }

    if (hasToday && selectedDate != today) {
        Spacer(modifier = Modifier.height(8.dp))
        AssistChip(onClick = onSelectToday, label = { Text("Сегодня") })
    }
}

@Composable
private fun Legend() {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Обозначения",
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LegendRow(color = statusBackgroundColor(CalendarDayStatus.COMPLETED, colors), label = "Выполнено")
        LegendRow(color = statusBackgroundColor(CalendarDayStatus.CURRENT, colors), label = "Сегодня")
        LegendRow(color = statusBackgroundColor(CalendarDayStatus.MISSED, colors), label = "Пропущено")
        LegendRow(color = statusBackgroundColor(CalendarDayStatus.REST, colors), label = "Отдых")
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(16.dp)
                .background(color, CircleShape)
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DaySummaryCard(
    summary: CalendarDaySummary,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit
) {
    val locale = Locale.getDefault()
    val dateFormatter = remember(locale) { DateFormatting.dateFormatter(locale, "yMMMMd") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = DateFormatting.capitalize(summary.date.format(dateFormatter), locale),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            summary.dayNumber?.let {
                Text(
                    text = "День $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            AssistChip(
                onClick = {},
                label = { Text(text = summary.status.label()) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = statusBackgroundColor(summary.status, MaterialTheme.colorScheme),
                    labelColor = statusContentColor(summary.status, MaterialTheme.colorScheme)
                )
            )
            if (summary.isUnscheduled) {
                Text(
                    text = "Выполнено вне расписания",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = summary.workoutName ?: "День отдыха",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            StatsRow(summary)
            Spacer(modifier = Modifier.height(16.dp))
            when {
                summary.status == CalendarDayStatus.COMPLETED && summary.workoutId != null -> {
                    Button(onClick = { onViewWorkoutDetails(summary.workoutId) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Просмотр")
                    }
                }
                summary.workoutId != null && summary.status != CalendarDayStatus.REST -> {
                    Button(onClick = { onStartWorkout(summary.workoutId) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Начать")
                    }
                }
                else -> {
                    Text(
                        text = "Нет тренировки на эту дату",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(summary: CalendarDaySummary) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatBlock(title = "Длительность", value = formatDuration(summary))
        StatBlock(title = "Объем", value = formatVolume(summary.totalVolume))
        StatBlock(title = "Повторения", value = summary.totalReps?.toString() ?: "—")
    }
}

@Composable
private fun StatBlock(title: String, value: String) {
    Column { 
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

private fun formatDuration(summary: CalendarDaySummary): String {
    val minutes = summary.completedDurationMinutes ?: summary.plannedDurationMinutes
    minutes ?: return "—"
    if (minutes <= 0) return "—"
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours == 0 -> "$minutes мин"
        remainder == 0 -> "$hours ч"
        else -> "$hours ч $remainder мин"
    }
}

private fun formatVolume(volume: Double?): String {
    volume ?: return "—"
    return if (volume >= 1000) {
        "%.1f кг".format(Locale.getDefault(), volume)
    } else {
        "%.0f кг".format(Locale.getDefault(), volume)
    }
}

private fun statusBackgroundColor(status: CalendarDayStatus, colors: androidx.compose.material3.ColorScheme): Color = when (status) {
    CalendarDayStatus.COMPLETED -> colors.tertiaryContainer
    CalendarDayStatus.MISSED -> colors.errorContainer
    CalendarDayStatus.CURRENT -> colors.primaryContainer
    CalendarDayStatus.UPCOMING -> colors.secondaryContainer
    CalendarDayStatus.REST -> colors.surfaceVariant
}

private fun statusContentColor(status: CalendarDayStatus, colors: androidx.compose.material3.ColorScheme): Color = when (status) {
    CalendarDayStatus.COMPLETED -> colors.onTertiaryContainer
    CalendarDayStatus.MISSED -> colors.onErrorContainer
    CalendarDayStatus.CURRENT -> colors.onPrimaryContainer
    CalendarDayStatus.UPCOMING -> colors.onSecondaryContainer
    CalendarDayStatus.REST -> colors.onSurfaceVariant
}

private fun CalendarDayStatus.label(): String = when (this) {
    CalendarDayStatus.COMPLETED -> "Выполнено"
    CalendarDayStatus.MISSED -> "Пропущено"
    CalendarDayStatus.CURRENT -> "Сегодня"
    CalendarDayStatus.UPCOMING -> "Запланировано"
    CalendarDayStatus.REST -> "Отдых"
}

private fun LocalDate.toEpochMillis(zone: ZoneId): Long = this.atStartOfDay(zone).toInstant().toEpochMilli()

private fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
