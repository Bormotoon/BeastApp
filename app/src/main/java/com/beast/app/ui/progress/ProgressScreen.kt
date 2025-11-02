@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.beast.app.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.beast.app.utils.DateFormatting
import java.time.LocalDate
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
                var selectedTab by rememberSaveable { mutableStateOf(ProgressTab.OVERVIEW) }
                LaunchedEffect(state.stats) {
                    if (state.stats == null) {
                        selectedTab = ProgressTab.OVERVIEW
                    }
                }

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
                        item {
                            ProgressTabs(
                                selected = selectedTab,
                                onSelect = { selectedTab = it }
                            )
                        }
                        when (selectedTab) {
                            ProgressTab.OVERVIEW -> {
                                item { ProgramSummaryCard(stats) }
                                item { CompletionStatsCard(stats) }
                                if (stats.heatmap.isNotEmpty()) {
                                    item { ActivityHeatmapCard(stats.heatmap) }
                                }
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
                            ProgressTab.VOLUME -> {
                                item { VolumeStatsCard(stats) }
                                if (stats.volumeSeries.isNotEmpty()) {
                                    item { VolumeTrendCard(stats.volumeSeries, stats.weightUnit) }
                                }
                                if (stats.muscleVolume.isNotEmpty()) {
                                    item { VolumeBreakdownCard("Нагрузка по группам мышц", stats.muscleVolume, stats.weightUnit) }
                                }
                                if (stats.exerciseVolume.isNotEmpty()) {
                                    item { VolumeBreakdownCard("Нагрузка по упражнениям", stats.exerciseVolume, stats.weightUnit, limit = 6) }
                                }
                                if (stats.muscleDistribution.isNotEmpty()) {
                                    item { MuscleDistributionCard(stats.muscleDistribution) }
                                }
                            }
                            ProgressTab.RECORDS -> {
                                if (stats.records.isEmpty()) {
                                    item {
                                        EmptyStateCard(
                                            message = "Личные рекорды пока не зафиксированы",
                                            onOpenHistory = onOpenHistory
                                        )
                                    }
                                } else {
                                    item { RecordsSummaryRow(stats.records) }
                                    items(stats.records, key = { it.exerciseId }) { record ->
                                        RecordCard(record)
                                    }
                                }
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
private fun VolumeTrendCard(series: List<VolumePoint>, weightUnit: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Общий объём по неделям", style = MaterialTheme.typography.titleMedium)
            if (series.isEmpty()) {
                Text(
                    text = "Недостаточно данных для отображения графика",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val locale = Locale.getDefault()
                val formatter = remember(locale) { DateFormatting.dateFormatter(locale, "MMMd") }
                val displaySeries = if (series.size > 12) series.takeLast(12) else series
                val labels = displaySeries.map { formatter.format(it.weekStart) }
                val unitSuffix = if (weightUnit.equals("lbs", ignoreCase = true)) "фунтов" else "кг"
                val barColor = MaterialTheme.colorScheme.primary.toArgb()
                val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                val valueColor = MaterialTheme.colorScheme.onSurface.toArgb()

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    factory = { context ->
                        BarChart(context).apply {
                            description.isEnabled = false
                            legend.isEnabled = false
                            setScaleEnabled(false)
                            setDrawBarShadow(false)
                            axisRight.isEnabled = false
                            axisLeft.setDrawGridLines(false)
                            xAxis.setDrawGridLines(false)
                        }
                    },
                    update = { chart ->
                        val entries = displaySeries.mapIndexed { index, point ->
                            BarEntry(index.toFloat(), point.totalVolume.toFloat())
                        }
                        val dataSet = BarDataSet(entries, null).apply {
                            color = barColor
                            valueTextColor = valueColor
                            valueTextSize = 10f
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    return formatNumber(value.toDouble())
                                }
                            }
                        }
                        chart.axisLeft.apply {
                            axisMinimum = 0f
                            textColor = axisColor
                        }
                        chart.xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            granularity = 1f
                            textColor = axisColor
                            valueFormatter = object : ValueFormatter() {
                                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                                    val index = value.toInt()
                                    return labels.getOrNull(index) ?: ""
                                }
                            }
                        }
                        chart.data = BarData(dataSet).apply { barWidth = 0.6f }
                        chart.invalidate()
                    }
                )

                val peak = displaySeries.maxByOrNull { it.totalVolume }
                peak?.let { point ->
                    val index = displaySeries.indexOf(point)
                    val label = labels.getOrNull(index) ?: formatter.format(point.weekStart)
                    Text(
                        text = "Пик объёма: $label — ${formatNumber(point.totalVolume)} $unitSuffix",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VolumeBreakdownCard(
    title: String,
    items: List<VolumeBreakdownEntry>,
    weightUnit: String,
    limit: Int = 8
) {
    val displayItems = items.take(limit)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (displayItems.isEmpty()) {
                Text(
                    text = "Недостаточно данных для отображения графика",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val entries = displayItems.reversed()
                val labels = entries.map { it.label }
                val barColor = MaterialTheme.colorScheme.primary.toArgb()
                val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                val valueColor = MaterialTheme.colorScheme.onSurface.toArgb()
                val unitSuffix = if (weightUnit.equals("lbs", ignoreCase = true)) "фунтов" else "кг"

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((entries.size * 32).coerceAtLeast(160).dp),
                    factory = { context ->
                        HorizontalBarChart(context).apply {
                            description.isEnabled = false
                            legend.isEnabled = false
                            setScaleEnabled(false)
                            axisLeft.setDrawGridLines(false)
                            axisRight.isEnabled = false
                            xAxis.setDrawGridLines(false)
                            setDrawValueAboveBar(true)
                        }
                    },
                    update = { chart ->
                        val barEntries = entries.mapIndexed { index, item ->
                            BarEntry(index.toFloat(), item.volume.toFloat())
                        }
                        val dataSet = BarDataSet(barEntries, null).apply {
                            color = barColor
                            valueTextColor = valueColor
                            valueTextSize = 10f
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    return formatNumber(value.toDouble())
                                }
                            }
                        }
                        chart.axisLeft.apply {
                            axisMinimum = 0f
                            textColor = axisColor
                        }
                        chart.xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            granularity = 1f
                            textColor = axisColor
                            valueFormatter = object : ValueFormatter() {
                                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                                    val index = value.toInt()
                                    return labels.getOrNull(index) ?: ""
                                }
                            }
                        }
                        chart.data = BarData(dataSet).apply { barWidth = 0.6f }
                        chart.invalidate()
                    }
                )

                val topItem = displayItems.first()
                Text(
                    text = "Максимум: ${topItem.label} — ${formatNumber(topItem.volume)} $unitSuffix",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

@Composable
private fun ProgressTabs(selected: ProgressTab, onSelect: (ProgressTab) -> Unit) {
    val tabs = ProgressTab.values()
    TabRow(selectedTabIndex = tabs.indexOf(selected)) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                text = {
                    Text(
                        text = tab.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
private fun ActivityHeatmapCard(days: List<HeatmapDay>) {
    val locale = Locale.getDefault()
    val weeks = days.chunked(7)
    if (weeks.isEmpty()) return

    val monthFormatter = DateFormatting.dateFormatter(locale, "LLL")
    val dayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Активность по дням", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayLabels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    weeks.forEachIndexed { index, week ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            week.forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(colorForIntensity(day.intensity), shape = MaterialTheme.shapes.extraSmall)
                                )
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                weeks.forEachIndexed { index, week ->
                    val firstDay = week.first().date
                    val label = monthFormatter.format(firstDay).replaceFirstChar { ch ->
                        if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString()
                    }
                    if (index == 0 || week.first().date.month != weeks.getOrNull(index - 1)?.first()?.date?.month) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.width(24.dp))
                    }
                }
            }
            IntensityLegend()
        }
    }
}

