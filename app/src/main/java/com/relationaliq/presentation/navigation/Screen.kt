package com.relationaliq.presentation.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object PreAssessment : Screen("pre_assessment")
    data object PostAssessment : Screen("post_assessment")
    data object Dashboard : Screen("dashboard")
    data object StageList : Screen("stage_list")
    data object Training : Screen("training/{stageId}") {
        fun createRoute(stageId: Int) = "training/$stageId"
    }
    data object SessionSummary : Screen("session_summary/{sessionId}") {
        fun createRoute(sessionId: Long) = "session_summary/$sessionId"
    }
    data object Exam : Screen("exam/{examId}") {
        fun createRoute(examId: Int) = "exam/$examId"
    }
    data object Progress : Screen("progress")
    data object Settings : Screen("settings")
    data object Science : Screen("science")
}
