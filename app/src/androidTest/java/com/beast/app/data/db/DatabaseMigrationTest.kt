package com.beast.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BeastDatabase::class.java,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrateAllSchemas() {
        helper.createDatabase(TEST_DB, 1).close()

        helper.runMigrationsAndValidate(TEST_DB, LATEST_VERSION, true, *DatabaseProvider.ALL_MIGRATIONS)
            .use { database ->
                database.verifyColumnExists("body_measurements", "calfLeft")
                database.verifyColumnExists("body_measurements", "calfRight")
                database.verifyTableExists("progress_photos")
                database.verifyColumnExists("user_profile", "avatarUri")
            }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.verifyColumnExists(
        table: String,
        column: String
    ) {
        query("PRAGMA table_info($table)").use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (name == column) return
            }
        }
        throw AssertionError("Column '$column' not found in table '$table'")
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.verifyTableExists(table: String) {
        query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { cursor ->
            if (cursor.moveToFirst()) return
        }
        throw AssertionError("Table '$table' not found after migration")
    }

    companion object {
        private const val TEST_DB = "migration-test.db"
        private const val LATEST_VERSION = 6
    }
}