@Composable
private fun IntensityLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Меньше",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        (0..4).forEach { level ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(colorForIntensity(level), shape = MaterialTheme.shapes.extraSmall)
            )
        }
        Text(
            text = "Больше",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MuscleDistributionCard(distribution: List<MuscleGroupShare>) {
    val themeColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.outlineVariant
    )
    val colorInts = themeColors.map(Color::toArgb)
    val total = distribution.sumOf { it.occurrences }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Распределение по мышечным группам", style = MaterialTheme.typography.titleMedium)
            if (total == 0) {
                Text(
                    text = "Недостаточно данных для построения диаграммы",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    factory = { context ->
                        PieChart(context).apply {
                            setUsePercentValues(true)
                            description.isEnabled = false
                            legend.isEnabled = false
                            setDrawEntryLabels(false)
                            setHoleColor(android.graphics.Color.TRANSPARENT)
                            setHoleRadius(65f)
                            setTransparentCircleRadius(68f)
                        }
                    },
                    update = { chart ->
                        val entries = distribution.map { slice ->
                            PieEntry(slice.occurrences.toFloat(), slice.group)
                        }
                        val entryColors = entries.mapIndexed { index, _ ->
                            colorInts[index % colorInts.size]
                        }
                        val dataSet = PieDataSet(entries, null).apply {
                            colors = entryColors
                            sliceSpace = 2f
                            valueTextSize = 12f
                            valueFormatter = PercentFormatter(chart)
                            setDrawValues(true)
                        }
                        val pieData = PieData(dataSet).apply {
                            setValueFormatter(PercentFormatter(chart))
                            setValueTextColor(android.graphics.Color.WHITE)
                        }
                        chart.data = pieData
                        chart.invalidate()
                    }
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    distribution.forEachIndexed { index, slice ->
                        val color = themeColors[index % themeColors.size]
                        LegendRow(color = color, label = slice.group, percentage = slice.percentage)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordsSummaryRow(records: List<RecordSummary>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryChip(label = "Всего рекордов", value = records.size.toString())
        val recent = records.count { it.isRecent }
        if (recent > 0) {
            SummaryChip(label = "Новые записи", value = recent.toString())
        }
        val strongest = records.maxByOrNull { it.estimatedOneRm }
        strongest?.let {
            SummaryChip(
                label = "Самый сильный", 
                value = it.exerciseName,
                emphasis = true
            )
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String, emphasis: Boolean = false) {
    Surface(
        color = if (emphasis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (emphasis) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RecordCard(record: RecordSummary) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = record.exerciseName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = record.lastAchievedLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (record.isRecent) {
                    AssistChip(onClick = {}, label = { Text("Новый") })
                }
            }
            val weightUnitLabel = if (record.weightUnit.equals("lbs", ignoreCase = true)) "фунтов" else "кг"
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatPill(label = "Вес", value = formatNumber(record.weight) + " " + weightUnitLabel)
                StatPill(label = "Повторения", value = record.reps.toString())
                StatPill(label = "1RM", value = formatNumber(record.estimatedOneRm) + " " + weightUnitLabel)
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, percentage: Double) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = MaterialTheme.shapes.extraSmall)
        )
        Text(
            text = "$label — ${formatPercent(percentage)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun colorForIntensity(level: Int): Color {
    return when (level) {
        0 -> MaterialTheme.colorScheme.surfaceVariant
        1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.primary
    }
}

enum class ProgressTab(val label: String) {
    OVERVIEW("Обзор"),
    VOLUME("Объем"),
    RECORDS("Рекорды")
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

private fun formatPercent(value: Double): String {
    return String.format(Locale.getDefault(), "%.0f%%", value)
}
