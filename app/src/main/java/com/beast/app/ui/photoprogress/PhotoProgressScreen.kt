package com.beast.app.ui.photoprogress

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import com.beast.app.utils.DateFormatting
import java.util.Locale

@Composable
fun PhotoProgressRoute(
    onBack: () -> Unit,
    viewModel: PhotoProgressViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PhotoProgressScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onAddPhoto = viewModel::addPhoto,
        onDeletePhoto = viewModel::deletePhoto,
        onToggleSelection = viewModel::toggleSelection,
        onClearSelection = viewModel::clearSelection,
        onUnlockWithPasscode = viewModel::unlockWithPasscode,
        onUnlockWithBiometrics = viewModel::unlockWithBiometrics,
        onLock = viewModel::lock,
        onSetPasscode = viewModel::setPasscode,
        onClearPasscode = viewModel::clearPasscode,
        onSetBiometricEnabled = viewModel::setBiometricEnabled,
        onConsumeError = viewModel::markErrorConsumed
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PhotoProgressScreen(
    state: PhotoProgressUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAddPhoto: (PhotoAngle, String, LocalDate, String?) -> Unit,
    onDeletePhoto: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onUnlockWithPasscode: (String) -> Boolean,
    onUnlockWithBiometrics: () -> Unit,
    onLock: () -> Unit,
    onSetPasscode: (String) -> Unit,
    onClearPasscode: () -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onConsumeError: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedAngle by rememberSaveable { mutableStateOf(PhotoAngle.FRONT) }
    var selectedDateEpochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var notes by rememberSaveable { mutableStateOf("") }
    var previewPhoto by remember { mutableStateOf<PhotoItem?>(null) }
    var showSetPasscodeDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showComparisonSheet by remember { mutableStateOf(false) }

    val flattenedPhotos = remember(state.groups) {
        state.groups.flatMap { it.photos }
    }
    val selectedPhotos = remember(state.selectedPhotoIds, flattenedPhotos) {
        flattenedPhotos.filter { state.selectedPhotoIds.contains(it.id) }
    }
    LaunchedEffect(selectedPhotos.size) {
        showComparisonSheet = selectedPhotos.size == 2
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage
        if (!message.isNullOrBlank()) {
            scope.launch {
                snackbarHostState.showSnackbar(message)
                onConsumeError()
            }
        }
    }

    LaunchedEffect(state.privacyEnabled, state.locked) {
        if (state.privacyEnabled && state.locked) {
            showUnlockDialog = true
        } else {
            showUnlockDialog = false
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            context.contentResolver.persistPermission(uri)
            val date = LocalDate.ofEpochDay(selectedDateEpochDay)
            onAddPhoto(selectedAngle, uri.toString(), date, notes.trim().takeIf { it.isNotEmpty() })
            notes = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Фото-прогресс", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = {
                        onLock()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Обновить")
                    }
                    IconButton(onClick = {
                        if (state.privacyEnabled && !state.locked) {
                            onLock()
                        }
                    }) {
                        Icon(
                            imageVector = if (state.locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading && state.groups.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AddPhotoCard(
                        selectedAngle = selectedAngle,
                        onAngleChange = { selectedAngle = it },
                        selectedDate = LocalDate.ofEpochDay(selectedDateEpochDay),
                        onDateChange = { selectedDateEpochDay = it.toEpochDay() },
                        notes = notes,
                        onNotesChange = { notes = it },
                        onPickPhoto = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                }
                item {
                    PrivacySettingsCard(
                        privacyEnabled = state.privacyEnabled,
                        locked = state.locked,
                        biometricAvailable = state.biometricAvailable,
                        biometricEnabled = state.biometricEnabled,
                        onSetPasscode = { showSetPasscodeDialog = true },
                        onClearPasscode = onClearPasscode,
                        onToggleBiometric = onSetBiometricEnabled
                    )
                }
                if (!state.locked) {
                    itemsIndexed(items = state.groups, key = { index, group -> group.date.toEpochDay() }) { index, group ->
                        PhotoGroupSection(
                            group = group,
                            selectedPhotoIds = state.selectedPhotoIds,
                            onToggleSelection = onToggleSelection,
                            onPreview = { previewPhoto = it },
                            onDelete = onDeletePhoto,
                            isFirst = index == 0,
                            isLast = index == state.groups.lastIndex
                        )
                    }
                }
            }
        }
    }

    if (showUnlockDialog && state.privacyEnabled) {
        PrivacyUnlockDialog(
            biometricEnabled = state.biometricEnabled,
            onDismiss = { },
            onUnlock = { passcode -> onUnlockWithPasscode(passcode) },
            onBiometricRequested = {
                launchBiometricAuth(context) { success ->
                    if (success) {
                        onUnlockWithBiometrics()
                    }
                }
            }
        )
    }

    if (showSetPasscodeDialog) {
        SetPasscodeDialog(
            onDismiss = { showSetPasscodeDialog = false },
            onConfirm = { code ->
                onSetPasscode(code)
                showSetPasscodeDialog = false
            }
        )
    }

    if (previewPhoto != null) {
        PhotoPreviewDialog(
            photo = previewPhoto!!,
            onDismiss = { previewPhoto = null },
            onDelete = {
                onDeletePhoto(previewPhoto!!.id)
                previewPhoto = null
            }
        )
    }

    if (showComparisonSheet && selectedPhotos.size == 2) {
        PhotoComparisonSheet(
            photos = selectedPhotos,
            onDismiss = {
                onClearSelection()
                showComparisonSheet = false
            }
        )
    }
}

@Composable
private fun AddPhotoCard(
    selectedAngle: PhotoAngle,
    onAngleChange: (PhotoAngle) -> Unit,
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onPickPhoto: () -> Unit
) {
    val context = LocalContext.current
    val locale = remember { Locale.getDefault() }
    val formatter = remember(locale) { DateFormatting.dateFormatter(locale, "yMMMd") }
    val formattedDate = remember(selectedDate) { formatter.format(selectedDate) }

    Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 4.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Добавить фото", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (angle in PhotoAngle.entries) {
                    FilterChip(
                        selected = angle == selectedAngle,
                        onClick = { onAngleChange(angle) },
                        label = { Text(angle.displayName) }
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Дата",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = {
                    showDatePickerDialog(context, selectedDate) { onDateChange(it) }
                }) {
                    Text(formattedDate)
                }
            }
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Заметки") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            Button(onClick = onPickPhoto, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Выбрать фото")
            }
        }
    }
}

