@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.animation.ExperimentalAnimationApi::class
)

package com.beast.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.repo.ProfileRepository
import kotlinx.coroutines.launch
import com.beast.app.ui.activeworkout.ActiveWorkoutResult
import com.beast.app.ui.activeworkout.ActiveWorkoutRoute
import com.beast.app.ui.calendar.CalendarRoute
import com.beast.app.ui.dashboard.DashboardRoute
import com.beast.app.ui.onboarding.OnboardingScreen
import com.beast.app.ui.program.ProgramRoute
import com.beast.app.ui.photoprogress.PhotoProgressRoute
import com.beast.app.ui.program.ProgramSelectionScreen
import com.beast.app.ui.theme.BeastAppTheme
import com.beast.app.ui.progress.ProgressRoute
import com.beast.app.ui.profile.ProfileRoute
import com.beast.app.ui.settings.SettingsRoute
import com.beast.app.ui.workoutcompletion.WorkoutCompletionRoute
import com.beast.app.ui.workoutcompletion.WorkoutCompletionViewModel
import com.beast.app.ui.workoutdetail.WorkoutDetailRoute
import com.beast.app.ui.workouthistory.WorkoutHistoryRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        OfflineStrictMode.enforce(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val onboardingShown = prefs.getBoolean("onboarding_shown", false)
        val programSetupDone = prefs.getBoolean("program_setup_done", false)
        val initialRoute = when {
            !onboardingShown -> "onboarding"
            !programSetupDone -> "program_selection"
            else -> "home"
        }

        setContent {
            BeastAppTheme {
                AppNav(
                    initialRoute = initialRoute,
                    onMarkOnboardingShown = {
                        prefs.edit().putBoolean("onboarding_shown", true).apply()
                    },
                    onMarkProgramSetupDone = {
                        prefs.edit().putBoolean("program_setup_done", true).apply()
                    }
                )
            }
        }
    }
}

@Composable
private fun AppNav(
    initialRoute: String,
    onMarkOnboardingShown: () -> Unit,
    onMarkProgramSetupDone: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = initialRoute,
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
                onMarkOnboardingShown()
                navController.navigate("program_selection") { popUpTo("onboarding") { inclusive = true } }
            })
        }
        composable("program_selection") {
            ProgramSelectionScreen(onStartProgram = {
                onMarkProgramSetupDone()
                navController.popBackStack("home", inclusive = true)
                navController.navigate("home") {
                    popUpTo("program_selection") { inclusive = true }
                    launchSingleTop = true
                }
            })
        }
        composable("home") {
            DashboardRoute(
                onNavigateHome = {
                    navController.navigate("home") {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenCalendar = {
                    navController.navigate("calendar") { launchSingleTop = true }
                },
                onOpenProgram = {
                    navController.navigate("programs") { launchSingleTop = true }
                },
                onOpenProgress = {
                    navController.navigate("progress") { launchSingleTop = true }
                },
                onOpenProfile = {
                    navController.navigate("profile") { launchSingleTop = true }
                },
                onOpenSettings = {
                    navController.navigate("settings") { launchSingleTop = true }
                },
                onStartWorkout = { workoutId ->
                    navController.navigate("active_workout/$workoutId") { launchSingleTop = true }
                },
                onViewWorkoutDetails = { workoutId ->
                    navController.navigate("workout/$workoutId") { launchSingleTop = true }
                }
            )
        }
        composable(
            route = "active_workout/{workoutId}",
            arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
        ) {
            ActiveWorkoutRoute(
                onBack = { navController.popBackStack() },
                onWorkoutCompleted = { result ->
                    navController.navigate("workout_complete") { launchSingleTop = true }
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        WorkoutCompletionViewModel.KEY_RESULT,
                        result
                    )
                    navController.previousBackStackEntry?.savedStateHandle?.remove<ActiveWorkoutResult>(
                        WorkoutCompletionViewModel.KEY_RESULT
                    )
                }
            )
        }
        composable("workout_complete") {
            WorkoutCompletionRoute(
                onBackToHome = {
                    navController.popBackStack("home", inclusive = false)
                },
                onDiscard = {
                    navController.popBackStack("home", inclusive = false)
                }
            )
        }
        composable(
            route = "workout/{workoutId}",
            arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
        ) {
            WorkoutDetailRoute(
                onBack = { navController.popBackStack() },
                onStartWorkout = { workoutId ->
                    navController.navigate("active_workout/$workoutId") { launchSingleTop = true }
                }
            )
        }
        composable("profile") {
            ProfileRoute(
                onBack = { navController.popBackStack() },
                onOpenPhotoProgress = { navController.navigate("photo_progress") }
            )
        }
        composable("calendar") {
            CalendarRoute(
                onBack = { navController.popBackStack() },
                onStartWorkout = { workoutId ->
                    navController.navigate("active_workout/$workoutId") { launchSingleTop = true }
                },
                onViewWorkoutDetails = { workoutId ->
                    navController.navigate("workout/$workoutId") { launchSingleTop = true }
                }
            )
        }
        composable("progress") {
            ProgressRoute(
                onBack = { navController.popBackStack() },
                onOpenHistory = { navController.navigate("workout_history") },
                onOpenWorkout = { workoutId ->
                    navController.navigate("workout/$workoutId") { launchSingleTop = true }
                }
            )
        }
        composable("programs") {
            ProgramRoute(
                onBack = { navController.popBackStack() },
                onStartWorkout = { workoutId ->
                    navController.navigate("active_workout/$workoutId") { launchSingleTop = true }
                },
                onViewWorkoutDetails = { workoutId ->
                    navController.navigate("workout/$workoutId") { launchSingleTop = true }
                },
                onAddProgram = { /* handled internally */ },
                onSelectProgram = { programName ->
                    lifecycleScope.launch {
                        try {
                            val db = DatabaseProvider.get(applicationContext)
                            val profileRepo = ProfileRepository(db)
                            val currentProfile = profileRepo.getProfile()
                            if (currentProfile != null) {
                                val updatedProfile = currentProfile.copy(currentProgramId = programName)
                                profileRepo.upsertProfile(updatedProfile)
                            }
                        } catch (e: Exception) {
                            // TODO: Handle error
                        }
                    }
                    navController.popBackStack()
                }
            )
        }
        composable("photo_progress") {
            PhotoProgressRoute(onBack = { navController.popBackStack() })
        }
        composable("workout_history") {
            WorkoutHistoryRoute(
                onBack = { navController.popBackStack() },
                onSelectWorkout = { workoutId ->
                    navController.navigate("workout/$workoutId") { launchSingleTop = true }
                },
                onStartFirstWorkout = {
                    navController.popBackStack("home", inclusive = false)
                }
            )
        }
        composable("settings") {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onChangeProgram = {
                    navController.navigate("program_selection") { launchSingleTop = true }
                }
            )
        }
    }
}
