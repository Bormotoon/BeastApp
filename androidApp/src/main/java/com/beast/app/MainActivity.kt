package com.beast.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appState: AppStateViewModel = hiltViewModel()
            val accent by appState.accentColor.collectAsState(initial = "#2E7D32")
            BeastTheme(accentHex = accent) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    BeastApp()
                }
            }
        }
    }
}
