package com.beast.shared.repository

import com.beast.shared.model.*
import kotlinx.coroutines.flow.Flow

interface ProgramRepository {
    suspend fun getAll(): List<Program>
    suspend fun getById(id: String): Program?
    suspend fun upsert(program: Program)
    suspend fun delete(id: String)
}

interface WorkoutDayRepository {
    suspend fun getByProgram(programId: String): List<WorkoutDay>
    suspend fun upsert(day: WorkoutDay)
    suspend fun delete(id: String)
}

interface ExerciseRepository {
    suspend fun getAll(): List<Exercise>
    suspend fun getByIds(ids: List<String>): List<Exercise>
    suspend fun upsert(exercise: Exercise)
    suspend fun delete(id: String)
}

interface WorkoutLogRepository {
    suspend fun getLogsForProgram(programId: String): List<WorkoutLog>
    suspend fun getLogsForDay(programId: String, dayIndex: Int): List<WorkoutLog>
    suspend fun upsert(log: WorkoutLog, sets: List<SetLog>)
}

interface SettingsRepository {
    fun units(): Flow<Units>
    suspend fun setUnits(units: Units)
    fun onboardingCompleted(): Flow<Boolean>
    suspend fun setOnboardingCompleted(value: Boolean)
    // Акцентный цвет (HEX)
    fun accentColor(): Flow<String>
    suspend fun setAccentColor(hex: String)
    // Выбранная активная программа (id)
    fun activeProgramId(): Flow<String?>
    suspend fun setActiveProgramId(id: String?)
    // Новое: дата старта активной программы (UTC millis, начало дня)
    fun activeProgramStartDate(): Flow<Long?>
    suspend fun setActiveProgramStartDate(value: Long?)
}

interface MeasurementRepository {
    suspend fun getAll(): List<Measurement>
    suspend fun upsert(measurement: Measurement)
    suspend fun delete(id: String)
}

interface PhotoProgressRepository {
    suspend fun getAll(): List<PhotoProgress>
    suspend fun upsert(photo: PhotoProgress)
    suspend fun delete(id: String)
}
