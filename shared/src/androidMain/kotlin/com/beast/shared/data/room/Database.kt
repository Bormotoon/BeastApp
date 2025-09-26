package com.beast.shared.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProgramEntity::class,
        WorkoutDayEntity::class,
        ExerciseEntity::class,
        WorkoutLogEntity::class,
        SetLogEntity::class,
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(ListConverters::class)
abstract class FitDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun workoutDayDao(): WorkoutDayDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun setLogDao(): SetLogDao
}

class DatabaseFactory(private val context: Context) {
    fun create(): FitDatabase = Room.databaseBuilder(
        context.applicationContext,
        FitDatabase::class.java,
        "beast_fit.db"
    ).addTypeConverter(ListConverters())
     .fallbackToDestructiveMigration()
     .build()
}

