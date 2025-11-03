package com.beast.app.ui.program

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.beast.app.data.repo.ProgramRepository
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
    val selectedProgram = remember { mutableStateOf("huge") }
    val startDate = remember { mutableStateOf(LocalDate.now()) }
    val weightUnit = remember { mutableStateOf("KG") }

    val locale = remember(context) {
        val locales = context.resources.configuration.locales
        if (!locales.isEmpty) locales[0] else Locale.getDefault()
    }
    val startDateLabel = remember(startDate.value, locale) {
        DateFormatting.format(startDate.value, locale, "yMMMMd", capitalizeFirst = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = com.beast.app.R.string.select_program_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = com.beast.app.R.string.select_program_subtitle),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProgramCard(
                modifier = Modifier.weight(1f),
                title = stringResource(id = com.beast.app.R.string.huge_beast),
                subtitle = stringResource(id = com.beast.app.R.string.huge_subtitle),
                selected = selectedProgram.value == "huge",
                onClick = { selectedProgram.value = "huge" }
            )
            ProgramCard(
                modifier = Modifier.weight(1f),
                title = stringResource(id = com.beast.app.R.string.lean_beast),
                subtitle = stringResource(id = com.beast.app.R.string.lean_subtitle),
                selected = selectedProgram.value == "lean",
                onClick = { selectedProgram.value = "lean" }
            )
        }

        Button(onClick = {
            showDatePicker(context) { year, month, day ->
                startDate.value = LocalDate.of(year, month + 1, day)
            }
        }) {
            Text(text = stringResource(id = com.beast.app.R.string.start_date, startDateLabel))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val activeColor = MaterialTheme.colorScheme.primary
            val inactiveColor = MaterialTheme.colorScheme.surface
            WeightChip(
                labelRes = com.beast.app.R.string.kg,
                selected = weightUnit.value == "KG",
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = { weightUnit.value = "KG" }
            )
            WeightChip(
                labelRes = com.beast.app.R.string.lbs,
                selected = weightUnit.value == "LBS",
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = { weightUnit.value = "LBS" }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = {
            saveProgramSelection(context, selectedProgram.value, startDate.value, weightUnit.value)
            scope.launch(Dispatchers.IO) {
                persistProfileAndCalendar(
                    context = context,
                    programSlug = selectedProgram.value,
                    startDate = startDate.value,
                    weightUnit = weightUnit.value
                )
            }
            onStartProgram()
        }, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = com.beast.app.R.string.start_program_button))
        }
    }
}

@Composable
private fun ProgramCard(modifier: Modifier, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$title. $subtitle" },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = contentColor)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = contentColor)
        }
    }
}

@Composable
private fun WeightChip(
    labelRes: Int,
    selected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    inactiveColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val background = if (selected) activeColor else inactiveColor
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Text(
        text = stringResource(id = labelRes),
        modifier = Modifier
            .background(background, shape = RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        color = content,
        style = MaterialTheme.typography.labelLarge
    )
}

private fun showDatePicker(context: Context, onDateSet: (year: Int, month: Int, day: Int) -> Unit) {
    val c = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, day -> onDateSet(year, month, day) },
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH),
        c.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun saveProgramSelection(context: Context, programSlug: String, startDate: LocalDate, weightUnit: String) {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putString("selected_program", programSlug)
        .putString("weight_unit", weightUnit)
        .putLong("program_start_epoch_day", startDate.toEpochDay())
        .apply()
}

private suspend fun persistProfileAndCalendar(
    context: Context,
    programSlug: String,
    startDate: LocalDate,
    weightUnit: String
) {
    try {
        val db = DatabaseProvider.get(context.applicationContext)
        val profileRepo = ProfileRepository(db)
        val programRepo = ProgramRepository(db)

        var resolvedProgramName = when (programSlug) {
            "huge" -> "Body Beast: Huge Beast"
            "lean" -> "Body Beast: Lean Beast"
            else -> programSlug
        }

        var schedule = runCatching { programRepo.getSchedule(resolvedProgramName) }.getOrDefault(emptyList())
        if (schedule.isEmpty()) {
            runCatching { db.programDao().getAllPrograms().firstOrNull() }.
                getOrNull()?.let { fallback ->
                    resolvedProgramName = fallback.name
                    schedule = runCatching { programRepo.getSchedule(resolvedProgramName) }.getOrDefault(emptyList())
                }
        }

        val profile = UserProfileEntity(
            name = "",
            startDateEpochDay = startDate.toEpochDay(),
            currentProgramId = resolvedProgramName,
            weightUnit = weightUnit
        )
        profileRepo.upsertProfile(profile)

        if (schedule.isNotEmpty()) {
            val calendarJson = JSONObject()
            schedule.forEach { entry ->
                val epochDay = startDate.toEpochDay() + (entry.dayNumber - 1)
                calendarJson.put(epochDay.toString(), entry.workoutId)
            }
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("user_calendar", calendarJson.toString())
                .putString("current_program_name", resolvedProgramName)
                .apply()
        }
    } catch (_: Throwable) {
        // TODO: add logging when diagnostics infrastructure is ready
    }
}
