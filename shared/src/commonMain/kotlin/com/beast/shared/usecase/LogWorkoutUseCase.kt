package com.beast.shared.usecase

import com.beast.shared.model.SetLog
import com.beast.shared.model.WorkoutLog
import com.beast.shared.repository.WorkoutLogRepository

class LogWorkoutUseCase(private val repo: WorkoutLogRepository) {
    suspend operator fun invoke(log: WorkoutLog, sets: List<SetLog>) {
        repo.upsert(log, sets)
    }
}

