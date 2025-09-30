package com.beast.app.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beast.shared.model.Measurement
import com.beast.shared.model.PhotoProgress
import com.beast.shared.model.Units
import com.beast.shared.repository.MeasurementRepository
import com.beast.shared.repository.PhotoProgressRepository
import com.beast.shared.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val measurements: MeasurementRepository,
    private val photos: PhotoProgressRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    data class UiState(
        val measurements: List<Measurement> = emptyList(),
        val photos: List<PhotoProgress> = emptyList(),
        val unitsLabel: String = "kg",
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun load() {
        viewModelScope.launch {
            val ms = measurements.getAll()
            val ps = photos.getAll()
            val units = settings.units().first()
            val label = if (units == Units.Imperial) "lbs" else "kg"
            _state.value = UiState(
                measurements = ms.sortedByDescending { it.date },
                photos = ps.sortedByDescending { it.date },
                unitsLabel = label
            )
        }
    }

    fun addMeasurement(weight: Double?) {
        viewModelScope.launch {
            val m = Measurement(
                id = UUID.randomUUID().toString(),
                date = System.currentTimeMillis(),
                weight = weight,
                chest = null, waist = null, hips = null,
                additionalFields = emptyMap()
            )
            measurements.upsert(m)
            load()
        }
    }

    fun deleteMeasurement(id: String) {
        viewModelScope.launch {
            measurements.delete(id)
            load()
        }
    }

    fun addPhoto(angle: String, uri: String) {
        viewModelScope.launch {
            val p = PhotoProgress(
                id = UUID.randomUUID().toString(),
                date = System.currentTimeMillis(),
                angle = angle,
                fileUri = uri
            )
            photos.upsert(p)
            load()
        }
    }

    fun deletePhoto(id: String) {
        viewModelScope.launch {
            photos.delete(id)
            load()
        }
    }
}
