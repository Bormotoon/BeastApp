package com.beast.shared.data

import com.beast.shared.model.*

/**
 * Seed data for Body Beast: Huge program
 * Based on the 90-day Body Beast program structure
 */
object SeedData {

    fun getBodyBeastHugeProgram(): Program {
        return Program(
            id = "body-beast-huge",
            title = "Body Beast: Huge",
            description = "A comprehensive 90-day muscle-building program designed to maximize size and strength gains through progressive resistance training.",
            durationDays = 90,
            difficulty = "Advanced",
            tags = listOf("Bodybuilding", "Mass Building", "Strength Training"),
            author = "Beachbody"
        )
    }

    fun getBodyBeastWorkoutDays(): List<WorkoutDay> {
        val programId = "body-beast-huge"
        val workouts = mutableListOf<WorkoutDay>()

        // Week 1-3: Build Phase
        val buildWeeks = 3
        for (week in 0 until buildWeeks) {
            val baseDay = week * 7

            // Day 1: Chest/Tris
            workouts.add(WorkoutDay(
                id = "$programId-day-${baseDay + 1}",
                programId = programId,
                dayIndex = baseDay + 1,
                title = "Chest & Triceps",
                durationEstimate = 45,
                exercisesOrder = listOf("bench-press", "incline-db-press", "cable-flyes", "tricep-dips", "overhead-extension")
            ))

            // Day 2: Back/Bis
            workouts.add(WorkoutDay(
                id = "$programId-day-${baseDay + 2}",
                programId = programId,
                dayIndex = baseDay + 2,
                title = "Back & Biceps",
                durationEstimate = 45,
                exercisesOrder = listOf("deadlift", "bent-over-row", "pull-ups", "barbell-curl", "hammer-curl")
            ))

            // Day 3: Shoulders
            workouts.add(WorkoutDay(
                id = "$programId-day-${baseDay + 3}",
                programId = programId,
                dayIndex = baseDay + 3,
                title = "Shoulders",
                durationEstimate = 40,
                exercisesOrder = listOf("military-press", "lateral-raise", "front-raise", "rear-delt-flye", "shrugs")
            ))

            // Day 4: Legs
            workouts.add(WorkoutDay(
                id = "$programId-day-${baseDay + 4}",
                programId = programId,
                dayIndex = baseDay + 4,
                title = "Legs",
                durationEstimate = 50,
                exercisesOrder = listOf("squats", "leg-press", "leg-curl", "leg-extension", "calf-raise")
            ))

            // Day 5: Chest/Tris
            workouts.add(WorkoutDay(
                id = "$programId-day-${baseDay + 5}",
                programId = programId,
                dayIndex = baseDay + 5,
                title = "Chest & Triceps",
                durationEstimate = 45,
                exercisesOrder = listOf("bench-press", "incline-db-press", "cable-flyes", "tricep-dips", "overhead-extension")
            ))

            // Day 6: Back/Bis
            workouts.add(WorkoutDay(
                id = "$programId-day-${baseDay + 6}",
                programId = programId,
                dayIndex = baseDay + 6,
                title = "Back & Biceps",
                durationEstimate = 45,
                exercisesOrder = listOf("deadlift", "bent-over-row", "pull-ups", "barbell-curl", "hammer-curl")
            ))

            // Day 7: Rest
            workouts.add(WorkoutDay(
                id = "$programId-day-${baseDay + 7}",
                programId = programId,
                dayIndex = baseDay + 7,
                title = "Rest Day",
                durationEstimate = 0,
                exercisesOrder = emptyList()
            ))
        }

        // Continue pattern for remaining days (simplified for now)
        // In real implementation, you'd add all 90 days with varying workouts
        for (day in 22..90) {
            workouts.add(WorkoutDay(
                id = "$programId-day-$day",
                programId = programId,
                dayIndex = day,
                title = if (day % 7 == 0) "Rest Day" else "Workout Day $day",
                durationEstimate = if (day % 7 == 0) 0 else 45,
                exercisesOrder = if (day % 7 == 0) emptyList() else listOf("bench-press", "squats")
            ))
        }

        return workouts
    }

