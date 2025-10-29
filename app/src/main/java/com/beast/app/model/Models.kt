package com.beast.app.model

import java.time.Instant
import java.time.LocalDate

/**
 * Базовые модели данных (MVP 1.0) согласно docs/TODO.md и ThePlan.md.
 * Внимание на нотацию:
 * - Внутренний код использует camelCase (например, setType, videoUrl),
 *   а пользовательские CSV/JSON в документации могут использовать snake_case
 *   (например, set_type, video_url). Маппинг будет реализован на уровне импорта.
 */

// ===== Enums =====

enum class ExerciseType { STRENGTH, CARDIO, ISOMETRIC }

enum class SetType { SINGLE, SUPER, GIANT, MULTI, FORCE, PROGRESSIVE, COMBO, CIRCUIT, TEMPO }

enum class WorkoutStatus { COMPLETED, INCOMPLETE }

enum class Side { LEFT, RIGHT, NONE }

enum class WeightUnit { KG, LBS }

// ===== Small value objects =====

data class Sided(val left: Double? = null, val right: Double? = null)

// ===== Core catalog entities =====

data class Exercise(
    val id: String,
    val name: String,
    val exerciseType: ExerciseType,
    val primaryMuscleGroup: String,
    val equipment: List<String> = emptyList(),
    val instructions: String? = null,
    val videoUrl: String? = null
)

data class ExerciseInWorkout(
    val exerciseId: String,
    // Внутри кода camelCase; для импорта поддержим snake_case (set_type) маппингом
    val setType: SetType,
    // Например: "15, 12, 8" или "15-12-8-8-12-15" для Progressive
    val targetReps: String,
    val notes: String? = null
)

data class Workout(
    val id: String,
    val name: String,
    val durationMinutes: Int,
    val targetMuscleGroups: List<String> = emptyList(),
    val exercises: List<ExerciseInWorkout> = emptyList()
)

data class Phase(
    val name: String,
    val durationWeeks: Int,
    val workouts: List<Workout> = emptyList()
)

data class Program(
    val name: String,
    val durationDays: Int,
    val phases: List<Phase> = emptyList(),
    // День программы (1..N) -> workoutId
    val schedule: Map<Int, String> = emptyMap()
)

// ===== Logging entities =====

data class WorkoutLog(
    val id: String,
    val workoutId: String,
    // Время начала или дата выполнения; используем Instant для гибкости
    val date: Instant,
    // Минуты общей длительности тренировки
    val totalDuration: Int,
    // Сумма (вес × повторения) по всем подходам
    val totalVolume: Double,
    val totalReps: Int,
    val calories: Int? = null,
    val notes: String? = null,
    // 1..5 (опционально)
    val rating: Int? = null,
    val status: WorkoutStatus = WorkoutStatus.COMPLETED
)

data class SetLog(
    val id: String,
    val workoutLogId: String,
    val exerciseId: String,
    val setNumber: Int,
    val weight: Double? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val distance: Double? = null,
    val side: Side = Side.NONE,
    val isCompleted: Boolean = false,
    val notes: String? = null,
    // 1..10 (RPE)
    val rpe: Int? = null
)

// ===== Profile and progress =====

data class UserProfile(
    val name: String,
    val startDate: LocalDate,
    val currentProgramId: String? = null,
    val weightUnit: WeightUnit = WeightUnit.KG,
    // История веса тела: дата -> вес
    val bodyWeightHistory: Map<LocalDate, Double> = emptyMap()
)

data class BodyMeasurement(
    val date: LocalDate,
    val chest: Double? = null,
    val waist: Double? = null,
    val hips: Double? = null,
    val biceps: Sided = Sided(),
    val thighs: Sided = Sided()
)

data class PersonalRecord(
    val exerciseId: String,
    val weight: Double,
    val reps: Int,
    val estimated1RM: Double,
    val date: LocalDate
)

