package com.beast.shared.usecase

import com.beast.shared.repository.WorkoutLogRepository

class CalculateProgressUseCase(private val logsRepo: WorkoutLogRepository) {
    data class Result(val completedDays: Int, val totalDays: Int, val percent: Int)

    suspend operator fun invoke(programId: String, totalDays: Int): Result {
        val logs = logsRepo.getLogsForProgram(programId)
        val completed = logs.count { it.completed }
        val percent = if (totalDays > 0) ((completed.toDouble() / totalDays) * 100).toInt() else 0
        return Result(completed, totalDays, percent.coerceIn(0, 100))
    }
}
