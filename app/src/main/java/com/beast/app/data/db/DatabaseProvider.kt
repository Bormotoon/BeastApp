package com.beast.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    @Volatile
    private var INSTANCE: BeastDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // no-op: стартовая миграция (структура не менялась), но регистрируем для корректного апдейта
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS favorite_workouts (
                    workoutId TEXT NOT NULL PRIMARY KEY,
                    addedAtEpochMillis INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    fun get(context: Context): BeastDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                BeastDatabase::class.java,
                "beast.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
            INSTANCE = instance
            instance
        }
    }
}
