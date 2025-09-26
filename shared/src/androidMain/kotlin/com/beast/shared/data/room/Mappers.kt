package com.beast.shared.data.room

import com.beast.shared.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

private val json = Json

fun ProgramEntity.toModel(): Program = Program(
    id, title, description, durationDays, difficulty, thumbnail,
    tags = json.decodeFromString(ListSerializer(String.serializer()), tagsJson),
    author = author
)

fun Program.toEntity(): ProgramEntity = ProgramEntity(
    id, title, description, durationDays, difficulty, thumbnail,
    tagsJson = json.encodeToString(ListSerializer(String.serializer()), tags),
    author = author
)

fun WorkoutDayEntity.toModel(): WorkoutDay = WorkoutDay(
    id, programId, dayIndex, title, durationEstimate,
    exercisesOrder = json.decodeFromString(ListSerializer(String.serializer()), exercisesOrderJson)
)

fun WorkoutDay.toEntity(): WorkoutDayEntity = WorkoutDayEntity(
    id, programId, dayIndex, title, durationEstimate,
    exercisesOrderJson = json.encodeToString(ListSerializer(String.serializer()), exercisesOrder)
)

fun ExerciseEntity.toModel(): Exercise = Exercise(
    id, name,
    primaryMuscles = json.decodeFromString(ListSerializer(String.serializer()), primaryMusclesJson),
    equipment = json.decodeFromString(ListSerializer(String.serializer()), equipmentJson),
    demoVideoUrl, defaultSets, defaultReps, restSec
)

fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id, name,
    primaryMusclesJson = json.encodeToString(ListSerializer(String.serializer()), primaryMuscles),
    equipmentJson = json.encodeToString(ListSerializer(String.serializer()), equipment),
    demoVideoUrl, defaultSets, defaultReps, restSec
)

fun WorkoutLogEntity.toModel(): WorkoutLog = WorkoutLog(id, programId, dayIndex, date, completed, notes)
fun WorkoutLog.toEntity(): WorkoutLogEntity = WorkoutLogEntity(id, programId, dayIndex, date, completed, notes)

fun SetLogEntity.toModel(): SetLog = SetLog(id, workoutDayId, exerciseId, setIndex, reps, weight, rpe, timestamp)
fun SetLog.toEntity(): SetLogEntity = SetLogEntity(id, workoutDayId, exerciseId, setIndex, reps, weight, rpe, timestamp)

