package com.beast.app.data.importer

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

class ProgramJsonImporter(
    private val gson: Gson = Gson()
) {
    fun parse(json: String): ProgramJsonV1 {
        try {
            val model = gson.fromJson(json, ProgramJsonV1::class.java)
            validate(model)
            return model
        } catch (e: JsonSyntaxException) {
            throw ProgramJsonValidationException("Некорректный JSON: ${e.message}")
        }
    }

    private fun validate(model: ProgramJsonV1) {
        if (model.title.isBlank()) throw ProgramJsonValidationException("Отсутствует title программы")
        if (model.durationDays <= 0) throw ProgramJsonValidationException("durationDays должен быть > 0")
        if (model.days.isEmpty()) throw ProgramJsonValidationException("Список days пуст")
        val daySet = mutableSetOf<Int>()
        model.days.forEach { day ->
            if (day.dayIndex <= 0) throw ProgramJsonValidationException("dayIndex должен быть >= 1: ${day.dayIndex}")
            if (!daySet.add(day.dayIndex)) throw ProgramJsonValidationException("Дубликат dayIndex: ${day.dayIndex}")
        }
    }
}

