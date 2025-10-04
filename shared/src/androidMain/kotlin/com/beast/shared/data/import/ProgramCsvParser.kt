package com.beast.shared.data.import

import com.beast.shared.model.Program
import com.beast.shared.model.WorkoutDay
import java.io.BufferedReader
import java.io.InputStream
import java.util.UUID

/**
 * CSV Parser for importing fitness programs
 * Supports the Beast Program Import Format v1
 */
class ProgramCsvParser {

    data class ImportResult(
        val program: Program,
        val workoutDays: List<WorkoutDay>,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )

    data class ProgramMetadata(
        var programName: String = "Imported Program",
        var description: String = "",
        var author: String? = null,
        var difficulty: String? = null,
        var durationDays: Int = 0,
        var tags: List<String> = emptyList()
    )

    /**
     * Parse CSV file from InputStream
     */
    fun parse(inputStream: InputStream): ImportResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val metadata = ProgramMetadata()
        val workoutDays = mutableListOf<WorkoutDay>()

        try {
            val reader = BufferedReader(inputStream.reader())
            var lineNumber = 0
            var headerParsed = false
            var columnIndices: Map<String, Int>? = null

            reader.useLines { lines ->
                for (line in lines) {
                    lineNumber++
                    val trimmedLine = line.trim()

                    // Skip empty lines
                    if (trimmedLine.isEmpty()) continue

                    // Parse metadata lines (start with #)
                    if (trimmedLine.startsWith("#")) {
                        parseMetadata(trimmedLine, metadata)
                        continue
                    }

                    // Parse header
                    if (!headerParsed) {
                        columnIndices = parseHeader(trimmedLine)
                        headerParsed = true
                        continue
                    }

                    // Parse workout day
                    if (columnIndices != null) {
                        try {
                            val workoutDay = parseWorkoutDay(trimmedLine, columnIndices, metadata)
                            workoutDays.add(workoutDay)
                        } catch (e: Exception) {
                            errors.add("Line $lineNumber: ${e.message}")
                        }
                    }
                }
            }

            // Validation
            if (workoutDays.isEmpty()) {
                errors.add("No workout days found in CSV file")
            }

            // Check day sequence
            val dayNumbers = workoutDays.map { it.dayIndex }.sorted()
            if (dayNumbers != (1..dayNumbers.size).toList()) {
                warnings.add("Day numbers are not sequential: ${dayNumbers.joinToString(", ")}")
            }

            // Update metadata duration if not set
            if (metadata.durationDays == 0) {
                metadata.durationDays = workoutDays.size
            }

            // Create program
            val programId = "imported-${UUID.randomUUID()}"
            val program = Program(
                id = programId,
                title = metadata.programName,
                description = metadata.description,
                durationDays = metadata.durationDays,
                difficulty = metadata.difficulty,
                tags = metadata.tags,
                author = metadata.author
            )

            // Update workout days with program ID
            val updatedWorkoutDays = workoutDays.map { day ->
                day.copy(
                    id = "$programId-day-${day.dayIndex}",
                    programId = programId
                )
            }

            return ImportResult(
                program = program,
                workoutDays = updatedWorkoutDays,
                errors = errors,
                warnings = warnings
            )

        } catch (e: Exception) {
            errors.add("Failed to parse CSV: ${e.message}")
            return ImportResult(
                program = Program(
                    id = "error",
                    title = "Import Failed",
                    description = "",
                    durationDays = 0
                ),
                workoutDays = emptyList(),
                errors = errors,
                warnings = warnings
            )
        }
    }

    private fun parseMetadata(line: String, metadata: ProgramMetadata) {
        // Remove leading # and split by :
        val content = line.removePrefix("#").trim()
        val parts = content.split(":", limit = 2)

        if (parts.size == 2) {
            val key = parts[0].trim().uppercase()
            val value = parts[1].trim()

            when (key) {
                "PROGRAM_NAME" -> metadata.programName = value
                "DESCRIPTION" -> metadata.description = value
                "AUTHOR" -> metadata.author = value
                "DIFFICULTY" -> metadata.difficulty = value
                "DURATION_DAYS" -> metadata.durationDays = value.toIntOrNull() ?: 0
                "TAGS" -> metadata.tags = value.split(",").map { it.trim() }
            }
        }
    }

    private fun parseHeader(line: String): Map<String, Int> {
        val columns = parseCsvLine(line)
        return columns.mapIndexed { index, column ->
            column.lowercase().trim() to index
        }.toMap()
    }

    private fun parseWorkoutDay(
        line: String,
        columnIndices: Map<String, Int>,
        metadata: ProgramMetadata
    ): WorkoutDay {
        val values = parseCsvLine(line)

        // Required fields
        val day = getColumnValue(values, columnIndices, "day")
            ?.toIntOrNull()
            ?: throw IllegalArgumentException("Day number is required")

        val title = getColumnValue(values, columnIndices, "title")
            ?: throw IllegalArgumentException("Title is required")

        // Optional fields
        val description = getColumnValue(values, columnIndices, "description")
        val duration = getColumnValue(values, columnIndices, "duration")?.toIntOrNull()
        val exercisesStr = getColumnValue(values, columnIndices, "exercises")
        val videoUrl = getColumnValue(values, columnIndices, "video_url")
        val restDay = getColumnValue(values, columnIndices, "rest_day")?.lowercase() == "true"
        val notes = getColumnValue(values, columnIndices, "notes")

        // Parse exercises
        val exercises = if (!exercisesStr.isNullOrBlank() && !restDay) {
            exercisesStr.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        return WorkoutDay(
            id = "temp-$day", // Will be updated with program ID
            programId = "temp", // Will be updated
            dayIndex = day,
            title = title,
            durationEstimate = duration,
            exercisesOrder = exercises
        )
    }

    private fun getColumnValue(
        values: List<String>,
        columnIndices: Map<String, Int>,
        columnName: String
    ): String? {
        val index = columnIndices[columnName] ?: return null
        return if (index < values.size) values[index].trim().takeIf { it.isNotEmpty() } else null
    }

    /**
     * Parse CSV line handling quoted values
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())

        return result
    }
}

