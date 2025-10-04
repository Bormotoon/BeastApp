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

    // Advanced Mode keys
    private val KEY_ADVANCED_MODE: Preferences.Key<Boolean> = booleanPreferencesKey("advanced_mode")

    // Default workout values keys
    private val KEY_DEFAULT_REPS: Preferences.Key<Int> = androidx.datastore.preferences.core.intPreferencesKey("default_reps")
    private val KEY_DEFAULT_SETS: Preferences.Key<Int> = androidx.datastore.preferences.core.intPreferencesKey("default_sets")
    private val KEY_DEFAULT_REST: Preferences.Key<Int> = androidx.datastore.preferences.core.intPreferencesKey("default_rest")
    private val KEY_DEFAULT_WEIGHT: Preferences.Key<String> = stringPreferencesKey("default_weight")

    // Notes settings keys
    private val KEY_WORKOUT_NOTES: Preferences.Key<Boolean> = booleanPreferencesKey("workout_notes_enabled")
    private val KEY_SET_NOTES: Preferences.Key<Boolean> = booleanPreferencesKey("set_notes_enabled")

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

    // Advanced Mode implementation
    override fun advancedMode(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ADVANCED_MODE] ?: false
    }

    override suspend fun setAdvancedMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ADVANCED_MODE] = enabled
        }
    }

    // Screen visibility implementation
    override fun screenVisibility(screenName: String): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[booleanPreferencesKey("screen_visible_$screenName")] ?: true
    }

    override suspend fun setScreenVisibility(screenName: String, visible: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[booleanPreferencesKey("screen_visible_$screenName")] = visible
        }
    }

    // Default workout values implementation
    override fun defaultReps(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_REPS] ?: 10
    }

    override suspend fun setDefaultReps(reps: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_REPS] = reps
        }
    }

    override fun defaultSets(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_SETS] ?: 3
    }

    override suspend fun setDefaultSets(sets: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_SETS] = sets
        }
    }

    override fun defaultRest(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_REST] ?: 60
    }

    override suspend fun setDefaultRest(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_REST] = seconds
        }
    }

    override fun defaultWeight(): Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_WEIGHT]?.toDoubleOrNull() ?: 0.0
    }

    override suspend fun setDefaultWeight(weight: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_WEIGHT] = weight.toString()
        }
    }

    // Notes settings implementation
    override fun workoutNotesEnabled(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WORKOUT_NOTES] ?: true
    }

    override suspend fun setWorkoutNotesEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WORKOUT_NOTES] = enabled
        }
    }

    override fun setNotesEnabled(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SET_NOTES] ?: false
    }

    override suspend fun setSetNotesEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SET_NOTES] = enabled
        }
    }
}
