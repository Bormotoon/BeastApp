package com.beast.app.domain.usecase

import com.beast.app.data.repo.ProgramRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImportProgramUseCase(
    private val repo: ProgramRepository
) {
    suspend operator fun invoke(json: String): ProgramRepository.ImportResult = withContext(Dispatchers.IO) {
        repo.importFromJson(json)
    }
}
