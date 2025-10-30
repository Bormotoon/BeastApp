@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.beast.app.ui.program

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.db.ProgramEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProgramScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var programs by remember { mutableStateOf<List<ProgramEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        programs = loadPrograms(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Программы", style = MaterialTheme.typography.titleLarge) })
        }
    ) { innerPadding ->
        if (programs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Пока нет программ\n(демо импортируется при первом запуске)",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(programs) { program ->
                    ProgramItem(program = program, onClick = { /* TODO: открыть детали */ })
                }
            }
        }
    }
}

@Composable
private fun ProgramItem(program: ProgramEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = program.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(text = "Длительность: ${program.durationDays} дн.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private suspend fun loadPrograms(context: Context): List<ProgramEntity> = withContext(Dispatchers.IO) {
    val db = DatabaseProvider.get(context)
    db.programDao().getAllPrograms()
}
