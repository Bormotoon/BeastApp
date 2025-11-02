package com.beast.app.data.backup

import android.content.ContentResolver
import android.net.Uri
import com.beast.app.data.db.BodyMeasurementEntity
import com.beast.app.data.db.BodyWeightEntryEntity
import com.beast.app.data.db.BeastDatabase
import com.beast.app.data.db.ExerciseEntity
import com.beast.app.data.db.ExerciseInWorkoutEntity
import com.beast.app.data.db.PersonalRecordEntity
import com.beast.app.data.db.PhaseEntity
import com.beast.app.data.db.PhaseWorkoutCrossRefEntity
import com.beast.app.data.db.ProgramEntity
import com.beast.app.data.db.ProgramScheduleEntity
import com.beast.app.data.db.ProgressPhotoEntity
import com.beast.app.data.db.SetLogEntity
import com.beast.app.data.db.UserProfileEntity
import com.beast.app.data.db.WorkoutEntity
import com.beast.app.data.db.WorkoutFavoriteEntity
import com.beast.app.data.db.WorkoutLogEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.text.Charsets

enum class DataExportFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String
) {
    JSON(extension = "json", mimeType = "application/json", displayName = "JSON"),
    CSV_ARCHIVE(extension = "zip", mimeType = "application/zip", displayName = "CSV (ZIP)");

    fun defaultFileName(nowMillis: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
        return "beast-backup-${formatter.format(Date(nowMillis))}.$extension"
    }
}

data class BackupMetadata(
    val schemaVersion: Int = 1,
    val exportedAtEpochMillis: Long,
    val exporterVersion: Int = 1
)

data class WorkoutLogWithSets(
    val log: WorkoutLogEntity,
    val sets: List<SetLogEntity>
)

data class BackupSnapshot(
    val metadata: BackupMetadata,
    val programs: List<ProgramEntity>,
    val phases: List<PhaseEntity>,
    val workouts: List<WorkoutEntity>,
    val phaseWorkouts: List<PhaseWorkoutCrossRefEntity>,
    val programSchedule: List<ProgramScheduleEntity>,
    val exercises: List<ExerciseEntity>,
    val exerciseMappings: List<ExerciseInWorkoutEntity>,
    val workoutFavorites: List<WorkoutFavoriteEntity>,
    val workoutLogs: List<WorkoutLogWithSets>,
    val userProfile: UserProfileEntity?,
    val bodyWeightEntries: List<BodyWeightEntryEntity>,
    val bodyMeasurements: List<BodyMeasurementEntity>,
    val personalRecords: List<PersonalRecordEntity>,
    val progressPhotos: List<ProgressPhotoEntity>
)

