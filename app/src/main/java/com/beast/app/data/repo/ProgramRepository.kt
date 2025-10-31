package com.beast.app.data.repo

import com.beast.app.data.db.*
import com.beast.app.data.importer.ProgramJsonImporter
import com.beast.app.data.importer.slugify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import androidx.room.withTransaction

class ProgramRepository(
    private val db: BeastDatabase
) {
    private val programDao = db.programDao()
    private val workoutDao = db.workoutDao()
    private val importer = ProgramJsonImporter()

    data class ImportResult(
        val programName: String,
        val daysImported: Int,
        val workoutsCreated: Int,
        val exercisesLinked: Int
    )

    data class ProgramSummary(
        val program: ProgramEntity,
        val phases: List<PhaseEntity>,
        val schedule: List<ProgramScheduleEntity>,
        val phaseByWorkout: Map<String, String>
    )

    suspend fun importFromJson(json: String): ImportResult = withContext(Dispatchers.IO) {
        val model = importer.parse(json)
        val programName = model.title.trim()
        val durationDays = model.durationDays

        val phases = if (!model.phases.isNullOrEmpty()) {
            model.phases.map { PhaseEntity(programName = programName, name = it.name.trim(), durationWeeks = it.durationWeeks) }
        } else {
            listOf(PhaseEntity(programName = programName, name = "Default", durationWeeks = ceil(durationDays / 7.0).toInt()))
        }
        val phaseNameForMapping = phases.first().name

        // Build workouts and schedule
        val workoutEntities = mutableListOf<WorkoutEntity>()
        val mappings = mutableListOf<ExerciseInWorkoutEntity>()
        val phaseXrefs = mutableListOf<PhaseWorkoutCrossRefEntity>()
        val schedule = mutableListOf<ProgramScheduleEntity>()

        val allExerciseIds = mutableSetOf<String>()

        model.days.sortedBy { it.dayIndex }.forEach { day ->
            val dayTitle = day.title.trim()
            val workoutId = (dayTitle + "-day-" + day.dayIndex).slugify()
            workoutEntities += WorkoutEntity(
                id = workoutId,
                name = dayTitle,
                durationMinutes = day.durationMinutes ?: 0,
                targetMuscleGroups = emptyList()
            )
            schedule += ProgramScheduleEntity(programName = programName, dayNumber = day.dayIndex, workoutId = workoutId)
            phaseXrefs += PhaseWorkoutCrossRefEntity(programName = programName, phaseName = phaseNameForMapping, workoutId = workoutId)

            day.exercisesOrder?.forEachIndexed { idx, exIdRaw ->
                val exId = exIdRaw.trim()
                if (exId.isNotEmpty()) {
                    allExerciseIds += exId
                    mappings += ExerciseInWorkoutEntity(
                        workoutId = workoutId,
                        orderIndex = idx,
                        exerciseId = exId,
                        setType = "SINGLE",
                        targetReps = "",
                        notes = null
                    )
                }
            }
        }

        db.withTransaction {
            // Upsert program and phases
            programDao.upsertProgram(ProgramEntity(name = programName, durationDays = durationDays))
            programDao.upsertPhases(phases)

            // Ensure exercises exist (create placeholders for missing IDs)
            val existing = if (allExerciseIds.isEmpty()) emptyList() else workoutDao.getExercisesByIds(allExerciseIds.toList())
            val existingIds = existing.map { it.id }.toSet()
            val missing = allExerciseIds.filterNot { existingIds.contains(it) }
            if (missing.isNotEmpty()) {
                val placeholders = missing.map { id ->
                    ExerciseEntity(
                        id = id,
                        name = id.replace('-', ' ').replace('_', ' ').replaceFirstChar { it.uppercase() },
                        exerciseType = "STRENGTH",
                        primaryMuscleGroup = "general",
                        equipment = emptyList(),
                        instructions = null,
                        videoUrl = null
                    )
                }
                workoutDao.upsertExercises(placeholders)
            }

            // Upsert workouts and relations
            if (workoutEntities.isNotEmpty()) programDao.upsertWorkouts(workoutEntities)
            if (mappings.isNotEmpty()) workoutDao.upsertExerciseInWorkout(mappings)
            if (phaseXrefs.isNotEmpty()) programDao.upsertPhaseWorkouts(phaseXrefs)
            if (schedule.isNotEmpty()) programDao.upsertSchedule(schedule)
        }

        ImportResult(
            programName = programName,
            daysImported = model.days.size,
            workoutsCreated = workoutEntities.size,
            exercisesLinked = mappings.size
        )
    }

    // Возвращает расписание (список ProgramScheduleEntity) для программы
    suspend fun getSchedule(programName: String): List<ProgramScheduleEntity> = withContext(Dispatchers.IO) {
        programDao.getSchedule(programName)
    }

    suspend fun getProgramSummary(programName: String): ProgramSummary? = withContext(Dispatchers.IO) {
        val program = programDao.getProgram(programName) ?: return@withContext null
        val phases = programDao.getPhases(programName)
        val schedule = programDao.getSchedule(programName)
        val phaseWorkouts = programDao.getPhaseWorkouts(programName)
        ProgramSummary(
            program = program,
            phases = phases,
            schedule = schedule,
            phaseByWorkout = phaseWorkouts.associate { crossRef ->
                crossRef.workoutId to crossRef.phaseName
            }
        )
    }
}
