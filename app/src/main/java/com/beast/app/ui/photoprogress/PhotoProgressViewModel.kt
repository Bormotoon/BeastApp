package com.beast.app.ui.photoprogress

import android.app.Application
import androidx.biometric.BiometricManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.db.ProgressPhotoEntity
import com.beast.app.data.db.UserProfileEntity
import com.beast.app.data.privacy.PhotoPrivacyManager
import com.beast.app.data.repo.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class PhotoProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DatabaseProvider.get(application.applicationContext)
    private val repository = ProfileRepository(database)
    private val privacyManager = PhotoPrivacyManager(application.applicationContext)
    private val biometricManager = BiometricManager.from(application.applicationContext)

    private val _uiState = MutableStateFlow(PhotoProgressUiState())
    val uiState: StateFlow<PhotoProgressUiState> = _uiState

    private var profileStartDate: LocalDate? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val profile = repository.getProfile()
                profileStartDate = profile?.startDate()
                val photos = repository.getProgressPhotos()
                val groups = buildGroups(photos)
                val privacyEnabled = privacyManager.isPasscodeSet()
                val biometricAvailable = biometricManager.canAuthenticate(BIOMETRIC_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
                val biometricEnabled = privacyManager.isBiometricEnabled() && biometricAvailable
                val shouldLock = when {
                    !privacyEnabled -> false
                    !_uiState.value.privacyEnabled -> true
                    else -> _uiState.value.locked
                }
                val selectedIds = _uiState.value.selectedPhotoIds.filter { id -> photos.any { it.id == id } }.toSet()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    groups = groups,
                    selectedPhotoIds = selectedIds,
                    privacyEnabled = privacyEnabled,
                    locked = shouldLock,
                    biometricAvailable = biometricAvailable,
                    biometricEnabled = biometricEnabled
                )
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Не удалось загрузить фото-прогресс") }
            }
        }
    }

    fun addPhoto(angle: PhotoAngle, uri: String, date: LocalDate, notes: String?) {
        viewModelScope.launch {
            val entity = ProgressPhotoEntity(
                dateEpochDay = date.toEpochDay(),
                angle = angle.storage,
                uri = uri,
                createdAtEpochMillis = System.currentTimeMillis(),
                notes = notes?.takeIf { it.isNotBlank() }
            )
            runCatching {
                repository.insertProgressPhoto(entity)
            }.onSuccess {
                reloadPhotos()
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Не удалось сохранить фото") }
            }
        }
    }

    fun deletePhoto(photoId: Long) {
        viewModelScope.launch {
            runCatching {
                repository.deleteProgressPhoto(photoId)
            }.onSuccess {
                reloadPhotos()
            }
        }
    }

    fun toggleSelection(photoId: Long) {
        _uiState.update { state ->
            val current = state.selectedPhotoIds
            val updated = when {
                current.contains(photoId) -> current - photoId
                current.size >= 2 -> current.drop(1).toSet() + photoId
                else -> current + photoId
            }
            state.copy(selectedPhotoIds = updated)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPhotoIds = emptySet()) }
    }

    fun markErrorConsumed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun setPasscode(passcode: String) {
        privacyManager.setPasscode(passcode)
        _uiState.update { it.copy(privacyEnabled = true, locked = true) }
    }

    fun clearPasscode() {
        privacyManager.clearPasscode()
        _uiState.update { it.copy(privacyEnabled = false, locked = false, biometricEnabled = false) }
    }

    fun unlockWithPasscode(passcode: String): Boolean {
        val success = privacyManager.verifyPasscode(passcode)
        if (success) {
            _uiState.update { it.copy(locked = false) }
        }
        return success
    }

    fun lock() {
        if (_uiState.value.privacyEnabled) {
            _uiState.update { it.copy(locked = true) }
        }
    }

    fun unlockWithBiometrics() {
        if (_uiState.value.privacyEnabled) {
            _uiState.update { it.copy(locked = false) }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        privacyManager.setBiometricEnabled(enabled)
        _uiState.update { it.copy(biometricEnabled = enabled) }
    }

    private suspend fun reloadPhotos() {
        val photos = repository.getProgressPhotos()
        val groups = buildGroups(photos)
        val selected = _uiState.value.selectedPhotoIds.filter { id -> photos.any { it.id == id } }.toSet()
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                groups = groups,
                selectedPhotoIds = selected
            )
        }
    }

    private fun buildGroups(photos: List<ProgressPhotoEntity>): List<PhotoDayGroup> {
        if (photos.isEmpty()) return emptyList()
        val groups = photos.groupBy { LocalDate.ofEpochDay(it.dateEpochDay) }
        val sortedDates = groups.keys.sortedDescending()
        return sortedDates.map { date ->
            val items = groups.getValue(date).sortedByDescending { it.createdAtEpochMillis }.map { it.toPhotoItem() }
            val weekLabel = profileStartDate?.let { start -> computeWeekLabel(start, date) }
            PhotoDayGroup(date = date, weekLabel = weekLabel, photos = items)
        }
    }

    private fun computeWeekLabel(start: LocalDate, date: LocalDate): String {
        val days = ChronoUnit.DAYS.between(start, date)
        if (days < 0) return "До старта"
        val weekIndex = (days / 7) + 1
        return "Неделя $weekIndex"
    }

    private fun ProgressPhotoEntity.toPhotoItem(): PhotoItem {
        val angleEnum = PhotoAngle.fromStorage(angle)
        return PhotoItem(
            id = id,
            angle = angleEnum,
            uri = uri,
            date = LocalDate.ofEpochDay(dateEpochDay),
            createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
            notes = notes
        )
    }

    private fun UserProfileEntity.startDate(): LocalDate {
        return LocalDate.ofEpochDay(startDateEpochDay)
    }

    companion object {
        const val BIOMETRIC_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}

data class PhotoProgressUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val groups: List<PhotoDayGroup> = emptyList(),
    val selectedPhotoIds: Set<Long> = emptySet(),
    val privacyEnabled: Boolean = false,
    val locked: Boolean = false,
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false
)

data class PhotoDayGroup(
    val date: LocalDate,
    val weekLabel: String?,
    val photos: List<PhotoItem>
)

data class PhotoItem(
    val id: Long,
    val angle: PhotoAngle,
    val uri: String,
    val date: LocalDate,
    val createdAt: Instant,
    val notes: String?
)

enum class PhotoAngle(val storage: String, val displayName: String) {
    FRONT("FRONT", "Фронт"),
    SIDE("SIDE", "Бок"),
    BACK("BACK", "Спина");

    companion object {
        fun fromStorage(value: String): PhotoAngle {
            return entries.firstOrNull { it.storage.equals(value, ignoreCase = true) } ?: FRONT
        }
    }

    fun shortLabel(): String = when (this) {
        FRONT -> "Front"
        SIDE -> "Side"
        BACK -> "Back"
    }
}

private val dateDisplayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

fun LocalDate.displayLabel(): String = format(dateDisplayFormatter)
