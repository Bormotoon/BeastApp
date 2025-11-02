package com.beast.app.data.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.beast.app.data.db.BeastDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlin.text.Charsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupImporter(
    private val database: BeastDatabase,
    private val gson: Gson = GsonBuilder().create()
) {
    suspend fun importJson(contentResolver: ContentResolver, uri: Uri) {
        val snapshot = readSnapshot(contentResolver, uri)
        restoreSnapshot(snapshot)
    }

    private suspend fun readSnapshot(contentResolver: ContentResolver, uri: Uri): BackupSnapshot {
        return withContext(Dispatchers.IO) {
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open input stream for uri=$uri")
            inputStream.use { stream ->
                stream.reader(Charsets.UTF_8).use { reader ->
                    gson.fromJson(reader, BackupSnapshot::class.java)
                }
            }
        }
    }

    private suspend fun restoreSnapshot(snapshot: BackupSnapshot) {
        withContext(Dispatchers.IO) {
            database.clearAllTables()
            database.withTransaction {
                val programDao = database.programDao()
                val workoutDao = database.workoutDao()
                val logDao = database.workoutLogDao()
                val profileDao = database.profileDao()

                if (snapshot.programs.isNotEmpty()) {
                    programDao.upsertPrograms(snapshot.programs)
                }
                if (snapshot.phases.isNotEmpty()) {
                    programDao.upsertPhases(snapshot.phases)
                }
                if (snapshot.workouts.isNotEmpty()) {
                    programDao.upsertWorkouts(snapshot.workouts)
                }
                if (snapshot.phaseWorkouts.isNotEmpty()) {
                    programDao.upsertPhaseWorkouts(snapshot.phaseWorkouts)
                }
                if (snapshot.programSchedule.isNotEmpty()) {
                    programDao.upsertSchedule(snapshot.programSchedule)
                }
                if (snapshot.exercises.isNotEmpty()) {
                    workoutDao.upsertExercises(snapshot.exercises)
                }
                if (snapshot.exerciseMappings.isNotEmpty()) {
                    workoutDao.upsertExerciseInWorkout(snapshot.exerciseMappings)
                }
                if (snapshot.workoutFavorites.isNotEmpty()) {
                    workoutDao.upsertFavorites(snapshot.workoutFavorites)
                }
                val logs = snapshot.workoutLogs.map(WorkoutLogWithSets::log)
                if (logs.isNotEmpty()) {
                    logDao.insertWorkoutLogs(logs)
                    val setLogs = snapshot.workoutLogs.flatMap(WorkoutLogWithSets::sets)
                    if (setLogs.isNotEmpty()) {
                        logDao.insertSetLogs(setLogs)
                    }
                }

                snapshot.userProfile?.let { profileDao.upsertProfile(it) }

                if (snapshot.bodyWeightEntries.isNotEmpty()) {
                    profileDao.insertBodyWeightEntries(snapshot.bodyWeightEntries)
                }
                if (snapshot.bodyMeasurements.isNotEmpty()) {
                    profileDao.insertMeasurements(snapshot.bodyMeasurements)
                }
                if (snapshot.personalRecords.isNotEmpty()) {
                    profileDao.insertPersonalRecords(snapshot.personalRecords)
                }
                if (snapshot.progressPhotos.isNotEmpty()) {
                    profileDao.insertProgressPhotos(snapshot.progressPhotos)
                }
            }
        }
    }
}
