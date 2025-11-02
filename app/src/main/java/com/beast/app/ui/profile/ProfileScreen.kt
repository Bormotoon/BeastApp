package com.beast.app.ui.profile

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Person
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.io.InputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.io.buffered

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onOpenPhotoProgress: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onUpdateAvatar = viewModel::updateAvatar,
        onSaveBasics = viewModel::saveProfileBasics,
        onStartDateChange = viewModel::updateStartDate,
        onAddWeight = viewModel::addWeightEntry,
        onWeightUnitChange = viewModel::updateWeightUnit,
        onAddMeasurement = viewModel::addMeasurement,
        onSelectMeasurementMetric = viewModel::selectMeasurementMetric,
        onOpenPhotoProgress = onOpenPhotoProgress
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onUpdateAvatar: (String?) -> Unit,
    onSaveBasics: (String, Double?, Int?, ProfileGender?) -> Unit,
    onStartDateChange: (LocalDate) -> Unit,
    onAddWeight: (Double) -> Unit,
    onWeightUnitChange: (String) -> Unit,
    onAddMeasurement: (LocalDate, MeasurementInput) -> Unit,
    onSelectMeasurementMetric: (MeasurementMetric) -> Unit,
    onOpenPhotoProgress: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Обновить")
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
                val info = state.info
                if (info == null) {
                    EmptyState(modifier = Modifier.padding(innerPadding))
                    return@Scaffold
                }
                ProfileContent(
                    state = state,
                    info = info,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onUpdateAvatar = onUpdateAvatar,
                    onSaveBasics = onSaveBasics,
                    onStartDateChange = onStartDateChange,
                    onAddWeight = onAddWeight,
                    onWeightUnitChange = onWeightUnitChange,
                    onAddMeasurement = onAddMeasurement,
                    onSelectMeasurementMetric = onSelectMeasurementMetric,
                    onOpenPhotoProgress = onOpenPhotoProgress
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState,
    info: ProfileInfo,
    modifier: Modifier,
    onUpdateAvatar: (String?) -> Unit,
    onSaveBasics: (String, Double?, Int?, ProfileGender?) -> Unit,
    onStartDateChange: (LocalDate) -> Unit,
    onAddWeight: (Double) -> Unit,
    onWeightUnitChange: (String) -> Unit,
    onAddMeasurement: (LocalDate, MeasurementInput) -> Unit,
    onSelectMeasurementMetric: (MeasurementMetric) -> Unit,
    onOpenPhotoProgress: () -> Unit
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf<ProfileGender?>(null) }
    var weightInput by rememberSaveable { mutableStateOf("") }
    var measurementDateEpochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var chestInput by rememberSaveable { mutableStateOf("") }
    var waistInput by rememberSaveable { mutableStateOf("") }
    var hipsInput by rememberSaveable { mutableStateOf("") }
    var bicepsLeftInput by rememberSaveable { mutableStateOf("") }
    var bicepsRightInput by rememberSaveable { mutableStateOf("") }
    var thighLeftInput by rememberSaveable { mutableStateOf("") }
    var thighRightInput by rememberSaveable { mutableStateOf("") }
    var calfLeftInput by rememberSaveable { mutableStateOf("") }
    var calfRightInput by rememberSaveable { mutableStateOf("") }
    fun filterNumeric(value: String) = value.filter { it.isDigit() || it == '.' || it == ',' }
    fun parseNumeric(value: String) = value.trim().replace(',', '.').toDoubleOrNull()

    LaunchedEffect(info) {
        name = info.name
        height = info.heightCm?.let { formatNumber(it) } ?: ""
        age = info.age?.toString() ?: ""
        gender = info.gender
    }

    LaunchedEffect(state.lastWeight) {
        weight = state.lastWeight?.let { formatNumber(it) } ?: ""
    }

    LaunchedEffect(state.lastMeasurement) {
        val last = state.lastMeasurement
        if (last != null) {
            chestInput = last.chest?.let(::formatNumber) ?: ""
            waistInput = last.waist?.let(::formatNumber) ?: ""
            hipsInput = last.hips?.let(::formatNumber) ?: ""
            bicepsLeftInput = last.bicepsLeft?.let(::formatNumber) ?: ""
            bicepsRightInput = last.bicepsRight?.let(::formatNumber) ?: ""
            thighLeftInput = last.thighsLeft?.let(::formatNumber) ?: ""
            thighRightInput = last.thighsRight?.let(::formatNumber) ?: ""
            calfLeftInput = last.calfLeft?.let(::formatNumber) ?: ""
            calfRightInput = last.calfRight?.let(::formatNumber) ?: ""
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileHeaderCard(
                info = info,
                context = context,
                name = name,
                onNameChange = { name = it },
                onUpdateAvatar = onUpdateAvatar,
                onStartDateChange = onStartDateChange,
                onWeightUnitChange = onWeightUnitChange
            )
        }
            item {
                PhotoProgressCard(onOpenPhotoProgress = onOpenPhotoProgress)
            }
        item {
            PhysicalParametersCard(
                weight = weight,
                weightUnit = info.weightUnit,
                onWeightChange = { value ->
                    weight = filterNumeric(value)
                },
                height = height,
                onHeightChange = { height = it },
                age = age,
                onAgeChange = { age = it },
                gender = gender,
                onGenderSelect = { gender = it },
                onSave = {
                    val weightValue = parseNumeric(weight)
                    val heightValue = parseNumeric(height)
                    val ageValue = age.trim().toIntOrNull()
                    onSaveBasics(name, heightValue, ageValue, gender)
                    if (weightValue != null) {
                        onAddWeight(weightValue)
                    }
                }
            )
        }
        item {
            WeightHistoryCard(
                history = state.weightHistory,
                weightUnit = info.weightUnit,
                lastWeight = state.lastWeight,
                weightInput = weightInput,
                onWeightInputChange = { value ->
                    weightInput = value.filter { it.isDigit() || it == '.' || it == ',' }
                },
                onAddWeight = {
                    val parsed = parseNumeric(weightInput)
                    if (parsed != null) {
                        onAddWeight(parsed)
                        weightInput = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    info: ProfileInfo,
    context: Context,
    name: String,
    onNameChange: (String) -> Unit,
    onUpdateAvatar: (String?) -> Unit,
    onStartDateChange: (LocalDate) -> Unit,
    onWeightUnitChange: (String) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        onUpdateAvatar(uri?.toString())
    }
    val avatarBitmap = rememberAvatarBitmap(context, info.avatarUri)
    val startDateLabel = info.startDateLabel

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .clickable {
                            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ) {
                    if (avatarBitmap != null) {
                        Image(bitmap = avatarBitmap, contentDescription = "Аватар", modifier = Modifier.fillMaxSize())
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(imageVector = Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Имя") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Дата старта", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = {
                            showDatePicker(context, info.startDate) { year, month, day ->
                                onStartDateChange(LocalDate.of(year, month + 1, day))
                            }
                        }) {
                            Text(startDateLabel)
                        }
                    }
                }
                if (info.avatarUri != null) {
                    IconButton(onClick = { onUpdateAvatar(null) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить фото")
                    }
                }
            }
            info.programName?.let {
                Text(text = "Текущая программа: $it", style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeightUnitChip(label = "КГ", selected = info.weightUnit.equals("kg", ignoreCase = true)) {
                    onWeightUnitChange("kg")
                }
                WeightUnitChip(label = "Фунты", selected = info.weightUnit.equals("lbs", ignoreCase = true)) {
                    onWeightUnitChange("lbs")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightUnitChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = if (selected) {
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else AssistChipDefaults.assistChipColors()
    )
}

@Composable
private fun PhysicalParametersCard(
    weight: String,
    weightUnit: String,
    onWeightChange: (String) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    age: String,
    onAgeChange: (String) -> Unit,
    gender: ProfileGender?,
    onGenderSelect: (ProfileGender?) -> Unit,
    onSave: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Физические параметры", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = weight,
                onValueChange = onWeightChange,
                label = { Text("Вес (${unitLabel(weightUnit)})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = height,
                onValueChange = onHeightChange,
                label = { Text("Рост (см)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = age,
                onValueChange = onAgeChange,
                label = { Text("Возраст") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileGender.entries.forEach { option ->
                    FilterChip(
                        selected = option == gender,
                        onClick = {
                            onGenderSelect(if (option == gender) null else option)
                        },
                        label = { Text(option.displayName) },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }
            Button(onClick = onSave, modifier = Modifier.align(Alignment.End)) {
                Text("Сохранить")
            }
        }
    }
}

@Composable
private fun WeightHistoryCard(
    history: List<WeightPoint>,
    weightUnit: String,
    lastWeight: Double?,
    weightInput: String,
    onWeightInputChange: (String) -> Unit,
    onAddWeight: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "История веса", style = MaterialTheme.typography.titleMedium)
            if (history.isEmpty()) {
                Text(
                    text = "Добавьте первую запись, чтобы увидеть динамику",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                WeightTrendChart(history, weightUnit)
                lastWeight?.let {
                    Text(
                        text = "Последняя запись: ${formatNumber(it)} ${unitLabel(weightUnit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = onWeightInputChange,
                    label = { Text("Новый вес (${unitLabel(weightUnit)})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = onAddWeight, enabled = weightInput.isNotBlank()) {
                    Text("Добавить")
                }
            }
        }
    }
}

@Composable
private fun MeasurementsCard(
    measurementDate: LocalDate,
    onMeasurementDateChange: (LocalDate) -> Unit,
    chest: String,
    onChestChange: (String) -> Unit,
    waist: String,
    onWaistChange: (String) -> Unit,
    hips: String,
    onHipsChange: (String) -> Unit,
    bicepsLeft: String,
    onBicepsLeftChange: (String) -> Unit,
    bicepsRight: String,
    onBicepsRightChange: (String) -> Unit,
    thighLeft: String,
    onThighLeftChange: (String) -> Unit,
    thighRight: String,
    onThighRightChange: (String) -> Unit,
    calfLeft: String,
    onCalfLeftChange: (String) -> Unit,
    calfRight: String,
    onCalfRightChange: (String) -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    history: List<MeasurementPoint>,
    selectedMetric: MeasurementMetric,
    onMetricSelect: (MeasurementMetric) -> Unit
) {
    val context = LocalContext.current
    val locale = remember { Locale.getDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", locale) }
    val formattedDate = remember(measurementDate) { dateFormatter.format(measurementDate) }
    val chipScroll = rememberScrollState()

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Измерения тела", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Дата",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = {
                    showDatePicker(context, measurementDate) { year, month, day ->
                        onMeasurementDateChange(LocalDate.of(year, month + 1, day))
                    }
                }) {
                    Text(formattedDate)
                }
            }
            MeasurementRow(
                leftLabel = "Грудь (см)",
                leftValue = chest,
                onLeftChange = onChestChange,
                rightLabel = "Талия (см)",
                rightValue = waist,
                onRightChange = onWaistChange
            )
            MeasurementRow(
                leftLabel = "Бёдра (см)",
                leftValue = hips,
                onLeftChange = onHipsChange,
                rightLabel = "Бицепс L (см)",
                rightValue = bicepsLeft,
                onRightChange = onBicepsLeftChange
            )
            MeasurementRow(
                leftLabel = "Бицепс R (см)",
                leftValue = bicepsRight,
                onLeftChange = onBicepsRightChange,
                rightLabel = "Бедро L (см)",
                rightValue = thighLeft,
                onRightChange = onThighLeftChange
            )
            MeasurementRow(
                leftLabel = "Бедро R (см)",
                leftValue = thighRight,
                onLeftChange = onThighRightChange,
                rightLabel = "Голень L (см)",
                rightValue = calfLeft,
                onRightChange = onCalfLeftChange
            )
            MeasurementRow(
                leftLabel = "Голень R (см)",
                leftValue = calfRight,
                onLeftChange = onCalfRightChange,
                rightLabel = null,
                rightValue = null,
                onRightChange = null
            )
            Button(onClick = onSave, enabled = canSave, modifier = Modifier.align(Alignment.End)) {
                Text("Сохранить")
            }
            Text(text = "История", style = MaterialTheme.typography.titleMedium)
            if (history.isEmpty()) {
                Text(
                    text = "Добавьте измерения, чтобы увидеть динамику",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier
                        .horizontalScroll(chipScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (metric in MeasurementMetric.entries) {
                        FilterChip(
                            selected = metric == selectedMetric,
                            onClick = { onMetricSelect(metric) },
                            label = { Text(metric.label) }
                        )
                    }
                }
                MeasurementTrendChart(history = history, metric = selectedMetric)
            }
        }
    }
}

@Composable
private fun PhotoProgressCard(onOpenPhotoProgress: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Фото-прогресс", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Отслеживайте визуальные изменения с помощью галереи прогресса и сравнения снимков.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onOpenPhotoProgress, modifier = Modifier.align(Alignment.End)) {
                Text("Открыть галерею")
            }
        }
    }
}

@Composable
private fun MeasurementRow(
    leftLabel: String,
    leftValue: String,
    onLeftChange: (String) -> Unit,
    rightLabel: String?,
    rightValue: String?,
    onRightChange: ((String) -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MeasurementField(label = leftLabel, value = leftValue, onValueChange = onLeftChange, modifier = Modifier.weight(1f))
        if (rightLabel != null && rightValue != null && onRightChange != null) {
            MeasurementField(label = rightLabel, value = rightValue, onValueChange = onRightChange, modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MeasurementField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

@Composable
private fun MeasurementTrendChart(history: List<MeasurementPoint>, metric: MeasurementMetric) {
    val entries = history.mapIndexedNotNull { index, point ->
        val value = metric.accessor(point)
        if (value != null) Entry(index.toFloat(), value.toFloat()) else null
    }
    if (entries.isEmpty()) {
        Text(
            text = "Недостаточно данных для графика",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val locale = Locale.getDefault()
    val formatter = DateTimeFormatter.ofPattern("d MMM", locale)
    val labels = history.map { formatter.format(it.date) }
    val lineColor = MaterialTheme.colorScheme.secondary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                axisLeft.setDrawGridLines(false)
                setTouchEnabled(false)
            }
        },
        update = { chart ->
            val dataSet = LineDataSet(entries, null).apply {
                color = lineColor.toArgb()
                lineWidth = 2f
                circleRadius = 3.5f
                setCircleColor(lineColor.toArgb())
                valueTextColor = axisColor.toArgb()
                valueTextSize = 10f
                setDrawFilled(true)
                fillColor = lineColor.copy(alpha = 0.2f).toArgb()
            }
            val minValue = entries.minOf { it.y.toDouble() }
            chart.axisLeft.apply {
                textColor = axisColor.toArgb()
                axisMinimum = (minValue - 2.0).coerceAtLeast(0.0).toFloat()
            }
            chart.xAxis.apply {
                granularity = 1f
                textColor = axisColor.toArgb()
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        val index = value.toInt()
                        return labels.getOrNull(index) ?: ""
                    }
                }
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}

@Composable
private fun WeightTrendChart(history: List<WeightPoint>, weightUnit: String) {
    val locale = Locale.getDefault()
    val formatter = DateTimeFormatter.ofPattern("d MMM", locale)
    val labels = history.map { formatter.format(it.date) }
    val entries = history.mapIndexed { index, point ->
        Entry(index.toFloat(), point.weight.toFloat())
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                axisLeft.setDrawGridLines(false)
                setTouchEnabled(false)
            }
        },
        update = { chart ->
            val dataSet = LineDataSet(entries, null).apply {
                color = lineColor.toArgb()
                lineWidth = 2f
                circleRadius = 3.5f
                setCircleColor(lineColor.toArgb())
                valueTextColor = axisColor.toArgb()
                valueTextSize = 10f
                setDrawFilled(true)
                fillColor = lineColor.copy(alpha = 0.2f).toArgb()
            }
            val minWeight = history.minOfOrNull { it.weight } ?: 0.0
            chart.axisLeft.apply {
                textColor = axisColor.toArgb()
                axisMinimum = (minWeight - 2.0).coerceAtLeast(0.0).toFloat()
            }
            chart.xAxis.apply {
                granularity = 1f
                textColor = axisColor.toArgb()
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        val index = value.toInt()
                        return labels.getOrNull(index) ?: ""
                    }
                }
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}

private fun unitLabel(weightUnit: String): String {
    return if (weightUnit.equals("lbs", ignoreCase = true)) "фунтов" else "кг"
}

@Composable
private fun ErrorState(modifier: Modifier, message: String, onRetry: () -> Unit) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text("Повторить") }
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Профиль пока не настроен",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

private fun rememberAvatarBitmap(context: Context, uriString: String?): ImageBitmap? {
    if (uriString.isNullOrBlank()) return null
    val uri = Uri.parse(uriString)
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source).asImageBitmap()
        } else {
            context.contentResolver.openInputStream(uri)?.use(InputStream::buffered)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }
    }.getOrNull()
}

private fun showDatePicker(context: Context, initial: LocalDate, onDateSelected: (Int, Int, Int) -> Unit) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = initial.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(year, month, dayOfMonth)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun formatNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}
