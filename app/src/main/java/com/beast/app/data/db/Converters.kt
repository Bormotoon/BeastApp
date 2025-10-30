package com.beast.app.data.db

import androidx.room.TypeConverter
import com.beast.app.model.*
import java.time.Instant
import java.time.LocalDate

/**
 * Room TypeConverters для списков, дат и enum-ов.
 */
object Converters {
    private const val SEP = "|" // простой разделитель для списков строк

    @TypeConverter
    @JvmStatic
    fun stringListToString(list: List<String>?): String? = list?.joinToString(SEP)

    @TypeConverter
    @JvmStatic
    fun stringToStringList(data: String?): List<String> = data?.takeIf { it.isNotEmpty() }?.split(SEP) ?: emptyList()

    // java.time
    @TypeConverter
    @JvmStatic
    fun instantToLong(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    @JvmStatic
    fun longToInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    @JvmStatic
    fun localDateToLong(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    @JvmStatic
    fun longToLocalDate(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    // Enums -> String
    @TypeConverter
    @JvmStatic
    fun exerciseTypeToString(value: ExerciseType?): String? = value?.name
    @TypeConverter
    @JvmStatic
    fun stringToExerciseType(value: String?): ExerciseType? = value?.let { ExerciseType.valueOf(it) }

    @TypeConverter
    @JvmStatic
    fun setTypeToString(value: SetType?): String? = value?.name
    @TypeConverter
    @JvmStatic
    fun stringToSetType(value: String?): SetType? = value?.let { SetType.valueOf(it) }

    @TypeConverter
    @JvmStatic
    fun workoutStatusToString(value: WorkoutStatus?): String? = value?.name
    @TypeConverter
    @JvmStatic
    fun stringToWorkoutStatus(value: String?): WorkoutStatus? = value?.let { WorkoutStatus.valueOf(it) }

    @TypeConverter
    @JvmStatic
    fun sideToString(value: Side?): String? = value?.name
    @TypeConverter
    @JvmStatic
    fun stringToSide(value: String?): Side? = value?.let { Side.valueOf(it) }

    @TypeConverter
    @JvmStatic
    fun weightUnitToString(value: WeightUnit?): String? = value?.name
    @TypeConverter
    @JvmStatic
    fun stringToWeightUnit(value: String?): WeightUnit? = value?.let { WeightUnit.valueOf(it) }
}
