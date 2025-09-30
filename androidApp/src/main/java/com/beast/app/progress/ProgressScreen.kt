package com.beast.app.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProgressScreen(vm: ProgressViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsState()

    var weightInput by remember { mutableStateOf("") }
    var photoAngle by remember { mutableStateOf("front") }
    var photoUri by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Прогресс", style = MaterialTheme.typography.titleLarge) }
        item { Spacer(Modifier.height(12.dp)) }

        // Ввод веса
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { s -> weightInput = s.filter { it.isDigit() || it == '.' } },
                    label = { Text("Вес (${state.unitsLabel})") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    val w = weightInput.toDoubleOrNull()
                    vm.addMeasurement(w)
                    weightInput = ""
                }) { Text("Добавить") }
            }
        }

        // График веса
        if (state.measurements.isNotEmpty()) {
            item { Spacer(Modifier.height(16.dp)) }
            item { Text("График веса", style = MaterialTheme.typography.titleMedium) }
            item { Spacer(Modifier.height(8.dp)) }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    val points = remember(state.measurements) {
                        state.measurements.filter { it.weight != null }.sortedBy { it.date }
                    }
                    val weights = points.mapNotNull { it.weight }
                    val minW = weights.minOrNull() ?: 0.0
                    val maxW = weights.maxOrNull() ?: 0.0
                    val minX = points.firstOrNull()?.date ?: 0L
                    val maxX = points.lastOrNull()?.date ?: 1L
                    val rangeW = (maxW - minW).let { if (it == 0.0) 1.0 else it }
                    val rangeX = (maxX - minX).let { if (it == 0L) 1L else it }
                    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp).padding(8.dp)) {
                        if (points.size >= 2) {
                            var prev: Offset? = null
                            for (m in points) {
                                val x = ((m.date - minX).toFloat() / rangeX.toFloat()) * size.width
                                val y = size.height - (((m.weight!! - minW).toFloat() / rangeW.toFloat()) * size.height)
                                val cur = Offset(x, y)
                                prev?.let { p -> drawLine(Color(0xFF2E7D32), p, cur, strokeWidth = 4f) }
                                prev = cur
                            }
                        }
                    }
                }
            }
        }

        // Список замеров
        item { Spacer(Modifier.height(16.dp)) }
        item { Text("Замеры", style = MaterialTheme.typography.titleMedium) }
        items(state.measurements) { m ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                    Text(fmt.format(Date(m.date)))
                    Text("Вес: ${m.weight ?: "—"} ${state.unitsLabel}")
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { vm.deleteMeasurement(m.id) }) { Text("Удалить") }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
        item { Divider() }
        item { Spacer(Modifier.height(16.dp)) }

        // Фото прогресса (упрощённо: угол + URI)
        item { Text("Фото прогресса", style = MaterialTheme.typography.titleMedium) }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = photoAngle, onValueChange = { photoAngle = it }, label = { Text("Угол") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = photoUri, onValueChange = { photoUri = it }, label = { Text("URI фото") }, singleLine = true, modifier = Modifier.weight(2f))
                Button(onClick = {
                    if (photoUri.isNotBlank()) {
                        vm.addPhoto(photoAngle.ifBlank { "front" }, photoUri)
                        photoUri = ""
                    }
                }) { Text("Добавить") }
            }
        }
        items(state.photos) { p ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val fmt = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }
                    Text("${fmt.format(Date(p.date))} — ${p.angle}")
                    Text(p.fileUri, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { vm.deletePhoto(p.id) }) { Text("Удалить") }
                    }
                }
            }
        }
    }
}

