package com.beast.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProgramEntity::class,
        PhaseEntity::class,
        WorkoutEntity::class,
        PhaseWorkoutCrossRefEntity::class,
        ExerciseEntity::class,
        ExerciseInWorkoutEntity::class,
        ProgramScheduleEntity::class,
        WorkoutLogEntity::class,
        SetLogEntity::class,
        UserProfileEntity::class,
        BodyWeightEntryEntity::class,
        BodyMeasurementEntity::class,
        PersonalRecordEntity::class,
        WorkoutFavoriteEntity::class,
        ProgressPhotoEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BeastDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun profileDao(): ProfileDao
}
