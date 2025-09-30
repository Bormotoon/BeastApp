package com.beast.app.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beast.shared.model.Units
import com.beast.shared.model.Program

@Composable
fun OnboardingScreen(
    programs: List<Program> = emptyList(),
    onLoad: () -> Unit = {},
    onContinue: (Units, String?) -> Unit
) {
    var units by remember { mutableStateOf(Units.Metric) }
    var selectedProgramId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { onLoad() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Добро пожаловать в Beast App", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            Text("Единицы измерения:")
            Spacer(Modifier.height(8.dp))
            UnitsOption(
                selected = units == Units.Metric,
                title = "Метрические (кг, см)",
                onSelect = { units = Units.Metric }
            )
            UnitsOption(
                selected = units == Units.Imperial,
                title = "Имперские (lbs, inch)",
                onSelect = { units = Units.Imperial }
            )

            Spacer(Modifier.height(24.dp))
            Text("Выберите программу:")
            Spacer(Modifier.height(8.dp))
            if (programs.isEmpty()) {
                Text("Список программ пуст")
            } else {
                programs.forEach { p ->
                    ProgramOption(
                        selected = selectedProgramId == p.id,
                        title = p.title,
                        subtitle = p.description,
                        onSelect = { selectedProgramId = p.id }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = { onContinue(units, selectedProgramId) }) { Text("Продолжить") }
        }
    }
}

@Composable
private fun UnitsOption(selected: Boolean, title: String, onSelect: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(8.dp))
        Text(title)
    }
}

@Composable
private fun ProgramOption(selected: Boolean, title: String, subtitle: String, onSelect: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
