@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.beast.app.ui.workouthistory

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beast.app.R

@Composable
fun WorkoutHistoryRoute(
    onBack: () -> Unit,
    onSelectWorkout: (String) -> Unit,
    onStartFirstWorkout: () -> Unit,
    viewModel: WorkoutHistoryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutHistoryScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onSelectLog = { onSelectWorkout(it.workoutId) },
        onStartFirstWorkout = onStartFirstWorkout
    )
}

@Composable
private fun WorkoutHistoryScreen(
    state: WorkoutHistoryUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelectLog: (WorkoutHistoryItem) -> Unit,
    onStartFirstWorkout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История тренировок", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onRefresh) {
                        Text("Обновить")
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
                HistoryErrorState(
                    message = state.errorMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onRetry = onRefresh
                )
            }
            state.entries.isEmpty() -> {
                HistoryEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    message = state.message ?: "История пока пустая",
                    showStartButton = state.hasActiveProgram,
                    onStartFirstWorkout = onStartFirstWorkout
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
                    items(state.entries, key = { it.logId }) { item ->
                        WorkoutHistoryCard(item = item, onSelect = { onSelectLog(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutHistoryCard(item: WorkoutHistoryItem, onSelect: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.dayOfWeekLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.hasRecord) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.MilitaryTech, contentDescription = null)
                            Text(
                                text = "Рекорд",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = item.workoutName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.phaseName?.let { phase ->
                    AssistChip(
                        onClick = {},
                        label = { Text(text = phase) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(text = item.statusLabel) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusContainerColor(item.status),
                        labelColor = statusContentColor(item.status)
                    )
                )
            }

            HistoryMetricsRow(item)

            item.rating?.let { rating ->
                RatingRow(rating = rating)
            }

            item.notesPreview?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onSelect, modifier = Modifier.align(Alignment.End)) {
                Text("Подробнее")
            }
        }
    }
}

@Composable
private fun HistoryMetricsRow(item: WorkoutHistoryItem) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item.durationLabel?.let {
            HistoryMetric(label = "Длительность", value = it)
        }
        item.volumeLabel?.let {
            HistoryMetric(label = "Объём", value = it)
        }
        HistoryMetric(
            label = "Подходы/упражнения",
            value = "${item.setsCount} / ${item.exercisesCount}"
        )
    }
}

@Composable
private fun HistoryMetric(label: String, value: String) {
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
private fun RatingRow(rating: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val safeRating = rating.coerceIn(1, 5)
        repeat(5) { index ->
            if (index < safeRating) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            } else {
                Icon(Icons.Outlined.StarBorder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HistoryEmptyState(
    modifier: Modifier,
    message: String,
    showStartButton: Boolean,
    onStartFirstWorkout: () -> Unit
) {
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
        if (showStartButton) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onStartFirstWorkout) {
                Text("Начать первую тренировку")
            }
        }
    }
}

@Composable
private fun HistoryErrorState(modifier: Modifier, message: String, onRetry: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Повторить")
        }
    }
}

@Composable
private fun statusContainerColor(status: WorkoutHistoryStatus): Color {
    val colors = MaterialTheme.colorScheme
    return when (status) {
        WorkoutHistoryStatus.COMPLETED -> colors.tertiaryContainer
        WorkoutHistoryStatus.INCOMPLETE -> colors.errorContainer
        WorkoutHistoryStatus.UNKNOWN -> colors.surfaceVariant
    }
}

@Composable
private fun statusContentColor(status: WorkoutHistoryStatus): Color {
    val colors = MaterialTheme.colorScheme
    return when (status) {
        WorkoutHistoryStatus.COMPLETED -> colors.onTertiaryContainer
        WorkoutHistoryStatus.INCOMPLETE -> colors.onErrorContainer
        WorkoutHistoryStatus.UNKNOWN -> colors.onSurfaceVariant
    }
}
