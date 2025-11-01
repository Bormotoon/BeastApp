@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.beast.app.ui.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProgressRoute(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenWorkout: (String) -> Unit,
    viewModel: ProgressViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onOpenHistory = onOpenHistory,
        onOpenWorkout = onOpenWorkout,
        onSelectPeriod = viewModel::selectPeriod
    )
}

@Composable
private fun ProgressScreen(
    state: ProgressUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenWorkout: (String) -> Unit,
    onSelectPeriod: (ProgressPeriod) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.BarChart, contentDescription = null)
                        Text(
                            text = "Прогресс",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = onOpenHistory) { Text("История") }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Обновить")
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
            state.errorMessage != null -> {
                ErrorState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    message = state.errorMessage,
                    onRetry = onRefresh
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PeriodSelector(
                                available = state.availablePeriods,
                                selected = state.selectedPeriod,
                                onSelect = onSelectPeriod
                            )
                            if (state.stats == null) {
                                EmptyStateCard(
                                    message = state.message ?: "Нет данных для отображения",
                                    onOpenHistory = onOpenHistory
                                )
                            }
                        }
                    }
                    state.stats?.let { stats ->
                        item { ProgramSummaryCard(stats) }
                        item { CompletionStatsCard(stats) }
                        item { VolumeStatsCard(stats) }
                        if (stats.recentWorkouts.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Недавние тренировки",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            items(stats.recentWorkouts, key = { it.logId }) { recent ->
                                RecentWorkoutCard(
                                    item = recent,
                                    onOpen = { onOpenWorkout(recent.workoutId) }
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
private fun PeriodSelector(
    available: List<ProgressPeriod>,
    selected: ProgressPeriod,
    onSelect: (ProgressPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        available.forEach { period ->
            FilterChip(
                selected = period == selected,
                onClick = { onSelect(period) },
                label = { Text(text = period.displayName) },
                leadingIcon = if (period == selected) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else null,
                colors = FilterChipDefaults.filterChipColors()
            )
        }
    }
}

@Composable
private fun ProgramSummaryCard(stats: ProgressStats) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stats.programName ?: "Активная программа не выбрана",
                style = MaterialTheme.typography.titleMedium,
                color = if (stats.programName == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = "Выполнено тренировок: ${stats.totalCompleted}",
                style = MaterialTheme.typography.bodyLarge
            )
            stats.completionPercent?.let { percent ->
                AssistChip(
                    onClick = {},
                    label = { Text(text = "Прогресс программы: $percent%") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun CompletionStatsCard(stats: ProgressStats) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Статистика выполнения", style = MaterialTheme.typography.titleMedium)
            StatRow(label = "Всего записей", value = stats.totalWorkoutsLogged.toString())
            StatRow(label = "Завершено", value = stats.totalCompleted.toString())
            StatRow(label = "Пропущено", value = stats.totalMissed.toString())
            StatRow(
                label = "Текущая серия",
                value = stats.currentStreak.toString(),
                emphasis = stats.currentStreak > 0
            )
            StatRow(label = "Лучшая серия", value = stats.bestStreak.toString())
        }
    }
}

@Composable
private fun VolumeStatsCard(stats: ProgressStats) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Нагрузка", style = MaterialTheme.typography.titleMedium)
            val volumeUnit = if (stats.weightUnit.equals("lbs", ignoreCase = true)) "фунтов" else "кг"
            StatRow(
                label = "Общий объём",
                value = formatNumber(stats.totalVolume) + " " + volumeUnit
            )
            StatRow(label = "Всего повторений", value = stats.totalReps.toString())
            StatRow(label = "Суммарное время", value = formatDuration(stats.totalDurationMinutes))
            StatRow(
                label = "Средняя длительность",
                value = formatDuration(stats.averageDurationMinutes.roundToInt())
            )
            StatRow(
                label = "Средний объём",
                value = formatNumber(stats.averageVolume) + " " + volumeUnit
            )
        }
    }
}

@Composable
private fun RecentWorkoutCard(item: RecentWorkoutSummary, onOpen: () -> Unit) {
    val statusColor = when (item.status) {
        RecentWorkoutStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
        RecentWorkoutStatus.MISSED -> MaterialTheme.colorScheme.errorContainer
        RecentWorkoutStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusContentColor = when (item.status) {
        RecentWorkoutStatus.COMPLETED -> MaterialTheme.colorScheme.onTertiaryContainer
        RecentWorkoutStatus.MISSED -> MaterialTheme.colorScheme.onErrorContainer
        RecentWorkoutStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = statusColor,
                    contentColor = statusContentColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = item.statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                item.durationLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item.volumeLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, emphasis: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (emphasis) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (emphasis) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun EmptyStateCard(message: String, onOpenHistory: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Button(onClick = onOpenHistory) { Text("Перейти к истории") }
        }
    }
}

@Composable
private fun ErrorState(modifier: Modifier, message: String, onRetry: () -> Unit) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Повторить") }
    }
}

private fun formatNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}

private fun formatDuration(totalMinutes: Int): String {
    if (totalMinutes <= 0) return "—"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0 -> "$minutes мин"
        minutes == 0 -> "$hours ч"
        else -> "$hours ч $minutes мин"
    }
}
