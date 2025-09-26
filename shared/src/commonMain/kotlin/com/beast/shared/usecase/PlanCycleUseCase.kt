package com.beast.shared.usecase

import kotlin.math.min

class PlanCycleUseCase {
    data class DayPlan(val dayIndex: Int, val dateMillis: Long)
    data class Result(val startDateMillis: Long, val days: List<DayPlan>)

    operator fun invoke(durationDays: Int, startDateMillis: Long): Result {
        val days = (0 until durationDays).map { i ->
            DayPlan(dayIndex = i + 1, dateMillis = startDateMillis + i * 86_400_000L)
        }
        return Result(startDateMillis, days)
    }
}

