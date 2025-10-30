package com.beast.app.ui.program

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.db.UserProfileEntity
import com.beast.app.data.repo.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

@Composable
fun ProgramSelectionScreen(onStartProgram: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selected = remember { mutableStateOf("huge") }
    val startDate = remember { mutableStateOf(LocalDate.now()) }
    val weightUnit = remember { mutableStateOf("KG") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Выберите вашу ��рограмму", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Huge Beast — фокус на массу\nLean Beast — масса + кардио",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp)
        )

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgramCard(
                title = "Huge Beast",
                subtitle = "Фокус на массу",
                selected = selected.value == "huge",
                onClick = { selected.value = "huge" }
            )
            ProgramCard(
                title = "Lean Beast",
                subtitle = "Масса + кардио",
                selected = selected.value == "lean",
                onClick = { selected.value = "lean" }
            )
        }

        // Date picker
        Button(onClick = {
            showDatePicker(context) { year, month, day ->
                startDate.value = LocalDate.of(year, month + 1, day)
            }
        }, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = "Дата начала: ${startDate.value}")
        }

        // Weight unit toggle (simple)
        Row(modifier = Modifier.padding(top = 12.dp)) {
            Button(onClick = { weightUnit.value = "KG" }, modifier = Modifier.padding(end = 8.dp)) {
                Text("Кг", color = if (weightUnit.value == "KG") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground)
            }
            Button(onClick = { weightUnit.value = "LBS" }) {
                Text("Фунты", color = if (weightUnit.value == "LBS") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground)
            }
        }

        Button(onClick = {
            // Сохраняем выбор во SharedPreferences (быстрая локальная запись)
            saveProgramSelection(context, selected.value, startDate.value, weightUnit.value)

            // Также сохраняем профиль в базе данных (асинхронно)
            scope.launch(Dispatchers.IO) {
                try {
                    val db = DatabaseProvider.get(context.applicationContext)
                    val profileRepo = ProfileRepository(db)
                    val profile = UserProfileEntity(
                        name = "",
                        startDateEpochDay = startDate.value.toEpochDay(),
                        currentProgramId = selected.value,
                        weightUnit = weightUnit.value
                    )
                    profileRepo.upsertProfile(profile)
                } catch (t: Throwable) {
                    // Log later if needed
                }
            }

            onStartProgram()
        }, modifier = Modifier.padding(top = 24.dp)) {
            Text("Начать программу")
        }
    }
}

@Composable
private fun ProgramCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

private fun showDatePicker(context: Context, onDateSet: (year: Int, month: Int, day: Int) -> Unit) {
    val c = Calendar.getInstance()
    val dialog = DatePickerDialog(context, { _, year, month, day -> onDateSet(year, month, day) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    dialog.show()
}

private fun saveProgramSelection(context: Context, programSlug: String, startDate: LocalDate, weightUnit: String) {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("selected_program", programSlug).putString("weight_unit", weightUnit).putLong("program_start_epoch_day", startDate.toEpochDay()).apply()
}
