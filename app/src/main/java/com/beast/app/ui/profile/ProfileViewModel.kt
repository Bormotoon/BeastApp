package com.beast.app.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beast.app.data.db.BodyMeasurementEntity
import com.beast.app.data.db.BodyWeightEntryEntity
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.db.UserProfileEntity
import com.beast.app.data.repo.ProfileRepository
import com.beast.app.data.repo.ProgramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseProvider.get(application.applicationContext)
    private val profileRepository = ProfileRepository(database)
    private val programRepository = ProgramRepository(database)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private var cachedProfile: UserProfileEntity? = null
    private var cachedWeights: List<BodyWeightEntryEntity> = emptyList()
    private var cachedMeasurements: List<BodyMeasurementEntity> = emptyList()

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val previousMetric = _uiState.value.selectedMeasurementMetric
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val profile = profileRepository.getProfile() ?: defaultProfile().also {
                    profileRepository.upsertProfile(it)
                }
                val weights = profileRepository.getBodyWeightHistory(limit = 120)
                val measurements = profileRepository.getMeasurements(limit = 120)
                val programName = profile.currentProgramId?.let {
                    programRepository.getProgramSummary(it)?.program?.name ?: it
                }
                cachedProfile = profile
                cachedWeights = weights
                cachedMeasurements = measurements
                val measurementPoints = measurements.toMeasurementPoints()
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    errorMessage = null,
                    info = profile.toInfo(programName),
                    weightHistory = weights.toChartPoints(),
                    lastWeight = weights.lastOrNull()?.weight,
                    measurementHistory = measurementPoints,
                    lastMeasurement = measurementPoints.lastOrNull(),
                    selectedMeasurementMetric = previousMetric
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Не удалось загрузить профиль"
                )
            }
        }
    }

    fun updateAvatar(uri: String?) {
        viewModelScope.launch { persistProfile { it.copy(avatarUri = uri) } }
    }

    fun updateStartDate(date: LocalDate) {
        viewModelScope.launch { persistProfile { it.copy(startDateEpochDay = date.toEpochDay()) } }
    }

    fun updateWeightUnit(unit: String) {
        viewModelScope.launch { persistProfile { it.copy(weightUnit = unit) } }
    }

    fun saveProfileBasics(name: String, heightCm: Double?, age: Int?, gender: ProfileGender?) {
        viewModelScope.launch {
            persistProfile {
                it.copy(
                    name = name.trim(),
                    heightCm = heightCm,
                    age = age,
                    gender = gender?.storageValue
                )
            }
        }
    }

    fun addWeightEntry(weight: Double) {
        viewModelScope.launch {
            val entry = BodyWeightEntryEntity(
                dateEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay(),
                weight = weight
            )
            runCatching {
                profileRepository.insertBodyWeight(entry)
            }.onSuccess {
                cachedWeights = profileRepository.getBodyWeightHistory(limit = 120)
                _uiState.value = _uiState.value.copy(
                    weightHistory = cachedWeights.toChartPoints(),
                    lastWeight = cachedWeights.lastOrNull()?.weight
                )
            }
        }
    }

    fun addMeasurement(date: LocalDate, measurement: MeasurementInput) {
        viewModelScope.launch {
            val entity = BodyMeasurementEntity(
                dateEpochDay = date.toEpochDay(),
                chest = measurement.chest,
                waist = measurement.waist,
                hips = measurement.hips,
                bicepsLeft = measurement.bicepsLeft,
                bicepsRight = measurement.bicepsRight,
                thighsLeft = measurement.thighsLeft,
                thighsRight = measurement.thighsRight,
                calfLeft = measurement.calfLeft,
                calfRight = measurement.calfRight
            )
            runCatching {
                profileRepository.insertMeasurement(entity)
            }.onSuccess {
                cachedMeasurements = profileRepository.getMeasurements(limit = 120)
                val measurementPoints = cachedMeasurements.toMeasurementPoints()
                _uiState.value = _uiState.value.copy(
                    measurementHistory = measurementPoints,
                    lastMeasurement = measurementPoints.lastOrNull()
                )
            }
        }
    }

    fun selectMeasurementMetric(metric: MeasurementMetric) {
        _uiState.value = _uiState.value.copy(selectedMeasurementMetric = metric)
    }

    private suspend fun persistProfile(block: (UserProfileEntity) -> UserProfileEntity) {
        val current = cachedProfile ?: defaultProfile()
        val updated = block(current)
        profileRepository.upsertProfile(updated)
        cachedProfile = updated
        val programName = updatedProgramName(updated)
        _uiState.value = _uiState.value.copy(
            info = updated.toInfo(programName = programName),
            lastWeight = cachedWeights.lastOrNull()?.weight
        )
    }

    private suspend fun updatedProgramName(profile: UserProfileEntity): String? {
        val id = profile.currentProgramId ?: return null
        return runCatching { programRepository.getProgramSummary(id)?.program?.name ?: id }.getOrNull()
    }

    private fun defaultProfile(): UserProfileEntity {
        val today = LocalDate.now(ZoneId.systemDefault())
        return UserProfileEntity(
            name = "",
            startDateEpochDay = today.toEpochDay(),
            currentProgramId = null,
            weightUnit = "kg"
        )
    }

    private fun UserProfileEntity.toInfo(programName: String?): ProfileInfo {
        val startDate = LocalDate.ofEpochDay(startDateEpochDay)
        return ProfileInfo(
            name = name,
            startDate = startDate,
            startDateLabel = startDate.format(dateFormatter),
            programName = programName,
            avatarUri = avatarUri,
            weightUnit = weightUnit,
            heightCm = heightCm,
            age = age,
            gender = gender?.let(ProfileGender::fromStorage)
        )
    }

    private fun List<BodyWeightEntryEntity>.toChartPoints(): List<WeightPoint> {
        if (isEmpty()) return emptyList()
        return map { entry ->
            WeightPoint(
                date = LocalDate.ofEpochDay(entry.dateEpochDay),
                weight = entry.weight
            )
        }
    }

    private fun List<BodyMeasurementEntity>.toMeasurementPoints(): List<MeasurementPoint> {
        if (isEmpty()) return emptyList()
        return map { entry ->
            MeasurementPoint(
                date = LocalDate.ofEpochDay(entry.dateEpochDay),
                chest = entry.chest,
                waist = entry.waist,
                hips = entry.hips,
                bicepsLeft = entry.bicepsLeft,
                bicepsRight = entry.bicepsRight,
                thighsLeft = entry.thighsLeft,
                thighsRight = entry.thighsRight,
                calfLeft = entry.calfLeft,
                calfRight = entry.calfRight
            )
        }
    }
}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val info: ProfileInfo? = null,
    val weightHistory: List<WeightPoint> = emptyList(),
    val lastWeight: Double? = null,
    val measurementHistory: List<MeasurementPoint> = emptyList(),
    val lastMeasurement: MeasurementPoint? = null,
    val selectedMeasurementMetric: MeasurementMetric = MeasurementMetric.CHEST
)

