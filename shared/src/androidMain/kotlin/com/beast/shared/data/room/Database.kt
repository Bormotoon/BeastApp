package com.beast.shared.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProgramEntity::class,
        WorkoutDayEntity::class,
        ExerciseEntity::class,
        WorkoutLogEntity::class,
        SetLogEntity::class,
        MeasurementEntity::class,
        PhotoProgressEntity::class,
    ],
    version = 2,
    exportSchema = false
)
abstract class FitDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun workoutDayDao(): WorkoutDayDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun setLogDao(): SetLogDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun photoProgressDao(): PhotoProgressDao
}

class DatabaseFactory(private val context: Context) {
    fun create(): FitDatabase = Room.databaseBuilder(
        context.applicationContext,
        FitDatabase::class.java,
        "beast_fit.db"
    )
     .fallbackToDestructiveMigration()
     .build()
}
