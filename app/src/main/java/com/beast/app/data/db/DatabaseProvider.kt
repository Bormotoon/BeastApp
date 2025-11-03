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
            // Old installs may have created or altered `progress_photos` with a different schema
            // (for example: an `id` column that is nullable). Room requires the on-disk schema
            // to match the generated schema exactly. To be safe we rebuild the table:
            // 1) create a temporary table with the correct schema
            // 2) copy the data (excluding `id` so new autoincrement ids will be generated)
            // 3) drop the old table
            // 4) rename the temp table to the expected name
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS progress_photos_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    dateEpochDay INTEGER NOT NULL,
                    angle TEXT NOT NULL,
                    uri TEXT NOT NULL,
                    createdAtEpochMillis INTEGER NOT NULL,
                    notes TEXT
                )
                """.trimIndent()
            )

            // Copy data from the old table if it exists. We copy only the non-PK columns so
            // the new table will generate proper NOT NULL autoincrement ids. This is safe
            // because no other table references progress_photos via foreign key.
            try {
                db.execSQL(
                    """
                    INSERT INTO progress_photos_new (dateEpochDay, angle, uri, createdAtEpochMillis, notes)
                    SELECT dateEpochDay, angle, uri, createdAtEpochMillis, notes FROM progress_photos
                    """.trimIndent()
                )
            } catch (ignored: Exception) {
                // If the source table doesn't exist or columns differ, ignore and continue
            }

            // Replace old table with the new one.
            db.execSQL("DROP TABLE IF EXISTS progress_photos")
            db.execSQL("ALTER TABLE progress_photos_new RENAME TO progress_photos")
        }
    }

    internal val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6
    )

    fun get(context: Context): BeastDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = buildDatabase(context.applicationContext)
            INSTANCE = instance
            instance
        }
    }

    internal fun buildDatabase(context: Context, name: String = "beast.db"): BeastDatabase {
        return Room.databaseBuilder(
            context,
            BeastDatabase::class.java,
            name
        ).addMigrations(*ALL_MIGRATIONS).build()
    }
}
