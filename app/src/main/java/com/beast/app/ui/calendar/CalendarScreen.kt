package com.beast.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import com.beast.app.utils.DateFormatting

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
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val selectedSummary = selectedDate?.let { state.daySummaries[it] }

    if (selectedSummary != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedDate = null },
            sheetState = bottomSheetState
        ) {
            DayDetailsSheet(
                summary = selectedSummary,
                onDismiss = { selectedDate = null },
                onStartWorkout = onStartWorkout,
                onViewWorkoutDetails = onViewWorkoutDetails
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Календарь", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Обновить")
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
                    onSelectDay = { date -> selectedDate = date }
                )
            }
        }
    }
}

@Composable
private fun CalendarContent(
    state: CalendarUiState,
    modifier: Modifier = Modifier,
    onSelectDay: (LocalDate) -> Unit
) {
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek) }
    val calendarState = rememberCalendarState(
        startMonth = state.startMonth,
        endMonth = state.endMonth,
        firstVisibleMonth = state.initialMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                val summary = state.daySummaries[day.date]
                DayCell(
                    day = day,
                    summary = summary,
                    isToday = day.date == state.today,
                    onSelect = onSelectDay
                )
            },
            monthHeader = { month ->
                MonthHeader(
                    monthName = month.yearMonth,
                    daysOfWeek = daysOfWeek
                )
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        Legend()
    }
}

@Composable
private fun MonthHeader(monthName: java.time.YearMonth, daysOfWeek: List<DayOfWeek>) {
    val locale = Locale.getDefault()
    val formatter = remember(locale) { DateFormatting.dateFormatter(locale, "LLLLyyyy") }
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp)) {
        Text(
            text = DateFormatting.capitalize(monthName.format(formatter), locale),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daysOfWeek.forEach { dayOfWeek ->
                Text(
                    modifier = Modifier.weight(1f),
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    summary: CalendarDaySummary?,
    isToday: Boolean,
    onSelect: (LocalDate) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val background = summary?.status?.let { statusBackgroundColor(it, colors) } ?: Color.Transparent
    val contentColor = summary?.status?.let { statusContentColor(it, colors) }
        ?: if (day.position == DayPosition.MonthDate) colors.onSurface else colors.onSurface.copy(alpha = 0.4f)
    val hasOutline = isToday && summary?.status != CalendarDayStatus.CURRENT

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .then(if (hasOutline) Modifier.border(1.5.dp, colors.primary, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = summary != null) { onSelect(day.date) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            fontWeight = if (summary?.status == CalendarDayStatus.CURRENT) FontWeight.Bold else FontWeight.Normal
        )
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
private fun DayDetailsSheet(
    summary: CalendarDaySummary,
    onDismiss: () -> Unit,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutDetails: (String) -> Unit
) {
    val locale = Locale.getDefault()
    val dateFormatter = remember(locale) { DateFormatting.dateFormatter(locale, "yMMMMd") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
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
            summary.workoutId == null -> {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Закрыть")
                }
            }
            summary.status == CalendarDayStatus.COMPLETED -> {
                Button(onClick = { onViewWorkoutDetails(summary.workoutId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Просмотр")
                }
            }
            summary.status == CalendarDayStatus.REST -> {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Закрыть")
                }
            }
            else -> {
                Button(onClick = { onStartWorkout(summary.workoutId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Начать")
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
