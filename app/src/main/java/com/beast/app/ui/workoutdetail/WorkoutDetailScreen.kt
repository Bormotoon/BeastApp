@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.beast.app.ui.workoutdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WorkoutDetailRoute(
    onBack: () -> Unit,
    onStartWorkout: (String) -> Unit,
    onViewWorkoutLog: (String) -> Unit = {},
    viewModel: WorkoutDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutDetailScreen(
        state = state,
        onBack = onBack,
        onStartWorkout = onStartWorkout,
        onViewLog = onViewWorkoutLog,
        onRetry = viewModel::refresh,
        onToggleFavorite = viewModel::toggleFavorite
    )
}

@Composable
private fun WorkoutDetailScreen(
    state: WorkoutDetailUiState,
    onBack: () -> Unit,
    onStartWorkout: (String) -> Unit,
    onViewLog: (String) -> Unit,
    onRetry: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.workoutName ?: "Тренировка",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
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
                ErrorContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    message = state.errorMessage,
                    onRetry = onRetry
                )
            }
            else -> {
                val workoutId = state.workoutId
                if (workoutId == null) {
                    ErrorContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        message = "Некорректные данные тренировки",
                        onRetry = onRetry
                    )
                } else {
                    WorkoutDetailContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        state = state,
                        onStartWorkout = { onStartWorkout(workoutId) },
                        onViewLog = onViewLog,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutDetailContent(
    modifier: Modifier,
    state: WorkoutDetailUiState,
    onStartWorkout: () -> Unit,
    onViewLog: (String) -> Unit,
    onToggleFavorite: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Упражнения", "История", "Графики")

    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OverviewCard(
                    state = state,
                    onStartWorkout = onStartWorkout,
                    onToggleFavorite = onToggleFavorite
                )
            }
            item {
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(label) }
                        )
                    }
                }
            }
            when (selectedTab) {
                0 -> {
                    if (state.exercises.isEmpty()) {
                        item {
                            Text(
                                text = "Упражнения не найдены",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(state.exercises, key = { it.id }) { exercise ->
                            ExerciseCard(exercise)
                        }
                    }
                }
                1 -> {
                    if (state.history.isEmpty()) {
                        item {
                            HistoryEmptyState()
                        }
                    } else {
                        items(state.history, key = { it.id }) { historyItem ->
                            HistoryCard(
                                item = historyItem,
                                onViewLog = { onViewLog(historyItem.id) },
                                onRepeat = onStartWorkout
                            )
                        }
                    }
                }
                else -> {
                    item {
                        WorkoutChartsTab(points = state.chartPoints, weightUnit = state.weightUnit)
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    state: WorkoutDetailUiState,
    onStartWorkout: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.programName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = state.workoutName ?: "Тренировка",
                style = MaterialTheme.typography.headlineSmall
            )

            val chips = buildList {
                state.phaseName?.let { add("Фаза: $it") }
                if (state.durationMinutes > 0) add("~${state.durationMinutes} мин")
                if (state.exerciseCount > 0) add("${state.exerciseCount} упр.")
            }
            if (chips.isNotEmpty()) {
                FlowRowSpacered(content = chips)
            }

            if (state.muscleGroups.isNotEmpty()) {
                Text(
                    text = "Целевые мышцы: ${state.muscleGroups.joinToString(", ")}"
                )
            }

            state.lastCompletedLabel?.let {
                Text(
                    text = "Последний раз: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val favoriteLabel = if (state.isFavorite) "В избранном" else "Добавить в избранное"
                val favoriteIcon = if (state.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder
                val favoriteColors = if (state.isFavorite) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }

                OutlinedButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.weight(1f),
                    colors = favoriteColors
                ) {
                    Icon(imageVector = favoriteIcon, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(favoriteLabel)
                }

                Button(onClick = onStartWorkout, modifier = Modifier.weight(1f)) {
                    Text("Начать тренировку")
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(model: WorkoutExerciseUiModel) {
    var showNotes by rememberSaveable(model.id) { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${model.order}. ${model.name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (model.primaryMuscle.isNotBlank()) {
                        Text(
                            text = "Основная группа: ${model.primaryMuscle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (model.setType.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = model.setTypeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (model.targetReps.isNotBlank()) {
                Text(
                    text = "Повторы: ${model.targetReps}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (model.equipment.isNotEmpty()) {
                Text(
                    text = "Оборудование: ${model.equipment.joinToString(", ")}"
                )
            }

            Text(
                text = "Последние данные: пока нет истории",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val hasDetails = (model.notes?.isNotBlank() == true) || (model.instructions?.isNotBlank() == true) || (model.videoUrl?.isNotBlank() == true)

            if (hasDetails) {
                OutlinedButton(onClick = { showNotes = !showNotes }) {
                    Text(if (showNotes) "Скрыть детали" else "Подробнее")
                }

                if (showNotes) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        model.notes?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = "Примечания: $it",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        model.instructions?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        model.videoUrl?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = "Видео: $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "История подходов появится позже",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEmptyState() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null
            )
            Text(
                text = "Пока нет завершённых сессий. После тренировки здесь появится история.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryCard(
    item: WorkoutHistoryItemUiModel,
    onViewLog: () -> Unit,
    onRepeat: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.dateLabel,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = item.statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.isBestVolume) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MilitaryTech,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Лучший объём",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            item.durationLabel?.let {
                HistoryInfoRow(label = "Длительность", value = it)
            }
            item.volumeLabel?.let {
                HistoryInfoRow(label = "Объём", value = it)
            }
            item.repsLabel?.let {
                HistoryInfoRow(label = "Повторения", value = it)
            }
            item.caloriesLabel?.let {
                HistoryInfoRow(label = "Калории", value = it)
            }
            item.ratingLabel?.let {
                HistoryInfoRow(label = "Оценка", value = it)
            }

            item.notesPreview?.let {
                Text(
                    text = "Заметка: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onViewLog, modifier = Modifier.weight(1f)) {
                    Text("Просмотр")
                }
                Button(
                    onClick = onRepeat,
                    enabled = item.canRepeat,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Повторить")
                }
            }
        }
    }
}

@Composable
private fun HistoryInfoRow(label: String, value: String) {
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
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ErrorContent(modifier: Modifier, message: String, onRetry: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) {
            Text("Повторить")
        }
    }
}

@Composable
private fun FlowRowSpacered(content: List<String>) {
    if (content.isEmpty()) return
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content.forEach { label ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun WorkoutChartsTab(points: List<WorkoutTrendPoint>, weightUnit: String) {
    val volumeUnit = if (weightUnit.equals("lbs", ignoreCase = true)) "фунтов" else "кг"
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        WorkoutMetricChart(
            title = "Общий объём",
            subtitle = "за тренировку, $volumeUnit",
            points = points,
            valueSelector = { it.volume },
            valueFormatterFn = { value -> formatFloat(value) }
        )
        WorkoutMetricChart(
            title = "Средняя интенсивность",
            subtitle = "${volumeUnit}/мин",
            points = points,
            valueSelector = { it.intensity },
            valueFormatterFn = { value -> formatFloat(value, 2) }
        )
        WorkoutMetricChart(
            title = "Время выполнения",
            subtitle = "минут",
            points = points,
            valueSelector = { it.durationMinutes },
            valueFormatterFn = { value -> formatFloat(value) }
        )
    }
}

@Composable
private fun WorkoutMetricChart(
    title: String,
    subtitle: String,
    points: List<WorkoutTrendPoint>,
    valueSelector: (WorkoutTrendPoint) -> Float?,
    valueFormatterFn: (Float) -> String
) {
    val entries = points.mapIndexedNotNull { index, point ->
        val value = valueSelector(point) ?: return@mapIndexedNotNull null
        Entry(index.toFloat(), value)
    }
    val labels = points.map { it.label }
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryArgb = primaryColor.toArgb()
    val primaryFillArgb = primaryColor.copy(alpha = 0.2f).toArgb()
    val axisTextArgb = MaterialTheme.colorScheme.onSurface.toArgb()

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (entries.isEmpty()) {
                Text(
                    text = "Недостаточно данных для построения графика",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    factory = { context ->
                        LineChart(context).apply {
                            setTouchEnabled(true)
                            isDragEnabled = true
                            setScaleEnabled(false)
                            description.isEnabled = false
                            legend.isEnabled = false
                            axisRight.isEnabled = false
                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            xAxis.setDrawGridLines(false)
                            axisLeft.setDrawGridLines(true)
                        }
                    },
                    update = { chart ->
                        val dataSet = LineDataSet(entries, title).apply {
                            color = primaryArgb
                            setCircleColor(primaryArgb)
                            lineWidth = 2f
                            circleRadius = 4f
                            setDrawValues(false)
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                            setDrawFilled(true)
                            fillColor = primaryFillArgb
                        }
                        chart.data = LineData(dataSet)
                        chart.xAxis.apply {
                            granularity = 1f
                            axisMinimum = 0f
                            axisMaximum = (labels.size - 1).coerceAtLeast(0).toFloat()
                            this.valueFormatter = IndexAxisValueFormatter(labels)
                            labelRotationAngle = if (labels.size > 4) 45f else 0f
                            textColor = axisTextArgb
                        }
                        chart.axisLeft.apply {
                            textColor = axisTextArgb
                            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                                override fun getFormattedValue(value: Float): String = valueFormatterFn(value)
                            }
                        }
                        chart.invalidate()
                    }
                )
            }
        }
    }
}

private fun formatFloat(value: Float, decimals: Int = 1): String {
    return if (value % 1f == 0f) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.${decimals}f", value)
    }
}