package com.beast.app.utils

import android.text.format.DateFormat
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Locale

object DateFormatting {

    fun dateFormatter(locale: Locale = Locale.getDefault(), skeleton: String): DateTimeFormatter {
        val bestPattern = DateFormat.getBestDateTimePattern(locale, skeleton)
        return DateTimeFormatter.ofPattern(bestPattern, locale)
    }

    fun format(
        temporal: TemporalAccessor,
        locale: Locale = Locale.getDefault(),
        skeleton: String,
        capitalizeFirst: Boolean = false
    ): String {
        val formatted = dateFormatter(locale, skeleton).format(temporal)
        return if (capitalizeFirst) capitalize(formatted, locale) else formatted
    }

    fun capitalize(value: String, locale: Locale = Locale.getDefault()): String {
        return value.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
    }
}
