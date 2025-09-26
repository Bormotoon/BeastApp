package com.beast.shared.repository

import com.beast.shared.model.*

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

