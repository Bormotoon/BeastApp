@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.animation.ExperimentalAnimationApi::class
)

package com.beast.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.repo.ProgramRepository
import com.beast.app.domain.usecase.ImportProgramUseCase
import com.beast.app.ui.dashboard.DashboardRoute
import com.beast.app.ui.onboarding.OnboardingScreen
import com.beast.app.ui.program.ProgramScreen
import com.beast.app.ui.program.ProgramSelectionScreen
import com.beast.app.ui.theme.BeastAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )

        seedDemoProgramIfFirstRun()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val onboardingShown = prefs.getBoolean("onboarding_shown", false)
        val programSetupDone = prefs.getBoolean("program_setup_done", false)

        setContent {
            BeastAppTheme {
                AppNav(
                    onboardingShown = onboardingShown,
                    programSetupDone = programSetupDone,
                    onOnboardingFinished = {
                        prefs.edit().putBoolean("onboarding_shown", true).apply()
                    }
                )
            }
        }
    }

    private fun seedDemoProgramIfFirstRun() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val seeded = prefs.getBoolean("seed_v1_done", false)
        if (seeded) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val json = assets.open("sample_program.json").bufferedReader().use { it.readText() }
                val db = DatabaseProvider.get(applicationContext)
                val repo = ProgramRepository(db)
                val useCase = ImportProgramUseCase(repo)
                useCase(json)
                Log.i("BeastApp", "Demo program imported")
                prefs.edit().putBoolean("seed_v1_done", true).apply()
            } catch (t: Throwable) {
                Log.e("BeastApp", "Failed to seed demo program", t)
            }
        }
    }
}

@Composable
private fun AppNav(onboardingShown: Boolean, programSetupDone: Boolean, onOnboardingFinished: () -> Unit) {
    val navController = rememberNavController()
    val start = when {
        !onboardingShown -> "onboarding"
        !programSetupDone -> "program_selection"
        else -> "home"
    }
    NavHost(
        navController = navController,
        startDestination = start,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) + fadeIn()
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) + fadeOut()
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) + fadeIn()
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) + fadeOut()
        }
    ) {
        composable("onboarding") {
            OnboardingScreen(onFinish = {
                onOnboardingFinished()
                navController.navigate("program_selection") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
        composable("program_selection") {
            ProgramSelectionScreen(onStartProgram = {
                navController.navigate("home") {
                    popUpTo("program_selection") { inclusive = true }
                }
            })
        }
        composable("home") {
            DashboardRoute(
                onOpenProfile = { navController.navigate("profile") },
                onOpenSettings = { navController.navigate("settings") },
                onStartWorkout = { navController.navigate("details") },
                onViewWorkoutDetails = { navController.navigate("details") }
            )
        }
        composable("details") {
            DetailsScreen(onBack = { navController.popBackStack() })
        }
        composable("profile") {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
        composable("programs") {
            ProgramScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() }, onChangeProgram = {
                navController.navigate("program_selection")
            })
        }
    }
}

@Composable
private fun DetailsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Это экран деталей",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Навигация работает, edge-to-edge включён",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfileScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Профиль пользователя появится в следующих версиях",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit, onChangeProgram: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedCard(modifier = Modifier) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Программа", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Выбор и изменение активной программы", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                    androidx.compose.material3.Button(onClick = onChangeProgram, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Изменить программу")
                    }
                }
            }
        }
    }
}
