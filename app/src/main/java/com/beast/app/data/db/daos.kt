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
    suspend fun upsertPrograms(programs: List<ProgramEntity>)

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

    @Query("SELECT * FROM phases ORDER BY programName, name")
    suspend fun getAllPhases(): List<PhaseEntity>

    @Query("SELECT * FROM programs ORDER BY name")
    suspend fun getAllPrograms(): List<ProgramEntity>

    @Query("SELECT * FROM program_schedule WHERE programName = :programName ORDER BY dayNumber")
    suspend fun getSchedule(programName: String): List<ProgramScheduleEntity>

    @Query("SELECT * FROM program_schedule ORDER BY programName, dayNumber")
    suspend fun getAllSchedules(): List<ProgramScheduleEntity>

    @Query("SELECT * FROM phase_workout WHERE programName = :programName")
    suspend fun getPhaseWorkouts(programName: String): List<PhaseWorkoutCrossRefEntity>

    @Query("SELECT * FROM phase_workout ORDER BY programName, phaseName, workoutId")
    suspend fun getAllPhaseWorkouts(): List<PhaseWorkoutCrossRefEntity>
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorites(favorites: List<WorkoutFavoriteEntity>)

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

    @Query("SELECT * FROM workouts ORDER BY id")
    suspend fun getAllWorkouts(): List<WorkoutEntity>

    @Query("SELECT * FROM exercise_in_workout WHERE workoutId = :workoutId ORDER BY orderIndex")
    suspend fun getExerciseMappings(workoutId: String): List<ExerciseInWorkoutEntity>

    @Query("SELECT * FROM exercise_in_workout WHERE workoutId IN (:workoutIds) ORDER BY workoutId, orderIndex")
    suspend fun getExerciseMappingsForWorkouts(workoutIds: List<String>): List<ExerciseInWorkoutEntity>

    @Query("SELECT * FROM exercise_in_workout ORDER BY workoutId, orderIndex")
    suspend fun getAllExerciseMappings(): List<ExerciseInWorkoutEntity>

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun getExercisesByIds(ids: List<String>): List<ExerciseEntity>

    @Query("SELECT * FROM exercises ORDER BY id")
    suspend fun getAllExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM favorite_workouts ORDER BY addedAtEpochMillis DESC")
    suspend fun getAllFavorites(): List<WorkoutFavoriteEntity>
}

// Logs
@Dao
interface WorkoutLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(log: WorkoutLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLogs(logs: List<WorkoutLogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLogs(logs: List<SetLogEntity>)

    @Query("SELECT * FROM workout_logs ORDER BY dateEpochMillis DESC")
    suspend fun getAllWorkoutLogs(): List<WorkoutLogEntity>

    @Query("SELECT * FROM workout_logs WHERE workoutId = :workoutId ORDER BY dateEpochMillis DESC")
    suspend fun getLogsForWorkout(workoutId: String): List<WorkoutLogEntity>

    @Query("SELECT * FROM set_logs WHERE workoutLogId = :workoutLogId ORDER BY setNumber")
    suspend fun getSetLogs(workoutLogId: String): List<SetLogEntity>

    @Query("SELECT * FROM set_logs ORDER BY workoutLogId, setNumber")
    suspend fun getAllSetLogs(): List<SetLogEntity>

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
    suspend fun insertBodyWeightEntries(entries: List<BodyWeightEntryEntity>)

    @Query("DELETE FROM body_weight WHERE dateEpochDay = :epochDay")
    suspend fun deleteBodyWeightByDate(epochDay: Long)

    @Transaction
    suspend fun replaceBodyWeight(entry: BodyWeightEntryEntity) {
        deleteBodyWeightByDate(entry.dateEpochDay)
        insertBodyWeight(entry)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(entry: BodyMeasurementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurements(entries: List<BodyMeasurementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalRecord(entry: PersonalRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalRecords(entries: List<PersonalRecordEntity>)

    @Query("DELETE FROM body_measurements WHERE dateEpochDay = :epochDay")
    suspend fun deleteMeasurementByDate(epochDay: Long)

    @Transaction
    suspend fun replaceMeasurement(entry: BodyMeasurementEntity) {
        deleteMeasurementByDate(entry.dateEpochDay)
        insertMeasurement(entry)
    }

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressPhoto(photo: ProgressPhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressPhotos(photos: List<ProgressPhotoEntity>)

    @Query("DELETE FROM progress_photos WHERE id = :photoId")
    suspend fun deleteProgressPhoto(photoId: Long)

    @Query("SELECT * FROM progress_photos ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    suspend fun getProgressPhotos(): List<ProgressPhotoEntity>

    @Query("SELECT DISTINCT dateEpochDay FROM personal_records WHERE dateEpochDay IN (:epochDays)")
    suspend fun getPersonalRecordDates(epochDays: List<Long>): List<Long>

    @Query("SELECT * FROM body_measurements ORDER BY dateEpochDay ASC")
    suspend fun getBodyMeasurements(): List<BodyMeasurementEntity>

    @Query("SELECT * FROM personal_records ORDER BY dateEpochDay DESC, id DESC")
    suspend fun getAllPersonalRecords(): List<PersonalRecordEntity>

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

    @Query("SELECT * FROM body_weight ORDER BY dateEpochDay ASC")
    suspend fun getBodyWeightEntries(): List<BodyWeightEntryEntity>
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
