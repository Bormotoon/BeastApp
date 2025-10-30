@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.animation.ExperimentalAnimationApi::class
)

package com.beast.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.beast.app.ui.theme.BeastAppTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.lifecycle.lifecycleScope
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.repo.ProgramRepository
import com.beast.app.domain.usecase.ImportProgramUseCase
import com.beast.app.ui.program.ProgramScreen
import com.beast.app.ui.onboarding.OnboardingScreen

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

        setContent {
            BeastAppTheme {
                AppNav(onboardingShown = onboardingShown, onOnboardingFinished = {
                    prefs.edit().putBoolean("onboarding_shown", true).apply()
                })
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
private fun AppNav(onboardingShown: Boolean, onOnboardingFinished: () -> Unit) {
    val navController = rememberNavController()
    val start = if (!onboardingShown) "onboarding" else "home"
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
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
        composable("home") {
            HomeScreen(
                onToggle = {},
                onOpenDetails = { navController.navigate("details") },
                onOpenPrograms = { navController.navigate("programs") }
            )
        }
        composable("details") {
            DetailsScreen(onBack = { navController.popBackStack() })
        }
        composable("programs") {
            ProgramScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun HomeScreen(onToggle: () -> Unit, onOpenDetails: () -> Unit, onOpenPrograms: () -> Unit) {
    var expanded by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BeastApp", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { expanded = !expanded; onToggle() }) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenPrograms,
                text = { Text("Программы") },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = null) }
            )
        }
    ) { innerPadding ->
        BeastContent(innerPadding, expanded)
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
private fun BeastContent(padding: PaddingValues, expanded: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
            ),
            exit = fadeOut() + shrinkVertically(
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            )
        ) {
            ElevatedCard(modifier = Modifier) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "BeastApp готов к работе!",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Material 3 • Expressive • Анимации",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewBeast() {
    BeastAppTheme { HomeScreen(onToggle = {}, onOpenDetails = {}, onOpenPrograms = {}) }
}
