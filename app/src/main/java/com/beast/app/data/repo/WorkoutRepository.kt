package com.beast.app.data.repo

import com.beast.app.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction

/**
 * Repository providing higher-level operations around workouts, exercises and logs.
 * Mirrors style of ProgramRepository and centralizes DB interactions for workout-related features.
 */
class WorkoutRepository private constructor(
    private val workoutDao: WorkoutDao,
    private val workoutLogDao: WorkoutLogDao,
    private val dbOrNull: BeastDatabase?
) {
    constructor(db: BeastDatabase) : this(
        workoutDao = db.workoutDao(),
        workoutLogDao = db.workoutLogDao(),
        dbOrNull = db
    )

    constructor(workoutDao: WorkoutDao, workoutLogDao: WorkoutLogDao) : this(
        workoutDao = workoutDao,
        workoutLogDao = workoutLogDao,
        dbOrNull = null
    )

    data class WorkoutWithExercises(
        val workout: WorkoutEntity,
        val mappings: List<ExerciseInWorkoutEntity>,
        val exercises: List<ExerciseEntity>
    )

    suspend fun getWorkoutWithExercises(workoutId: String): WorkoutWithExercises? = withContext(Dispatchers.IO) {
        val workout = workoutDao.getWorkout(workoutId) ?: return@withContext null
        val mappings = workoutDao.getExerciseMappings(workoutId)
        val exerciseIds = mappings.map { it.exerciseId }
        val exercises = if (exerciseIds.isNotEmpty()) workoutDao.getExercisesByIds(exerciseIds) else emptyList()
        WorkoutWithExercises(workout = workout, mappings = mappings, exercises = exercises)
    }

    suspend fun upsertExercises(exercises: List<ExerciseEntity>) = withContext(Dispatchers.IO) {
        if (exercises.isEmpty()) return@withContext
        workoutDao.upsertExercises(exercises)
    }

    suspend fun upsertExerciseInWorkout(mappings: List<ExerciseInWorkoutEntity>) = withContext(Dispatchers.IO) {
        if (mappings.isEmpty()) return@withContext
        workoutDao.upsertExerciseInWorkout(mappings)
    }

    /**
     * Inserts a workout log and its set logs in a single transaction.
     * Returns true if successful.
     */
    suspend fun insertWorkoutLogWithSets(log: WorkoutLogEntity, setLogs: List<SetLogEntity>) = withContext(Dispatchers.IO) {
        val db = dbOrNull
        if (db != null) {
            db.withTransaction {
                workoutLogDao.insertWorkoutLog(log)
                if (setLogs.isNotEmpty()) workoutLogDao.insertSetLogs(setLogs)
            }
        } else {
            // Fallback path for tests where we don't have a real RoomDatabase
            workoutLogDao.insertWorkoutLog(log)
            if (setLogs.isNotEmpty()) workoutLogDao.insertSetLogs(setLogs)
        }
    }

    suspend fun getAllWorkoutLogs(): List<WorkoutLogEntity> = withContext(Dispatchers.IO) {
        workoutLogDao.getAllWorkoutLogs()
    }

    suspend fun getLogsForWorkout(workoutId: String): List<WorkoutLogEntity> = withContext(Dispatchers.IO) {
        workoutLogDao.getLogsForWorkout(workoutId)
    }

    suspend fun getSetLogsForWorkoutLog(workoutLogId: String): List<SetLogEntity> = withContext(Dispatchers.IO) {
        workoutLogDao.getSetLogs(workoutLogId)
    }

    suspend fun getSetAggregates(logIds: List<String>): Map<String, WorkoutLogSetAggregate> = withContext(Dispatchers.IO) {
        if (logIds.isEmpty()) return@withContext emptyMap()
        workoutLogDao.getSetLogAggregates(logIds).associateBy { it.workoutLogId }
    }

    suspend fun getExerciseVolumeAggregates(logIds: List<String>): List<WorkoutLogExerciseAggregate> = withContext(Dispatchers.IO) {
        if (logIds.isEmpty()) return@withContext emptyList()
        workoutLogDao.getExerciseVolumeAggregates(logIds)
    }

    suspend fun getLogsBetween(startMillis: Long, endMillis: Long): List<WorkoutLogEntity> = withContext(Dispatchers.IO) {
        if (startMillis > endMillis) return@withContext emptyList()
        workoutLogDao.getLogsBetween(startMillis, endMillis)
    }

    suspend fun getWorkoutsByIds(workoutIds: List<String>): List<WorkoutEntity> = withContext(Dispatchers.IO) {
        if (workoutIds.isEmpty()) return@withContext emptyList()
        workoutDao.getWorkoutsByIds(workoutIds)
    }

    suspend fun getWorkout(workoutId: String): WorkoutEntity? = withContext(Dispatchers.IO) {
        workoutDao.getWorkout(workoutId)
    }

    suspend fun getExercisesByIds(exerciseIds: List<String>): List<ExerciseEntity> = withContext(Dispatchers.IO) {
        if (exerciseIds.isEmpty()) return@withContext emptyList()
        workoutDao.getExercisesByIds(exerciseIds)
    }

    suspend fun getLatestLogsForWorkouts(workoutIds: List<String>): Map<String, WorkoutLogEntity> = withContext(Dispatchers.IO) {
        if (workoutIds.isEmpty()) return@withContext emptyMap()
        workoutLogDao.getLatestLogsForWorkouts(workoutIds).associateBy { it.workoutId }
    }

    suspend fun isWorkoutFavorite(workoutId: String): Boolean = withContext(Dispatchers.IO) {
        workoutDao.isFavorite(workoutId)
    }

    suspend fun setWorkoutFavorite(workoutId: String, favorite: Boolean) = withContext(Dispatchers.IO) {
        if (favorite) {
            workoutDao.addFavorite(
                WorkoutFavoriteEntity(
                    workoutId = workoutId,
                    addedAtEpochMillis = System.currentTimeMillis()
                )
            )
        } else {
            workoutDao.removeFavorite(workoutId)
        }
    }

    suspend fun getFavoriteWorkoutIds(): List<String> = withContext(Dispatchers.IO) {
        workoutDao.getFavoriteWorkoutIds()
    }
}

