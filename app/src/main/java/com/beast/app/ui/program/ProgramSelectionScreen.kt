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

        // No preset programs shown. User can import a program later.
        Text(
            text = stringResource(id = com.beast.app.R.string.select_program_subtitle),
            style = MaterialTheme.typography.bodyLarge
        )

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

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                // Start with an empty profile (no program selected)
                saveProgramSelection(context, "", startDate.value, weightUnit.value)
                scope.launch(Dispatchers.IO) {
                    persistProfileAndCalendar(
                        context = context,
                        programSlug = "",
                        startDate = startDate.value,
                        weightUnit = weightUnit.value
                    )
                }
                onStartProgram()
            }) {
                Text(text = stringResource(id = com.beast.app.R.string.start_program_button))
            }

            Button(onClick = {
                // TODO: navigate to import screen. For now behave same as start empty.
                saveProgramSelection(context, "", startDate.value, weightUnit.value)
                scope.launch(Dispatchers.IO) {
                    persistProfileAndCalendar(
                        context = context,
                        programSlug = "",
                        startDate = startDate.value,
                        weightUnit = weightUnit.value
                    )
                }
                onStartProgram()
            }) {
                Text(text = stringResource(id = com.beast.app.R.string.start_program_button))
            }
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

        // Do not assign or resolve any built-in/demo program here.
        // Create an empty profile with no current program and leave calendar unset.
        val profile = UserProfileEntity(
            name = "",
            startDateEpochDay = startDate.toEpochDay(),
            currentProgramId = "",
            weightUnit = weightUnit
        )
        profileRepo.upsertProfile(profile)
    } catch (_: Throwable) {
        // TODO: add logging when diagnostics infrastructure is ready
    }
}
