package com.beast.app.ui.import

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.repo.ProgramRepository
import com.beast.app.domain.usecase.ImportProgramUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ImportState {
    object Idle : ImportState()
    object InProgress : ImportState()
    object Success : ImportState()
    data class Error(val message: String) : ImportState()
}

class ProgramImportViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DatabaseProvider.get(application.applicationContext)
    private val programRepository = ProgramRepository(database)
    private val importProgramUseCase = ImportProgramUseCase(programRepository)

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    fun importFromUri(uri: Uri) {
        _state.value = ImportState.InProgress
        viewModelScope.launch {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                } ?: throw Exception("Не удалось прочитать файл")

                importProgramUseCase(jsonString)
                _state.value = ImportState.Success
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    fun resetState() {
        _state.value = ImportState.Idle
    }
}
