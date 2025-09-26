package com.beast.shared.usecase

import kotlin.test.Test
import kotlin.test.assertEquals

class PlanCycleUseCaseTest {
    @Test
    fun testPlan90Days() {
        val uc = PlanCycleUseCase()
        val start = 1_700_000_000_000L
        val res = uc(durationDays = 90, startDateMillis = start)
        assertEquals(90, res.days.size)
        assertEquals(start, res.days.first().dateMillis)
        assertEquals(start + 89 * 86_400_000L, res.days.last().dateMillis)
    }
}

