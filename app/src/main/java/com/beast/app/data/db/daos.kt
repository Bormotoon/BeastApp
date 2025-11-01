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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(workoutFavoriteEntity: WorkoutFavoriteEntity)

    @Query("DELETE FROM favorite_workouts WHERE workoutId = :workoutId")
    suspend fun removeFavorite(workoutId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_workouts WHERE workoutId = :workoutId)")
    suspend fun isFavorite(workoutId: String): Boolean

    @Query("SELECT workoutId FROM favorite_workouts")
    suspend fun getFavoriteWorkoutIds(): List<String>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id LIMIT 1")
    suspend fun getWorkout(id: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE id IN (:ids)")
    suspend fun getWorkoutsByIds(ids: List<String>): List<WorkoutEntity>

    @Query("SELECT * FROM exercise_in_workout WHERE workoutId = :workoutId ORDER BY orderIndex")
    suspend fun getExerciseMappings(workoutId: String): List<ExerciseInWorkoutEntity>

    @Query("SELECT * FROM exercise_in_workout WHERE workoutId IN (:workoutIds) ORDER BY workoutId, orderIndex")
    suspend fun getExerciseMappingsForWorkouts(workoutIds: List<String>): List<ExerciseInWorkoutEntity>

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

    @Query("SELECT * FROM workout_logs ORDER BY dateEpochMillis DESC")
    suspend fun getAllWorkoutLogs(): List<WorkoutLogEntity>

    @Query("SELECT * FROM workout_logs WHERE workoutId = :workoutId ORDER BY dateEpochMillis DESC")
    suspend fun getLogsForWorkout(workoutId: String): List<WorkoutLogEntity>

    @Query("SELECT * FROM set_logs WHERE workoutLogId = :workoutLogId ORDER BY setNumber")
    suspend fun getSetLogs(workoutLogId: String): List<SetLogEntity>

    @Query(
        """
        SELECT workoutLogId, COUNT(*) AS setCount, COUNT(DISTINCT exerciseId) AS exerciseCount
        FROM set_logs
        WHERE workoutLogId IN (:logIds)
        GROUP BY workoutLogId
        """
    )
    suspend fun getSetLogAggregates(logIds: List<String>): List<WorkoutLogSetAggregate>

    @Query(
        """
        SELECT workoutLogId, exerciseId,
            SUM(COALESCE(weight, 0) * COALESCE(reps, 0)) AS totalVolume,
            SUM(COALESCE(reps, 0)) AS totalReps
        FROM set_logs
        WHERE workoutLogId IN (:logIds)
        GROUP BY workoutLogId, exerciseId
        """
    )
    suspend fun getExerciseVolumeAggregates(logIds: List<String>): List<WorkoutLogExerciseAggregate>

    @Query("SELECT * FROM workout_logs WHERE dateEpochMillis BETWEEN :startMillis AND :endMillis ORDER BY dateEpochMillis")
    suspend fun getLogsBetween(startMillis: Long, endMillis: Long): List<WorkoutLogEntity>

    @Query(
        """
        SELECT wl.* FROM workout_logs wl
        INNER JOIN (
            SELECT workoutId, MAX(dateEpochMillis) AS maxDate
            FROM workout_logs
            WHERE workoutId IN (:workoutIds)
            GROUP BY workoutId
        ) grouped ON wl.workoutId = grouped.workoutId AND wl.dateEpochMillis = grouped.maxDate
        """
    )
    suspend fun getLatestLogsForWorkouts(workoutIds: List<String>): List<WorkoutLogEntity>
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

    @Query("SELECT DISTINCT dateEpochDay FROM personal_records WHERE dateEpochDay IN (:epochDays)")
    suspend fun getPersonalRecordDates(epochDays: List<Long>): List<Long>

    @Query(
        """
        SELECT pr.exerciseId,
               pr.weight,
               pr.reps,
               pr.estimated1RM AS estimatedOneRm,
               pr.dateEpochDay,
               ex.name AS exerciseName
        FROM personal_records pr
        INNER JOIN (
            SELECT exerciseId, MAX(estimated1RM) AS bestOneRm
            FROM personal_records
            GROUP BY exerciseId
        ) best ON pr.exerciseId = best.exerciseId AND pr.estimated1RM = best.bestOneRm
        LEFT JOIN exercises ex ON pr.exerciseId = ex.id
        ORDER BY pr.estimated1RM DESC
        LIMIT :limit
        """
    )
    suspend fun getTopPersonalRecords(limit: Int): List<PersonalRecordWithExercise>
}

data class WorkoutLogSetAggregate(
    val workoutLogId: String,
    val setCount: Int,
    val exerciseCount: Int
)

data class WorkoutLogExerciseAggregate(
    val workoutLogId: String,
    val exerciseId: String,
    val totalVolume: Double,
    val totalReps: Int
)

data class PersonalRecordWithExercise(
    val exerciseId: String,
    val weight: Double,
    val reps: Int,
    val estimatedOneRm: Double,
    val dateEpochDay: Long,
    val exerciseName: String?
)