@Composable
private fun PrivacySettingsCard(
    privacyEnabled: Boolean,
    locked: Boolean,
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    onSetPasscode: () -> Unit,
    onClearPasscode: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit
) {
    Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.Security, contentDescription = null)
                Column {
                    Text("Приватность", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (privacyEnabled) {
                            if (locked) "Доступ заблокирован" else "Доступ открыт"
                        } else "Защита отключена",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!privacyEnabled) {
                Button(onClick = onSetPasscode) { Text("Установить код доступа") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onSetPasscode) { Text("Изменить код") }
                    OutlinedButton(onClick = onClearPasscode) { Text("Отключить") }
                }
                if (biometricAvailable) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Switch(checked = biometricEnabled, onCheckedChange = onToggleBiometric)
                        Text("Разрешить разблокировку по биометрии")
                    }
                } else {
                    Text(
                        text = "Биометрия недоступна на этом устройстве",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGroupSection(
    group: PhotoDayGroup,
    selectedPhotoIds: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onPreview: (PhotoItem) -> Unit,
    onDelete: (Long) -> Unit,
    isFirst: Boolean,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        Column(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(2.dp)
                    .background(if (isFirst) Color.Transparent else lineColor)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(2.dp)
                    .background(if (isLast) Color.Transparent else lineColor)
            )
        }
        Surface(
            modifier = Modifier
                .weight(1f),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(group.date.displayLabel(), style = MaterialTheme.typography.titleMedium)
                    group.weekLabel?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "${group.photos.size} фото",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(group.photos.size, key = { index -> group.photos[index].id }) { index ->
                        val photo = group.photos[index]
                        PhotoThumbnail(
                            photo = photo,
                            selected = selectedPhotoIds.contains(photo.id),
                            onToggleSelection = onToggleSelection,
                            onPreview = onPreview,
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoThumbnail(
    photo: PhotoItem,
    selected: Boolean,
    onToggleSelection: (Long) -> Unit,
    onPreview: (PhotoItem) -> Unit,
    onDelete: (Long) -> Unit
) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = {
                    if (selected) {
                        onToggleSelection(photo.id)
                    } else {
                        onPreview(photo)
                    }
                },
                onLongClick = { onToggleSelection(photo.id) }
            )
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
        ) {
            IconButton(onClick = { onDelete(photo.id) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            shape = MaterialTheme.shapes.small,
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Text(
                text = photo.angle.shortLabel(),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PhotoPreviewDialog(
    photo: PhotoItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            OutlinedButton(onClick = onDelete) {
                Text("Удалить")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Закрыть") }
        },
        title = {
            Text("${photo.angle.displayName} — ${photo.date.displayLabel()}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(model = photo.uri, contentDescription = null, modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(MaterialTheme.shapes.medium), contentScale = ContentScale.Crop)
                photo.notes?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoComparisonSheet(
    photos: List<PhotoItem>,
    onDismiss: () -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }
    val (before, after) = photos.sortedBy { it.createdAt }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Сравнение", style = MaterialTheme.typography.titleLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("До: ${before.date.displayLabel()}")
                Text("После: ${after.date.displayLabel()}")
            }
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)) {
                Box(modifier = Modifier.weight(sliderPosition.coerceIn(0.1f, 0.9f))) {
                    AsyncImage(model = before.uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Box(modifier = Modifier.weight((1f - sliderPosition).coerceIn(0.1f, 0.9f))) {
                    AsyncImage(model = after.uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            Slider(value = sliderPosition, onValueChange = { sliderPosition = it })
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Закрыть")
            }
        }
    }
}

@Composable
private fun PrivacyUnlockDialog(
    biometricEnabled: Boolean,
    onDismiss: () -> Unit,
    onUnlock: (String) -> Boolean,
    onBiometricRequested: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            Button(onClick = {
                if (!onUnlock(passcode)) {
                    error = "Неверный код"
                } else {
                    passcode = ""
                    error = null
                    onDismiss()
                }
            }) {
                Text("Разблокировать")
            }
        },
        dismissButton = {
            if (biometricEnabled) {
                OutlinedButton(onClick = onBiometricRequested) {
                    Text("Биометрия")
                }
            }
        },
        title = { Text("Введите код доступа") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it.take(8) },
                    label = { Text("Код") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}

@Composable
private fun SetPasscodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                if (passcode.length < 4) {
                    error = "Минимум 4 цифры"
                    return@Button
                }
                if (passcode != confirmation) {
                    error = "Коды не совпадают"
                    return@Button
                }
                onConfirm(passcode)
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Отмена") }
        },
        title = { Text("Установить код") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it.take(8) },
                    label = { Text("Код") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it.take(8) },
                    label = { Text("Повторите код") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}

private fun showDatePickerDialog(context: android.content.Context, date: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val zone = ZoneId.systemDefault()
    val listener = android.app.DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
        onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
    }
    val picker = android.app.DatePickerDialog(
        context,
        listener,
        date.year,
        date.monthValue - 1,
        date.dayOfMonth
    )
    picker.show()
}

private fun android.content.ContentResolver.persistPermission(uri: Uri) {
    try {
        takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
        // Игнорируем: не все источники требуют persistable permission
    }
}

private fun launchBiometricAuth(context: android.content.Context, onResult: (Boolean) -> Unit) {
    val activity = context as? androidx.fragment.app.FragmentActivity ?: run {
        onResult(false)
        return
    }
    val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
    val prompt = androidx.biometric.BiometricPrompt(activity, executor, object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
            onResult(true)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            onResult(false)
        }

        override fun onAuthenticationFailed() {
            onResult(false)
        }
    })
    val info = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
        .setTitle("Разблокировка фото")
        .setSubtitle("Подтвердите личность")
        .setAllowedAuthenticators(PhotoProgressViewModel.BIOMETRIC_AUTHENTICATORS)
        .build()
    prompt.authenticate(info)
}
