package com.beast.shared.usecase

import com.beast.shared.data.import.ProgramCsvParser
import com.beast.shared.repository.ExerciseRepository
import com.beast.shared.repository.ProgramRepository
import com.beast.shared.repository.WorkoutDayRepository
import java.io.InputStream

/**
 * Use case for importing fitness programs from CSV files
 */
class ImportProgramUseCase(
    private val programRepository: ProgramRepository,
    private val workoutDayRepository: WorkoutDayRepository,
    private val exerciseRepository: ExerciseRepository
) {

    data class ImportResult(
        val success: Boolean,
        val programId: String?,
        val programName: String?,
        val workoutDaysCount: Int,
        val errors: List<String>,
        val warnings: List<String>
    )

    suspend fun execute(csvInputStream: InputStream): ImportResult {
        try {
            // Parse CSV
            val parser = ProgramCsvParser()
            val parseResult = parser.parse(csvInputStream)

            // Check for errors
            if (parseResult.errors.isNotEmpty()) {
                return ImportResult(
                    success = false,
                    programId = null,
                    programName = null,
                    workoutDaysCount = 0,
                    errors = parseResult.errors,
                    warnings = parseResult.warnings
                )
            }

            // Validate exercises exist
            val allExercises = exerciseRepository.getAll()
            val existingExerciseIds = allExercises.map { it.id }.toSet()

            val missingExercises = mutableSetOf<String>()
            parseResult.workoutDays.forEach { day ->
                day.exercisesOrder.forEach { exerciseId ->
                    if (!existingExerciseIds.contains(exerciseId)) {
                        missingExercises.add(exerciseId)
                    }
                }
            }

            val warnings = parseResult.warnings.toMutableList()
            if (missingExercises.isNotEmpty()) {
                warnings.add("Some exercises not found and will be skipped: ${missingExercises.joinToString(", ")}")
            }

            // Filter out missing exercises
            val validatedWorkoutDays = parseResult.workoutDays.map { day ->
                day.copy(
                    exercisesOrder = day.exercisesOrder.filter { existingExerciseIds.contains(it) }
                )
            }

            // Save program
            programRepository.upsert(parseResult.program)

            // Save workout days
            validatedWorkoutDays.forEach { day ->
                workoutDayRepository.upsert(day)
            }

            return ImportResult(
                success = true,
                programId = parseResult.program.id,
                programName = parseResult.program.title,
                workoutDaysCount = validatedWorkoutDays.size,
                errors = emptyList(),
                warnings = warnings
            )

        } catch (e: Exception) {
            return ImportResult(
                success = false,
                programId = null,
                programName = null,
                workoutDaysCount = 0,
                errors = listOf("Import failed: ${e.message}"),
                warnings = emptyList()
            )
        }
    }
}

