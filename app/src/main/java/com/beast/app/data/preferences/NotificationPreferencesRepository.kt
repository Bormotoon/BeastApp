package com.beast.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime

private val Context.notificationPreferencesDataStore by preferencesDataStore(name = "notification_prefs")

class NotificationPreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.notificationPreferencesDataStore

    val preferencesFlow: Flow<NotificationPreferences> = dataStore.data.map { preferences ->
        NotificationPreferences(
            trainingReminder = preferences.toTrainingReminder()
        )
    }

    suspend fun setTrainingReminderEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[TRAINING_ENABLED] = enabled
        }
    }

    suspend fun setTrainingReminderTime(time: LocalTime) {
        dataStore.edit { prefs ->
            prefs[TRAINING_HOUR] = time.hour
            prefs[TRAINING_MINUTE] = time.minute
        }
    }

    suspend fun setTrainingReminderDays(days: Set<DayOfWeek>) {
        val serialized = days.sortedBy { it.value }.joinToString(separator = ",") { it.value.toString() }
        dataStore.edit { prefs ->
            prefs[TRAINING_DAYS] = serialized
        }
    }

    private fun Preferences.toTrainingReminder(): TrainingReminderPreferences {
        val enabled = this[TRAINING_ENABLED] ?: false
        val hour = (this[TRAINING_HOUR] ?: DEFAULT_HOUR).coerceIn(0, 23)
        val minute = (this[TRAINING_MINUTE] ?: DEFAULT_MINUTE).coerceIn(0, 59)
        val time = LocalTime.of(hour, minute)
        val days = parseDays(this[TRAINING_DAYS])
        return TrainingReminderPreferences(
            enabled = enabled,
            time = time,
            days = days
        )
    }

    private fun parseDays(serialized: String?): Set<DayOfWeek> {
        if (serialized.isNullOrBlank()) {
            return DayOfWeek.values().toSet()
        }
        val parsed = serialized.split(',')
            .mapNotNull { token -> token.toIntOrNull()?.let { DayOfWeek.of(it) } }
            .toSet()
        return if (parsed.isEmpty()) DayOfWeek.values().toSet() else parsed
    }

    companion object {
        private val TRAINING_ENABLED = booleanPreferencesKey("training_reminder_enabled")
        private val TRAINING_HOUR = intPreferencesKey("training_reminder_hour")
        private val TRAINING_MINUTE = intPreferencesKey("training_reminder_minute")
        private val TRAINING_DAYS = stringPreferencesKey("training_reminder_days")

        private const val DEFAULT_HOUR = 7
        private const val DEFAULT_MINUTE = 0
    }
}

data class NotificationPreferences(
    val trainingReminder: TrainingReminderPreferences = TrainingReminderPreferences()
)

data class TrainingReminderPreferences(
    val enabled: Boolean = false,
    val time: LocalTime = LocalTime.of(DEFAULT_HOUR, DEFAULT_MINUTE),
    val days: Set<DayOfWeek> = DayOfWeek.values().toSet()
) {
    companion object {
        private const val DEFAULT_HOUR = 7
        private const val DEFAULT_MINUTE = 0
    }
}
