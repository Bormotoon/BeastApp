package com.beast.shared.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.beast.shared.model.Units
import com.beast.shared.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {
    private val KEY_UNITS: Preferences.Key<String> = stringPreferencesKey("units")
    private val KEY_ONBOARDED: Preferences.Key<Boolean> = booleanPreferencesKey("onboarding_completed")
    private val KEY_ACCENT: Preferences.Key<String> = stringPreferencesKey("accent_hex")
    private val KEY_ACTIVE_PROGRAM: Preferences.Key<String> = stringPreferencesKey("active_program_id")
    private val KEY_ACTIVE_START: Preferences.Key<Long> = longPreferencesKey("active_program_start")

    override fun units(): Flow<Units> = context.dataStore.data.map { prefs ->
        val v = prefs[KEY_UNITS]
        when (v) {
            Units.Imperial.name -> Units.Imperial
            else -> Units.Metric
        }
    }

    override suspend fun setUnits(units: Units) {
        context.dataStore.edit { prefs ->
            prefs[KEY_UNITS] = units.name
        }
    }

    override fun onboardingCompleted(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDED] ?: false
    }

    override suspend fun setOnboardingCompleted(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDED] = value
        }
    }

    override fun accentColor(): Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACCENT] ?: "#2E7D32"
    }

    override suspend fun setAccentColor(hex: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCENT] = hex
        }
    }

    override fun activeProgramId(): Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROGRAM]
    }

    override suspend fun setActiveProgramId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id == null) prefs.remove(KEY_ACTIVE_PROGRAM) else prefs[KEY_ACTIVE_PROGRAM] = id
        }
    }

    override fun activeProgramStartDate(): Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_START]
    }

    override suspend fun setActiveProgramStartDate(value: Long?) {
        context.dataStore.edit { prefs ->
            if (value == null) prefs.remove(KEY_ACTIVE_START) else prefs[KEY_ACTIVE_START] = value
        }
    }
}
