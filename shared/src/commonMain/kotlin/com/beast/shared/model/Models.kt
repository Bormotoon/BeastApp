package com.beast.shared.model

data class Program(
    val id: String,
    val title: String,
    val description: String,
    val durationDays: Int,
    val difficulty: String? = null,
    val thumbnail: String? = null,
    val tags: List<String> = emptyList(),
    val author: String? = null,
)

data class WorkoutDay(
    val id: String,
    val programId: String,
    val dayIndex: Int,
    val title: String,
    val durationEstimate: Int? = null,
    val exercisesOrder: List<String> = emptyList(),
)

data class Exercise(
    val id: String,
    val name: String,
    val primaryMuscles: List<String> = emptyList(),
    val equipment: List<String> = emptyList(),
    val demoVideoUrl: String? = null,
    val defaultSets: Int? = null,
    val defaultReps: Int? = null,
    val restSec: Int? = null,
)

data class SetLog(
    val id: String,
    val workoutDayId: String,
    val exerciseId: String,
    val setIndex: Int,
    val reps: Int,
    val weight: Double,
    val rpe: Double? = null,
    val timestamp: Long,
)

data class WorkoutLog(
    val id: String,
    val programId: String,
    val dayIndex: Int,
    val date: Long,
    val completed: Boolean,
    val notes: String? = null,
)

