package com.beast.app.ui.program

import com.beast.app.data.db.ProgramEntity
import java.time.LocalDate

data class ProgramUiState(
    val isLoading: Boolean = false,
    val programs: List<ProgramEntity> = emptyList(),
    val errorMessage: String? = null
)