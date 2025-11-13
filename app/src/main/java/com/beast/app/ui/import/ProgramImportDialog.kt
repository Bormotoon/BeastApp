package com.beast.app.ui.import

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beast.app.domain.usecase.ImportProgramUseCase

@Composable
fun ProgramImportDialog(
    onDismiss: () -> Unit,
    onImportSuccess: () -> Unit,
    viewModel: ProgramImportViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importFromUri(it)
        }
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.resetState()
            onDismiss()
        },
        title = { Text("Импорт программы") },
        text = {
            when (state) {
                is ImportState.Idle -> {
                    Text("Выберите JSON файл с программой для импорта.")
                }
                is ImportState.InProgress -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Импорт программы...")
                    }
                }
                is ImportState.Success -> {
                    Text("Программа успешно импортирована!")
                }
                is ImportState.Error -> {
                    Text("Ошибка импорта: ${(state as ImportState.Error).message}")
                }
            }
        },
        confirmButton = {
            when (state) {
                is ImportState.Idle -> {
                    Button(onClick = { filePickerLauncher.launch("application/json") }) {
                        Text("Выбрать файл")
                    }
                }
                is ImportState.Success -> {
                    Button(onClick = {
                        onImportSuccess()
                        onDismiss()
                    }) {
                        Text("OK")
                    }
                }
                else -> {
                    // No confirm button for InProgress or Error
                }
            }
        },
        dismissButton = {
            if (state !is ImportState.InProgress) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        }
    )
}