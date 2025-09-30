package com.beast.app.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    vm: CalendarViewModel = hiltViewModel(),
    onStartWorkout: (String, Int) -> Unit,
) {
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsState()

    val monthTitle = state.month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { vm.prevMonth() }) { Text("←") }
            Text(monthTitle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { vm.nextMonth() }) { Text("→") }
        }
        Spacer(Modifier.height(12.dp))

        // Weekday header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Вс","Пн","Вт","Ср","Чт","Пт","Сб").forEach { d ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(d, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(state.grid) { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { cell ->
                        val bg = when (cell.status) {
                            CalendarViewModel.DayStatus.Done -> Color(0xFF2E7D32).copy(alpha = 0.25f)
                            CalendarViewModel.DayStatus.Rest -> Color(0xFFAAAAAA).copy(alpha = 0.25f)
                            CalendarViewModel.DayStatus.Planned -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else -> Color.Transparent
                        }
                        val isSelected = state.selected?.date == cell.date
                        val border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clickable(enabled = cell.dayIndex != null) { vm.select(cell) },
                            shape = RoundedCornerShape(8.dp),
                            border = border,
                            colors = CardDefaults.cardColors(containerColor = bg)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.TopStart) {
                                val dayNumber = cell.date.dayOfMonth
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Text(dayNumber.toString(), style = MaterialTheme.typography.bodySmall)
                                    if (!cell.title.isNullOrBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(cell.title, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        // Selected day actions
        val selected = state.selected
        if (selected != null && selected.dayIndex != null && state.activeProgramId != null) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { onStartWorkout(state.activeProgramId!!, selected.dayIndex) }) { Text("Start") }
                OutlinedButton(onClick = { vm.markSelectedDone() }) { Text("Отметить выполненным") }
            }
        }
    }
}
