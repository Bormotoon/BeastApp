package com.beast.app.data.importer

// Простые DTO для JSON импорта (v1). Поля названы под JSON (camelCase как в примерах docs/examples).

data class ProgramJsonV1(
    val version: String? = null,
    val id: String? = null,
    val title: String,
    val description: String? = null,
    val author: String? = null,
    val durationDays: Int,
    val weightUnit: String? = null,
    val phases: List<PhaseJson>? = null,
    val days: List<DayJson>
)

data class PhaseJson(
    val id: String? = null,
    val name: String,
    val durationWeeks: Int,
    val days: List<Int>? = null
)

data class DayJson(
    val dayIndex: Int,
    val title: String,
    val description: String? = null,
    val durationMinutes: Int? = null,
    val video_url: String? = null,
    val rest_day: Boolean? = null,
    val exercisesOrder: List<String>? = null,
    val notes: String? = null
)

internal fun String.slugify(): String = lowercase()
    .replace("[^a-z0-9]+".toRegex(), "-")
    .trim('-')

class ProgramJsonValidationException(message: String): IllegalArgumentException(message)