    fun getCommonExercises(): List<Exercise> {
        return listOf(
            Exercise(
                id = "bench-press",
                name = "Barbell Bench Press",
                primaryMuscles = listOf("Chest", "Triceps", "Shoulders"),
                equipment = listOf("Barbell", "Bench"),
                defaultSets = 3,
                defaultReps = 10,
                restSec = 90
            ),
            Exercise(
                id = "incline-db-press",
                name = "Incline Dumbbell Press",
                primaryMuscles = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Dumbbells", "Incline Bench"),
                defaultSets = 3,
                defaultReps = 10,
                restSec = 90
            ),
            Exercise(
                id = "cable-flyes",
                name = "Cable Flyes",
                primaryMuscles = listOf("Chest"),
                equipment = listOf("Cable Machine"),
                defaultSets = 3,
                defaultReps = 12,
                restSec = 60
            ),
            Exercise(
                id = "tricep-dips",
                name = "Tricep Dips",
                primaryMuscles = listOf("Triceps", "Chest"),
                equipment = listOf("Dip Bar"),
                defaultSets = 3,
                defaultReps = 10,
                restSec = 60
            ),
            Exercise(
                id = "overhead-extension",
                name = "Overhead Tricep Extension",
                primaryMuscles = listOf("Triceps"),
                equipment = listOf("Dumbbell"),
                defaultSets = 3,
                defaultReps = 12,
                restSec = 60
            ),
            Exercise(
                id = "deadlift",
                name = "Barbell Deadlift",
                primaryMuscles = listOf("Back", "Legs", "Core"),
                equipment = listOf("Barbell"),
                defaultSets = 3,
                defaultReps = 8,
                restSec = 120
            ),
            Exercise(
                id = "bent-over-row",
                name = "Bent Over Barbell Row",
                primaryMuscles = listOf("Back", "Biceps"),
                equipment = listOf("Barbell"),
                defaultSets = 3,
                defaultReps = 10,
                restSec = 90
            ),
            Exercise(
                id = "pull-ups",
                name = "Pull-ups",
                primaryMuscles = listOf("Back", "Biceps"),
                equipment = listOf("Pull-up Bar"),
                defaultSets = 3,
                defaultReps = 10,
                restSec = 90
            ),
            Exercise(
                id = "barbell-curl",
                name = "Barbell Curl",
                primaryMuscles = listOf("Biceps"),
                equipment = listOf("Barbell"),
                defaultSets = 3,
                defaultReps = 10,
                restSec = 60
            ),
            Exercise(
                id = "hammer-curl",
                name = "Hammer Curl",
                primaryMuscles = listOf("Biceps", "Forearms"),
                equipment = listOf("Dumbbells"),
                defaultSets = 3,
                defaultReps = 12,
                restSec = 60
            ),
            Exercise(
                id = "military-press",
                name = "Military Press",
                primaryMuscles = listOf("Shoulders", "Triceps"),
                equipment = listOf("Barbell"),
                defaultSets = 3,
                defaultReps = 10,
                restSec = 90
            ),
            Exercise(
                id = "lateral-raise",
                name = "Lateral Raise",
                primaryMuscles = listOf("Shoulders"),
                equipment = listOf("Dumbbells"),
                defaultSets = 3,
                defaultReps = 12,
                restSec = 60
            ),
            Exercise(
                id = "front-raise",
                name = "Front Raise",
                primaryMuscles = listOf("Shoulders"),
                equipment = listOf("Dumbbells"),
                defaultSets = 3,
                defaultReps = 12,
                restSec = 60
            ),
            Exercise(
                id = "rear-delt-flye",
                name = "Rear Delt Flye",
                primaryMuscles = listOf("Shoulders"),
                equipment = listOf("Dumbbells"),
                defaultSets = 3,
                defaultReps = 12,
                restSec = 60
            ),
            Exercise(
                id = "shrugs",
                name = "Barbell Shrugs",
                primaryMuscles = listOf("Traps"),
                equipment = listOf("Barbell"),
                defaultSets = 3,
                defaultReps = 12,
                restSec = 60
            ),
            Exercise(
                id = "squats",
                name = "Barbell Squat",
                primaryMuscles = listOf("Quads", "Glutes", "Hamstrings"),
                equipment = listOf("Barbell", "Squat Rack"),
                defaultSets = 3,
                defaultReps = 10,
                restSec = 120
            ),
            Exercise(
                id = "leg-press",
                name = "Leg Press",
                primaryMuscles = listOf("Quads", "Glutes"),
                equipment = listOf("Leg Press Machine"),
                defaultSets = 3,
                defaultReps = 12,
                restSec = 90
            ),
            Exercise(
                id = "leg-curl",
                name = "Leg Curl",
                primaryMuscles = listOf("Hamstrings"),
                equipment = listOf("Leg Curl Machine"),
                defaultSets = 3,
                defaultReps = 12,
                restSec = 60
            ),
            Exercise(
                id = "leg-extension",
                name = "Leg Extension",
                primaryMuscles = listOf("Quads"),
                equipment = listOf("Leg Extension Machine"),
                defaultSets = 3,
                defaultReps = 12,
                restSec = 60
            ),
            Exercise(
                id = "calf-raise",
                name = "Standing Calf Raise",
                primaryMuscles = listOf("Calves"),
                equipment = listOf("Calf Raise Machine"),
                defaultSets = 3,
                defaultReps = 15,
                restSec = 60
            )
        )
    }
}

