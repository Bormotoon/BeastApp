package com.beast.shared.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs")
    suspend fun getAll(): List<ProgramEntity>

    @Query("SELECT * FROM programs WHERE id = :id")
    suspend fun getById(id: String): ProgramEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProgramEntity)

    @Query("DELETE FROM programs WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface WorkoutDayDao {
    @Query("SELECT * FROM workout_days WHERE programId = :programId ORDER BY dayIndex")
    suspend fun getByProgram(programId: String): List<WorkoutDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WorkoutDayEntity)

    @Query("DELETE FROM workout_days WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface WorkoutLogDao {
    @Query("SELECT * FROM workout_logs WHERE programId = :programId ORDER BY date DESC")
    suspend fun getForProgram(programId: String): List<WorkoutLogEntity>

    @Query("SELECT * FROM workout_logs WHERE programId = :programId AND dayIndex = :dayIndex ORDER BY date DESC")
    suspend fun getForDay(programId: String, dayIndex: Int): List<WorkoutLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WorkoutLogEntity)
}

@Dao
interface SetLogDao {
    @Query("SELECT * FROM set_logs WHERE workoutDayId = :workoutDayId ORDER BY timestamp")
    suspend fun getForWorkoutDay(workoutDayId: String): List<SetLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SetLogEntity>)
}

