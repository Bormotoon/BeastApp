package com.beast.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// Catalog: Program / Phase / Workout / Exercise and relations

@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey val name: String,
    val durationDays: Int
)

@Entity(tableName = "phases", primaryKeys = ["programName", "name"])
data class PhaseEntity(
    val programName: String,
    val name: String,
    val durationWeeks: Int
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val name: String,
    val durationMinutes: Int,
    val targetMuscleGroups: List<String> = emptyList()
)

@Entity(tableName = "phase_workout", primaryKeys = ["programName", "phaseName", "workoutId"])
data class PhaseWorkoutCrossRefEntity(
    val programName: String,
    val phaseName: String,
    val workoutId: String
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val exerciseType: String,
    val primaryMuscleGroup: String,
    val equipment: List<String> = emptyList(),
    val instructions: String? = null,
    val videoUrl: String? = null
)

@Entity(tableName = "exercise_in_workout", primaryKeys = ["workoutId", "orderIndex"])
data class ExerciseInWorkoutEntity(
    val workoutId: String,
    val orderIndex: Int,
    val exerciseId: String,
    val setType: String,
    val targetReps: String,
    val notes: String? = null
)

@Entity(tableName = "program_schedule", primaryKeys = ["programName", "dayNumber"])
data class ProgramScheduleEntity(
    val programName: String,
    val dayNumber: Int,
    val workoutId: String
)

// Logs
@Entity(tableName = "workout_logs")
data class WorkoutLogEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val dateEpochMillis: Long,
    val totalDuration: Int,
    val totalVolume: Double,
    val totalReps: Int,
    val calories: Int? = null,
    val notes: String? = null,
    val rating: Int? = null,
    val status: String
)

@Entity(tableName = "set_logs")
data class SetLogEntity(
    @PrimaryKey val id: String,
    val workoutLogId: String,
    val exerciseId: String,
    val setNumber: Int,
    val weight: Double? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val distance: Double? = null,
    val side: String = "NONE",
    val isCompleted: Boolean = false,
    val notes: String? = null,
    val rpe: Int? = null
)

// Profile and measurements
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val startDateEpochDay: Long,
    val currentProgramId: String? = null,
    val weightUnit: String,
    val avatarUri: String? = null,
    val heightCm: Double? = null,
    val age: Int? = null,
    val gender: String? = null
)

@Entity(tableName = "body_weight")
data class BodyWeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateEpochDay: Long,
    val weight: Double
)

@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateEpochDay: Long,
    val chest: Double? = null,
    val waist: Double? = null,
    val hips: Double? = null,
    val bicepsLeft: Double? = null,
    val bicepsRight: Double? = null,
    val thighsLeft: Double? = null,
    val thighsRight: Double? = null,
    val calfLeft: Double? = null,
    val calfRight: Double? = null
)

@Entity(tableName = "personal_records")
data class PersonalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseId: String,
    val weight: Double,
    val reps: Int,
    val estimated1RM: Double,
    val dateEpochDay: Long
)

@Entity(tableName = "favorite_workouts")
data class WorkoutFavoriteEntity(
    @PrimaryKey val workoutId: String,
    val addedAtEpochMillis: Long
)

@Entity(tableName = "progress_photos")
data class ProgressPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val angle: String,
    val uri: String,
    val createdAtEpochMillis: Long,
    val notes: String? = null
)