data class ProfileInfo(
    val name: String,
    val startDate: LocalDate,
    val startDateLabel: String,
    val programName: String?,
    val avatarUri: String?,
    val weightUnit: String,
    val heightCm: Double?,
    val age: Int?,
    val gender: ProfileGender?
)

data class WeightPoint(
    val date: LocalDate,
    val weight: Double
)

data class MeasurementPoint(
    val date: LocalDate,
    val chest: Double? = null,
    val waist: Double? = null,
    val hips: Double? = null,
    val bicepsLeft: Double? = null,
    val bicepsRight: Double? = null,
    val thighsLeft: Double? = null,
    val thighsRight: Double? = null,
    val calfLeft: Double? = null,
    val calfRight: Double? = null
)

data class MeasurementInput(
    val chest: Double? = null,
    val waist: Double? = null,
    val hips: Double? = null,
    val bicepsLeft: Double? = null,
    val bicepsRight: Double? = null,
    val thighsLeft: Double? = null,
    val thighsRight: Double? = null,
    val calfLeft: Double? = null,
    val calfRight: Double? = null
)

enum class MeasurementMetric(val label: String, val accessor: (MeasurementPoint) -> Double?) {
    CHEST("Грудь", { it.chest }),
    WAIST("Талия", { it.waist }),
    HIPS("Бёдра", { it.hips }),
    BICEPS_LEFT("Бицепс L", { it.bicepsLeft }),
    BICEPS_RIGHT("Бицепс R", { it.bicepsRight }),
    THIGH_LEFT("Бедро L", { it.thighsLeft }),
    THIGH_RIGHT("Бедро R", { it.thighsRight }),
    CALF_LEFT("Голень L", { it.calfLeft }),
    CALF_RIGHT("Голень R", { it.calfRight })
}

enum class ProfileGender(val storageValue: String, val displayName: String) {
    MALE("MALE", "Мужской"),
    FEMALE("FEMALE", "Женский"),
    OTHER("OTHER", "Другое");

    companion object {
        fun fromStorage(value: String): ProfileGender? {
            return entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) }
        }
    }
}
