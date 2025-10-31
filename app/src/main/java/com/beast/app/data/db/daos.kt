package com.beast.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

// Program / Phase / Workout

@Dao
interface ProgramDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgram(program: ProgramEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPhases(phases: List<PhaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkouts(workouts: List<WorkoutEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPhaseWorkouts(list: List<PhaseWorkoutCrossRefEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchedule(entries: List<ProgramScheduleEntity>)

    @Transaction
    @Query("SELECT * FROM programs WHERE name = :name LIMIT 1")
    suspend fun getProgram(name: String): ProgramEntity?

    @Transaction
    @Query("SELECT * FROM phases WHERE programName = :programName")
    suspend fun getPhases(programName: String): List<PhaseEntity>

    @Query("SELECT * FROM programs ORDER BY name")
    suspend fun getAllPrograms(): List<ProgramEntity>

    @Query("SELECT * FROM program_schedule WHERE programName = :programName ORDER BY dayNumber")
    suspend fun getSchedule(programName: String): List<ProgramScheduleEntity>

    @Query("SELECT * FROM phase_workout WHERE programName = :programName")
    suspend fun getPhaseWorkouts(programName: String): List<PhaseWorkoutCrossRefEntity>
}

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkout(workout: WorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExerciseInWorkout(list: List<ExerciseInWorkoutEntity>)

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id LIMIT 1")
    suspend fun getWorkout(id: String): WorkoutEntity?

    @Query("SELECT * FROM exercise_in_workout WHERE workoutId = :workoutId ORDER BY orderIndex")
    suspend fun getExerciseMappings(workoutId: String): List<ExerciseInWorkoutEntity>

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun getExercisesByIds(ids: List<String>): List<ExerciseEntity>
}

// Logs
@Dao
interface WorkoutLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(log: WorkoutLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLogs(logs: List<SetLogEntity>)

    @Query("SELECT * FROM workout_logs WHERE workoutId = :workoutId ORDER BY dateEpochMillis DESC")
    suspend fun getLogsForWorkout(workoutId: String): List<WorkoutLogEntity>

    @Query("SELECT * FROM set_logs WHERE workoutLogId = :workoutLogId ORDER BY setNumber")
    suspend fun getSetLogs(workoutLogId: String): List<SetLogEntity>
}

// Profile / measurements
@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyWeight(entry: BodyWeightEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(entry: BodyMeasurementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalRecord(entry: PersonalRecordEntity)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfile(): UserProfileEntity?
}
