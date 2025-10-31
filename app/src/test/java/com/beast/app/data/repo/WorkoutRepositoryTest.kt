package com.beast.app.data.repo

import com.beast.app.data.db.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper

// Простые фейковые реализации DAO/Database для JVM unit-тестов (без Android)
class FakeWorkoutDao : WorkoutDao {
    private val workouts = mutableMapOf<String, WorkoutEntity>()
    private val mappings = mutableMapOf<String, MutableList<ExerciseInWorkoutEntity>>()
    private val exercises = mutableMapOf<String, ExerciseEntity>()

    override suspend fun upsertWorkout(workout: WorkoutEntity) {
        workouts[workout.id] = workout
    }

    override suspend fun upsertExercises(exercises: List<ExerciseEntity>) {
        exercises.forEach { this.exercises[it.id] = it }
    }

    override suspend fun upsertExerciseInWorkout(list: List<ExerciseInWorkoutEntity>) {
        list.forEach { mappings.computeIfAbsent(it.workoutId) { mutableListOf() }.add(it) }
    }

    override suspend fun getWorkout(id: String): WorkoutEntity? = workouts[id]

    override suspend fun getWorkoutsByIds(ids: List<String>): List<WorkoutEntity> = ids.mapNotNull { workouts[it] }

    override suspend fun getExerciseMappings(workoutId: String): List<ExerciseInWorkoutEntity> = mappings[workoutId]?.toList() ?: emptyList()

    override suspend fun getExerciseMappingsForWorkouts(workoutIds: List<String>): List<ExerciseInWorkoutEntity> =
        workoutIds.flatMap { id -> mappings[id]?.toList() ?: emptyList() }

    override suspend fun getExercisesByIds(ids: List<String>): List<ExerciseEntity> = ids.mapNotNull { exercises[it] }
}

class FakeWorkoutLogDao : WorkoutLogDao {
    private val logs = mutableListOf<WorkoutLogEntity>()
    private val setLogs = mutableMapOf<String, MutableList<SetLogEntity>>()

    override suspend fun insertWorkoutLog(log: WorkoutLogEntity) {
        logs.add(log)
    }

    override suspend fun insertSetLogs(logs: List<SetLogEntity>) {
        logs.forEach { setLogs.computeIfAbsent(it.workoutLogId) { mutableListOf() }.add(it) }
    }

    override suspend fun getLogsForWorkout(workoutId: String): List<WorkoutLogEntity> = logs.filter { it.workoutId == workoutId }

    override suspend fun getSetLogs(workoutLogId: String): List<SetLogEntity> = setLogs[workoutLogId]?.toList() ?: emptyList()

    override suspend fun getLatestLogsForWorkouts(workoutIds: List<String>): List<WorkoutLogEntity> {
        return workoutIds.mapNotNull { id ->
            logs.filter { it.workoutId == id }.maxByOrNull { it.dateEpochMillis }
        }
    }
}

// Fake BeastDatabase provides required DAOs
// For these JVM tests we avoid subclassing RoomDatabase to dodge internal API differences.
// Instead, we pass fakes directly into the repository via its testing constructor.
class FakeBeastDatabase(
    val workoutDaoImpl: WorkoutDao = FakeWorkoutDao(),
    val workoutLogDaoImpl: WorkoutLogDao = FakeWorkoutLogDao()
)

class WorkoutRepositoryTest {

    @Test
    fun `getWorkoutWithExercises returns null when missing`() = runBlocking {
        val db = FakeBeastDatabase()
        val repo = WorkoutRepository(db.workoutDaoImpl, db.workoutLogDaoImpl)
        val result = repo.getWorkoutWithExercises("missing")
        assertNull(result)
    }

    @Test
    fun `getWorkoutWithExercises returns composed object`() = runBlocking {
        val fakeWorkoutDao = FakeWorkoutDao()
        val workout = WorkoutEntity(id = "w1", name = "W1", durationMinutes = 30)
        fakeWorkoutDao.upsertWorkout(workout)
        val ex1 = ExerciseEntity(id = "e1", name = "E1", exerciseType = "STRENGTH", primaryMuscleGroup = "chest")
        val ex2 = ExerciseEntity(id = "e2", name = "E2", exerciseType = "STRENGTH", primaryMuscleGroup = "back")
        fakeWorkoutDao.upsertExercises(listOf(ex1, ex2))
        fakeWorkoutDao.upsertExerciseInWorkout(listOf(
            ExerciseInWorkoutEntity(workoutId = "w1", orderIndex = 0, exerciseId = "e1", setType = "SINGLE", targetReps = "10"),
            ExerciseInWorkoutEntity(workoutId = "w1", orderIndex = 1, exerciseId = "e2", setType = "SINGLE", targetReps = "8")
        ))

        val db = FakeBeastDatabase(workoutDaoImpl = fakeWorkoutDao)
        val repo = WorkoutRepository(db.workoutDaoImpl, db.workoutLogDaoImpl)
        val composed = repo.getWorkoutWithExercises("w1")
        assertNotNull(composed)
        assertEquals("W1", composed!!.workout.name)
        assertEquals(2, composed.mappings.size)
        assertEquals(2, composed.exercises.size)
    }

    @Test
    fun `logs and set logs roundtrip`() = runBlocking {
        val fakeWorkoutLogDao = FakeWorkoutLogDao()
        val db = FakeBeastDatabase(workoutLogDaoImpl = fakeWorkoutLogDao)
        val repo = WorkoutRepository(db.workoutDaoImpl, db.workoutLogDaoImpl)

        val log = WorkoutLogEntity(id = "log1", workoutId = "w1", dateEpochMillis = 1L, totalDuration = 30, totalVolume = 100.0, totalReps = 20, calories = null, notes = null, rating = null, status = "COMPLETED")
        val sets = listOf(
            SetLogEntity(id = "s1", workoutLogId = "log1", exerciseId = "e1", setNumber = 1),
            SetLogEntity(id = "s2", workoutLogId = "log1", exerciseId = "e1", setNumber = 2)
        )

        // We can't rely on db.withTransaction in fake; call DAO directly to simulate insertion
        fakeWorkoutLogDao.insertWorkoutLog(log)
        fakeWorkoutLogDao.insertSetLogs(sets)

        val fetchedLogs = repo.getLogsForWorkout("w1")
        assertEquals(1, fetchedLogs.size)
        val fetchedSets = repo.getSetLogsForWorkoutLog("log1")
        assertEquals(2, fetchedSets.size)
    }
}
