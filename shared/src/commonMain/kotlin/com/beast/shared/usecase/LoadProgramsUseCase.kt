package com.beast.shared.usecase

import com.beast.shared.model.Program
import com.beast.shared.repository.ProgramRepository

class LoadProgramsUseCase(private val repo: ProgramRepository) {
    suspend operator fun invoke(): List<Program> = repo.getAll()
}

