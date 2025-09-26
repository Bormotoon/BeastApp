package com.beast.shared.usecase

import com.beast.shared.model.WorkoutLog
import com.beast.shared.repository.WorkoutLogRepository
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeLogsRepo(private val logs: List<WorkoutLog>) : WorkoutLogRepository {
    override suspend fun getLogsForProgram(programId: String) = logs
    override suspend fun getLogsForDay(programId: String, dayIndex: Int) = logs.filter { it.dayIndex == dayIndex }
    override suspend fun upsert(log: WorkoutLog, sets: List<com.beast.shared.model.SetLog>) {}
}

class CalculateProgressUseCaseTest {
    @Test
    fun testProgress() {
        val logs = listOf(
            WorkoutLog("1","p",1,0,true,null),
            WorkoutLog("2","p",2,0,false,null),
            WorkoutLog("3","p",3,0,true,null),
        )
        val uc = CalculateProgressUseCase(FakeLogsRepo(logs))
        val res = kotlin.run { kotlinx.coroutines.runBlocking { uc("p", totalDays = 90) } }
        assertEquals(2, res.completedDays)
        assertEquals(90, res.totalDays)
        assertEquals((2.0/90*100).toInt(), res.percent)
    }
}

