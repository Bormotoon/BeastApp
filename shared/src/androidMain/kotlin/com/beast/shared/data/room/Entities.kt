package com.beast.shared.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val durationDays: Int,
    val difficulty: String?,
    val thumbnail: String?,
    val tagsJson: String,
    val author: String?,
)

@Entity(tableName = "workout_days")
data class WorkoutDayEntity(
    @PrimaryKey val id: String,
    val programId: String,
    val dayIndex: Int,
    val title: String,
    val durationEstimate: Int?,
    val exercisesOrderJson: String,
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val primaryMusclesJson: String,
    val equipmentJson: String,
    val demoVideoUrl: String?,
    val defaultSets: Int?,
    val defaultReps: Int?,
    val restSec: Int?,
)

@Entity(tableName = "workout_logs")
data class WorkoutLogEntity(
    @PrimaryKey val id: String,
    val programId: String,
    val dayIndex: Int,
    val date: Long,
    val completed: Boolean,
    val notes: String?,
)

@Entity(tableName = "set_logs")
data class SetLogEntity(
    @PrimaryKey val id: String,
    val workoutDayId: String,
    val exerciseId: String,
    val setIndex: Int,
    val reps: Int,
    val weight: Double,
    val rpe: Double?,
    val timestamp: Long,
)

