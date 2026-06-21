package com.relationaliq.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.relationaliq.presentation.screens.assessment.AssessmentScreen
import com.relationaliq.presentation.screens.dashboard.DashboardScreen
import com.relationaliq.presentation.screens.onboarding.OnboardingScreen
import com.relationaliq.presentation.screens.science.ScienceScreen
import com.relationaliq.presentation.screens.settings.SettingsScreen
import com.relationaliq.presentation.screens.training.StageListScreen
import com.relationaliq.presentation.screens.training.SessionSummaryScreen
import com.relationaliq.presentation.screens.training.TrainingScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.PreAssessment.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PreAssessment.route) {
            AssessmentScreen(
                isPreAssessment = true,
                onComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.PreAssessment.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PostAssessment.route) {
            AssessmentScreen(
                isPreAssessment = false,
                onComplete = { navController.popBackStack() }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToTraining = { stageId ->
                    navController.navigate(Screen.Training.createRoute(stageId))
                },
                onNavigateToStages = {
                    navController.navigate(Screen.StageList.route)
                },
                onNavigateToProgress = {
                    navController.navigate(Screen.Progress.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToScience = {
                    navController.navigate(Screen.Science.route)
                }
            )
        }

        composable(Screen.StageList.route) {
            StageListScreen(
                onStageSelected = { stageId ->
                    navController.navigate(Screen.Training.createRoute(stageId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Training.route,
            arguments = listOf(navArgument("stageId") { type = NavType.IntType })
        ) { backStackEntry ->
            val stageId = backStackEntry.arguments?.getInt("stageId") ?: 1
            TrainingScreen(
                stageId = stageId,
                onSessionComplete = { sessionId ->
                    navController.navigate(Screen.SessionSummary.createRoute(sessionId)) {
                        popUpTo(Screen.Training.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SessionSummary.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) {
            SessionSummaryScreen(
                onContinue = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onRetry = { stageId ->
                    navController.navigate(Screen.Training.createRoute(stageId)) {
                        popUpTo(Screen.Dashboard.route)
                    }
                }
            )
        }

        composable(Screen.Progress.route) {
            StageListScreen(
                onStageSelected = { stageId ->
                    navController.navigate(Screen.Training.createRoute(stageId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Science.route) {
            ScienceScreen(onBack = { navController.popBackStack() })
        }
    }
}
