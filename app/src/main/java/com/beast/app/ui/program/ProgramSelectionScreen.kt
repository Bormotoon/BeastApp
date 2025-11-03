package com.beast.app.ui.program

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.db.UserProfileEntity
import com.beast.app.data.repo.ProfileRepository
import com.beast.app.utils.DateFormatting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

@Composable
fun ProgramSelectionScreen(onStartProgram: () -> Unit) {
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
        Text(text = stringResource(id = com.beast.app.R.string.select_program_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(id = com.beast.app.R.string.select_program_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp)
        )

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgramCard(
                title = stringResource(id = com.beast.app.R.string.huge_beast),
                subtitle = stringResource(id = com.beast.app.R.string.huge_subtitle),
                selected = selected.value == "huge",
                onClick = { selected.value = "huge" }
            )
            ProgramCard(
                title = stringResource(id = com.beast.app.R.string.lean_beast),
                subtitle = stringResource(id = com.beast.app.R.string.lean_subtitle),
                selected = selected.value == "lean",
                onClick = { selected.value = "lean" }
            )
        }

        val locale = remember(context) {
            val locales = context.resources.configuration.locales
            if (!locales.isEmpty) locales[0] else Locale.getDefault()
        }

        val startDateLabelState = remember(startDate.value, locale) {
            DateFormatting.format(startDate.value, locale, "yMMMMd", capitalizeFirst = true)
        }

        // Date picker
        Button(onClick = {
            showDatePicker(context) { year, month, day ->
                startDate.value = LocalDate.of(year, month + 1, day)
            }
        }, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = stringResource(id = com.beast.app.R.string.start_date, startDateLabelState))
        }

        // Weight unit toggle (simple)
        Row(modifier = Modifier.padding(top = 12.dp)) {
            Button(onClick = { weightUnit.value = "KG" }, modifier = Modifier.padding(end = 8.dp)) {
                Text(stringResource(id = com.beast.app.R.string.kg), color = if (weightUnit.value == "KG") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground)
            }
            Button(onClick = { weightUnit.value = "LBS" }) {
                Text(stringResource(id = com.beast.app.R.string.lbs), color = if (weightUnit.value == "LBS") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground)
            }
        }

        Button(onClick = {
            // Сохраняем выбор во SharedPreferences (быстрая локальная запись)
            saveProgramSelection(context, selected.value, startDate.value, weightUnit.value)

            // Также сохраняем профиль в базе данных (асинхронно) и генерируем календарь на основе фактической программы из БД
            scope.launch(Dispatchers.IO) {
                try {
                    val db = DatabaseProvider.get(context.applicationContext)
                    val profileRepo = ProfileRepository(db)

                    // Определяем и��я программы, по которому реально есть расписание в БД
                    var programNameForSchedule = when (selected.value) {
                        // Если позже будут импортированы шаблоны Body Beast, попробуем их человеко-читаемые имена
                        "huge" -> "Body Beast: Huge Beast"
                        "lean" -> "Body Beast: Lean Beast"
                        else -> selected.value
                    }

                    // Пытаемся получить расписание по ожидаемому имени
                    val programRepo = com.beast.app.data.repo.ProgramRepository(db)
                    var schedule = try { programRepo.getSchedule(programNameForSchedule) } catch (_: Throwable) { emptyList() }

                    // Фолбэк: если расписание не найдено, используем первую доступную программу в БД (например, демо "Sample CSV Program")
                    if (schedule.isEmpty()) {
                        try {
                            val all = db.programDao().getAllPrograms()
                            if (all.isNotEmpty()) {
                                programNameForSchedule = all.first().name
                                schedule = programRepo.getSchedule(programNameForSchedule)
                            }
                        } catch (_: Throwable) { /* ignore */ }
                    }

                    // Обновляем профиль, сохраняя фактическое имя текущей программы
                    val profile = UserProfileEntity(
                        name = "",
                        startDateEpochDay = startDate.value.toEpochDay(),
                        currentProgramId = programNameForSchedule,
                        weightUnit = weightUnit.value
                    )
                    profileRepo.upsertProfile(profile)

                    // Генерируем пользовательский календарь (epochDay -> workoutId) и сохраняем в SharedPreferences
                    try {
                        if (schedule.isNotEmpty()) {
                            val calendarJson = JSONObject()
                            for (entry in schedule) {
                                // entry.dayNumber предполагается 1-based
                                val epochDay = startDate.value.toEpochDay() + (entry.dayNumber - 1)
                                calendarJson.put(epochDay.toString(), entry.workoutId)
                            }
                            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putString("user_calendar", calendarJson.toString())
                                .putString("current_program_name", programNameForSchedule)
                                .apply()
                        }
                    } catch (_: Throwable) {
                        // ignore calendar generation errors for now
                    }
                } catch (_: Throwable) {
                    // Log later if needed
                }
            }

            onStartProgram()
        }, modifier = Modifier.padding(top = 24.dp)) {
            Text(stringResource(id = com.beast.app.R.string.start_program_button))
        }
    }
}

@Composable
private fun ProgramCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .semantics { contentDescription = "$title. $subtitle" },
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
    prefs.edit()
        .putString("selected_program", programSlug)
        .putString("weight_unit", weightUnit)
        .putLong("program_start_epoch_day", startDate.toEpochDay())
        .putBoolean("program_setup_done", true)
        .apply()
}
