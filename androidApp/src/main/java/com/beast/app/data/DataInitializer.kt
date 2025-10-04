package com.beast.app.data

import com.beast.shared.data.SeedData
import com.beast.shared.repository.ExerciseRepository
import com.beast.shared.repository.ProgramRepository
import com.beast.shared.repository.SettingsRepository
import com.beast.shared.repository.WorkoutDayRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initializes the database with seed data on first run
 */
@Singleton
class DataInitializer @Inject constructor(
    private val programRepository: ProgramRepository,
    private val workoutDayRepository: WorkoutDayRepository,
    private val exerciseRepository: ExerciseRepository,
    private val settingsRepository: SettingsRepository
) {

    private var initialized = false

    suspend fun initializeIfNeeded() {
        if (initialized) return

        // Check if data already exists
        val existingPrograms = programRepository.getAll()
        if (existingPrograms.isNotEmpty()) {
            initialized = true
            return
        }

        // Insert seed data
        insertSeedData()
        initialized = true
    }

    private suspend fun insertSeedData() {
        // Insert Body Beast: Huge program
        val program = SeedData.getBodyBeastHugeProgram()
        programRepository.upsert(program)

        // Insert workout days
        val workoutDays = SeedData.getBodyBeastWorkoutDays()
        workoutDays.forEach { workoutDay ->
            workoutDayRepository.upsert(workoutDay)
        }

        // Insert common exercises
        val exercises = SeedData.getCommonExercises()
        exercises.forEach { exercise ->
            exerciseRepository.upsert(exercise)
        }
    }
}