class BackupExporter(
    private val database: BeastDatabase,
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
) {
    suspend fun exportJson(contentResolver: ContentResolver, uri: Uri) {
        writeToUri(contentResolver, uri) { stream ->
            val snapshot = collectSnapshot()
            stream.writer(Charsets.UTF_8).use { writer ->
                writer.write(gson.toJson(snapshot))
            }
        }
    }

    suspend fun exportCsvArchive(contentResolver: ContentResolver, uri: Uri) {
        writeToUri(contentResolver, uri) { stream ->
            val snapshot = collectSnapshot()
            writeCsvArchive(snapshot, stream)
        }
    }

    private suspend fun writeToUri(
        contentResolver: ContentResolver,
        uri: Uri,
        block: suspend (OutputStream) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val output = contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Cannot open output stream for uri=$uri")
            output.use { stream ->
                block(stream)
            }
        }
    }

    private suspend fun collectSnapshot(): BackupSnapshot {
        return withContext(Dispatchers.IO) {
            val programDao = database.programDao()
            val workoutDao = database.workoutDao()
            val logDao = database.workoutLogDao()
            val profileDao = database.profileDao()

            val metadata = BackupMetadata(exportedAtEpochMillis = System.currentTimeMillis())
            val programs = programDao.getAllPrograms()
            val phases = programDao.getAllPhases()
            val schedule = programDao.getAllSchedules()
            val phaseWorkouts = programDao.getAllPhaseWorkouts()
            val workouts = workoutDao.getAllWorkouts()
            val exercises = workoutDao.getAllExercises()
            val exerciseMappings = workoutDao.getAllExerciseMappings()
            val favorites = workoutDao.getAllFavorites()
            val workoutLogs = logDao.getAllWorkoutLogs()
            val setLogsByWorkout = logDao.getAllSetLogs().groupBy { it.workoutLogId }
            val logsWithSets = workoutLogs.map { entity ->
                WorkoutLogWithSets(entity, setLogsByWorkout[entity.id].orEmpty())
            }
            val profile = profileDao.getProfile()
            val bodyWeight = profileDao.getBodyWeightEntries()
            val measurements = profileDao.getBodyMeasurements()
            val personalRecords = profileDao.getAllPersonalRecords()
            val photos = profileDao.getProgressPhotos()

            BackupSnapshot(
                metadata = metadata,
                programs = programs,
                phases = phases,
                workouts = workouts,
                phaseWorkouts = phaseWorkouts,
                programSchedule = schedule,
                exercises = exercises,
                exerciseMappings = exerciseMappings,
                workoutFavorites = favorites,
                workoutLogs = logsWithSets,
                userProfile = profile,
                bodyWeightEntries = bodyWeight,
                bodyMeasurements = measurements,
                personalRecords = personalRecords,
                progressPhotos = photos
            )
        }
    }

    private fun writeCsvArchive(snapshot: BackupSnapshot, output: OutputStream) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write(gson.toJson(snapshot.metadata).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.writeCsv(
                "programs.csv",
                listOf("name", "durationDays"),
                snapshot.programs.map { listOf(it.name, it.durationDays.toString()) }
            )

            zip.writeCsv(
                "phases.csv",
                listOf("programName", "name", "durationWeeks"),
                snapshot.phases.map { listOf(it.programName, it.name, it.durationWeeks.toString()) }
            )

            zip.writeCsv(
                "program_schedule.csv",
                listOf("programName", "dayNumber", "workoutId"),
                snapshot.programSchedule.map { entry ->
                    listOf(entry.programName, entry.dayNumber.toString(), entry.workoutId)
                }
            )

            zip.writeCsv(
                "phase_workouts.csv",
                listOf("programName", "phaseName", "workoutId"),
                snapshot.phaseWorkouts.map { ref ->
                    listOf(ref.programName, ref.phaseName, ref.workoutId)
                }
            )

            zip.writeCsv(
                "workouts.csv",
                listOf("id", "name", "durationMinutes", "targetMuscleGroups"),
                snapshot.workouts.map { workout ->
                    listOf(
                        workout.id,
                        workout.name,
                        workout.durationMinutes.toString(),
                        workout.targetMuscleGroups.joinToString("|")
                    )
                }
            )

            zip.writeCsv(
                "exercises.csv",
                listOf(
                    "id",
                    "name",
                    "exerciseType",
                    "primaryMuscleGroup",
                    "equipment",
                    "instructions",
                    "videoUrl"
                ),
                snapshot.exercises.map { exercise ->
                    listOf(
                        exercise.id,
                        exercise.name,
                        exercise.exerciseType,
                        exercise.primaryMuscleGroup,
                        exercise.equipment.joinToString("|"),
                        exercise.instructions.orEmpty(),
                        exercise.videoUrl.orEmpty()
                    )
                }
            )

            zip.writeCsv(
                "exercise_in_workout.csv",
                listOf("workoutId", "orderIndex", "exerciseId", "setType", "targetReps", "notes"),
                snapshot.exerciseMappings.map { mapping ->
                    listOf(
                        mapping.workoutId,
                        mapping.orderIndex.toString(),
                        mapping.exerciseId,
                        mapping.setType,
                        mapping.targetReps,
                        mapping.notes.orEmpty()
                    )
                }
            )

            zip.writeCsv(
                "favorite_workouts.csv",
                listOf("workoutId", "addedAtEpochMillis"),
                snapshot.workoutFavorites.map { favorite ->
                    listOf(favorite.workoutId, favorite.addedAtEpochMillis.toString())
                }
            )

            zip.writeCsv(
                "workout_logs.csv",
                listOf(
                    "id",
                    "workoutId",
                    "dateEpochMillis",
                    "totalDuration",
                    "totalVolume",
                    "totalReps",
                    "calories",
                    "notes",
                    "rating",
                    "status"
                ),
                snapshot.workoutLogs.map { item ->
                    val log = item.log
                    listOf(
                        log.id,
                        log.workoutId,
                        log.dateEpochMillis.toString(),
                        log.totalDuration.toString(),
                        log.totalVolume.toString(),
                        log.totalReps.toString(),
                        log.calories?.toString().orEmpty(),
                        log.notes.orEmpty(),
                        log.rating?.toString().orEmpty(),
                        log.status
                    )
                }
            )

            zip.writeCsv(
                "set_logs.csv",
                listOf(
                    "id",
                    "workoutLogId",
                    "exerciseId",
                    "setNumber",
                    "weight",
                    "reps",
                    "durationSeconds",
                    "distance",
                    "side",
                    "isCompleted",
                    "notes",
                    "rpe"
                ),
                snapshot.workoutLogs.flatMap { item ->
                    item.sets.map { set ->
                        listOf(
                            set.id,
                            set.workoutLogId,
                            set.exerciseId,
                            set.setNumber.toString(),
                            set.weight?.toString().orEmpty(),
                            set.reps?.toString().orEmpty(),
                            set.durationSeconds?.toString().orEmpty(),
                            set.distance?.toString().orEmpty(),
                            set.side,
                            set.isCompleted.toString(),
                            set.notes.orEmpty(),
                            set.rpe?.toString().orEmpty()
                        )
                    }
                }
            )

            zip.writeCsv(
                "user_profile.csv",
                listOf(
                    "id",
                    "name",
                    "startDateEpochDay",
                    "currentProgramId",
                    "weightUnit",
                    "avatarUri",
                    "heightCm",
                    "age",
                    "gender"
                ),
                snapshot.userProfile?.let { profile ->
                    listOf(
                        listOf(
                            profile.id.toString(),
                            profile.name,
                            profile.startDateEpochDay.toString(),
                            profile.currentProgramId.orEmpty(),
                            profile.weightUnit,
                            profile.avatarUri.orEmpty(),
                            profile.heightCm?.toString().orEmpty(),
                            profile.age?.toString().orEmpty(),
                            profile.gender.orEmpty()
                        )
                    )
                } ?: emptyList()
            )

            zip.writeCsv(
                "body_weight.csv",
                listOf("id", "dateEpochDay", "weight"),
                snapshot.bodyWeightEntries.map { entry ->
                    listOf(entry.id.toString(), entry.dateEpochDay.toString(), entry.weight.toString())
                }
            )

            zip.writeCsv(
                "body_measurements.csv",
                listOf(
                    "id",
                    "dateEpochDay",
                    "chest",
                    "waist",
                    "hips",
                    "bicepsLeft",
                    "bicepsRight",
                    "thighsLeft",
                    "thighsRight",
                    "calfLeft",
                    "calfRight"
                ),
                snapshot.bodyMeasurements.map { entry ->
                    listOf(
                        entry.id.toString(),
                        entry.dateEpochDay.toString(),
                        entry.chest?.toString().orEmpty(),
                        entry.waist?.toString().orEmpty(),
                        entry.hips?.toString().orEmpty(),
                        entry.bicepsLeft?.toString().orEmpty(),
                        entry.bicepsRight?.toString().orEmpty(),
                        entry.thighsLeft?.toString().orEmpty(),
                        entry.thighsRight?.toString().orEmpty(),
                        entry.calfLeft?.toString().orEmpty(),
                        entry.calfRight?.toString().orEmpty()
                    )
                }
            )

            zip.writeCsv(
                "personal_records.csv",
                listOf("id", "exerciseId", "weight", "reps", "estimated1RM", "dateEpochDay"),
                snapshot.personalRecords.map { record ->
                    listOf(
                        record.id.toString(),
                        record.exerciseId,
                        record.weight.toString(),
                        record.reps.toString(),
                        record.estimated1RM.toString(),
                        record.dateEpochDay.toString()
                    )
                }
            )

            zip.writeCsv(
                "progress_photos.csv",
                listOf("id", "dateEpochDay", "angle", "uri", "createdAtEpochMillis", "notes"),
                snapshot.progressPhotos.map { photo ->
                    listOf(
                        photo.id.toString(),
                        photo.dateEpochDay.toString(),
                        photo.angle,
                        photo.uri,
                        photo.createdAtEpochMillis.toString(),
                        photo.notes.orEmpty()
                    )
                }
            )
        }
    }

    private fun ZipOutputStream.writeCsv(name: String, headers: List<String>, rows: List<List<String>>) {
        putNextEntry(ZipEntry(name))
        val builder = StringBuilder()
        builder.appendLine(headers.joinToString(separator = ",") { it.escapeCsv() })
        for (row in rows) {
            builder.appendLine(row.joinToString(separator = ",") { it.escapeCsv() })
        }
        write(builder.toString().toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun String.escapeCsv(): String {
        var needsQuotes = false
        for (ch in this) {
            if (ch == ',' || ch == '"' || ch == '\n' || ch == '\r') {
                needsQuotes = true
                break
            }
        }
        if (!needsQuotes) return this
        val escaped = StringBuilder(length + 2)
        escaped.append('"')
        for (ch in this) {
            if (ch == '"') {
                escaped.append("\"\"")
            } else {
                escaped.append(ch)
            }
        }
        escaped.append('"')
        return escaped.toString()
    }
}
