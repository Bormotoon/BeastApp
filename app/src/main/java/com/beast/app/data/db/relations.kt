package com.beast.app.data.db

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

// Program -> Phases

data class ProgramWithPhases(
    @Embedded val program: ProgramEntity,
    @Relation(
        parentColumn = "name",
        entityColumn = "programName"
    )
    val phases: List<PhaseEntity>
)

// Phase -> Workouts (через join таблицу)

data class PhaseWithWorkouts(
    @Embedded val phase: PhaseEntity,
    @Relation(
        parentColumn = "name",
        entityColumn = "id",
        associateBy = Junction(
            value = PhaseWorkoutCrossRefEntity::class,
            parentColumn = "phaseName",
            entityColumn = "workoutId"
        )
    )
    val workouts: List<WorkoutEntity>
)

// Workout -> ExerciseInWorkout (упорядоченные маппинги)

data class WorkoutWithMappings(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val mappings: List<ExerciseInWorkoutEntity>
)

