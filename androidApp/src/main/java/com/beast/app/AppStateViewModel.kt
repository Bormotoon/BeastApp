package com.beast.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beast.shared.model.Units
import com.beast.shared.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@HiltViewModel
class AppStateViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {
    val onboardingCompleted: Flow<Boolean> = settings.onboardingCompleted()
    val accentColor: Flow<String> = settings.accentColor()
    val units: Flow<Units> = settings.units()

    fun setAccent(hex: String) {
        viewModelScope.launch { settings.setAccentColor(hex) }
    }

    fun setUnits(value: Units) {
        viewModelScope.launch { settings.setUnits(value) }
    }
}
