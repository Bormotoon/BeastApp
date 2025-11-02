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

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE user_profile ADD COLUMN avatarUri TEXT")
            db.execSQL("ALTER TABLE user_profile ADD COLUMN heightCm REAL")
            db.execSQL("ALTER TABLE user_profile ADD COLUMN age INTEGER")
            db.execSQL("ALTER TABLE user_profile ADD COLUMN gender TEXT")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE body_measurements ADD COLUMN calfLeft REAL")
            db.execSQL("ALTER TABLE body_measurements ADD COLUMN calfRight REAL")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS progress_photos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    dateEpochDay INTEGER NOT NULL,
                    angle TEXT NOT NULL,
                    uri TEXT NOT NULL,
                    createdAtEpochMillis INTEGER NOT NULL,
                    notes TEXT
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
            INSTANCE = instance
            instance
        }
    }
}
