package com.beast.shared.data.room

import com.beast.shared.model.*
import com.beast.shared.repository.*

class ProgramRepositoryImpl(private val db: FitDatabase) : ProgramRepository {
    private val dao = db.programDao()
    override suspend fun getAll(): List<Program> = dao.getAll().map { it.toModel() }
    override suspend fun getById(id: String): Program? = dao.getById(id)?.toModel()
    override suspend fun upsert(program: Program) { dao.upsert(program.toEntity()) }
    override suspend fun delete(id: String) { dao.delete(id) }
}

class WorkoutDayRepositoryImpl(private val db: FitDatabase) : WorkoutDayRepository {
    private val dao = db.workoutDayDao()
    override suspend fun getByProgram(programId: String): List<WorkoutDay> = dao.getByProgram(programId).map { it.toModel() }
    override suspend fun upsert(day: WorkoutDay) { dao.upsert(day.toEntity()) }
    override suspend fun delete(id: String) { dao.delete(id) }
}

class ExerciseRepositoryImpl(private val db: FitDatabase) : ExerciseRepository {
    private val dao = db.exerciseDao()
    override suspend fun getAll(): List<Exercise> = dao.getAll().map { it.toModel() }
    override suspend fun getByIds(ids: List<String>): List<Exercise> = dao.getByIds(ids).map { it.toModel() }
    override suspend fun upsert(exercise: Exercise) { dao.upsert(exercise.toEntity()) }
    override suspend fun delete(id: String) { dao.delete(id) }
}

class WorkoutLogRepositoryImpl(
    private val db: FitDatabase
) : WorkoutLogRepository {
    private val logDao = db.workoutLogDao()
    private val setDao = db.setLogDao()

    override suspend fun getLogsForProgram(programId: String): List<WorkoutLog> =
        logDao.getForProgram(programId).map { it.toModel() }

    override suspend fun getLogsForDay(programId: String, dayIndex: Int): List<WorkoutLog> =
        logDao.getForDay(programId, dayIndex).map { it.toModel() }

    override suspend fun upsert(log: WorkoutLog, sets: List<SetLog>) {
        logDao.upsert(log.toEntity())
        setDao.upsertAll(sets.map { it.toEntity() })
    }
}

