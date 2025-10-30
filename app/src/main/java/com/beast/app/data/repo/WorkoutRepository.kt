package com.beast.app.data.repo

import com.beast.app.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction

/**
 * Repository providing higher-level operations around workouts, exercises and logs.
 * Mirrors style of ProgramRepository and centralizes DB interactions for workout-related features.
 */
class WorkoutRepository(
    private val db: BeastDatabase
) {
    private val workoutDao = db.workoutDao()
    private val workoutLogDao = db.workoutLogDao()

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
        db.withTransaction {
            workoutLogDao.insertWorkoutLog(log)
            if (setLogs.isNotEmpty()) workoutLogDao.insertSetLogs(setLogs)
        }
    }

    suspend fun getLogsForWorkout(workoutId: String): List<WorkoutLogEntity> = withContext(Dispatchers.IO) {
        workoutLogDao.getLogsForWorkout(workoutId)
    }

    suspend fun getSetLogsForWorkoutLog(workoutLogId: String): List<SetLogEntity> = withContext(Dispatchers.IO) {
        workoutLogDao.getSetLogs(workoutLogId)
    }
}

