package com.relationaliq.presentation.screens.exam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.relationaliq.domain.model.ExamTrialState
import com.relationaliq.presentation.theme.CorrectGreenDark
import com.relationaliq.presentation.theme.IncorrectRedDark
import com.relationaliq.presentation.theme.StreakAmber
import com.relationaliq.presentation.theme.XpBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    examId: Int,
    onExamComplete: (passed: Boolean) -> Unit,
    onBack: () -> Unit,
    viewModel: ExamViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    when (state.phase) {
        ExamPhase.INTRO -> ExamIntroContent(
            state = state,
            onStart = { viewModel.startExam() },
            onBack = onBack
        )
        ExamPhase.QUESTION, ExamPhase.FEEDBACK -> ExamQuestionContent(
            state = state,
            onAnswer = { viewModel.submitAnswer(it) },
            onBack = onBack
        )
        ExamPhase.RESULTS -> ExamResultsContent(
            state = state,
            onContinue = { onExamComplete(state.examResult?.passed == true) },
            onRetry = { viewModel.startExam() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamIntroContent(
    state: ExamUiState,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exam") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = state.error, color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = state.exam?.title ?: "Exam",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = state.exam?.description ?: "",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    ExamInfoRow("Questions", "${state.totalQuestions}")
                    Spacer(modifier = Modifier.height(8.dp))
                    ExamInfoRow("Passing Score", "${((state.exam?.passingThreshold ?: 0.7f) * 100).toInt()}%")
                    Spacer(modifier = Modifier.height(8.dp))
                    ExamInfoRow("Difficulty", "Adaptive")
                    Spacer(modifier = Modifier.height(8.dp))
                    ExamInfoRow("Starting Level", "Medium")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "How it works",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The exam adapts to your performance. Answer correctly and questions get harder. " +
                            "Struggle and they become easier. It focuses on your weaker relation types.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Begin Exam", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ExamInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamQuestionContent(
    state: ExamUiState,
    onAnswer: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Exam",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Time remaining",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${state.timeRemainingSeconds}s",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (state.timeRemainingSeconds <= 5)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress
            val progress by animateFloatAsState(
                targetValue = if (state.totalQuestions > 0)
                    (state.questionsAnswered + 1).toFloat() / state.totalQuestions
                else 0f,
                animationSpec = tween(300),
                label = "examProgress"
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Q${state.questionsAnswered + 1} / ${state.totalQuestions}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                DifficultyBadge(difficulty = state.currentDifficulty)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Trial content
            state.currentTrial?.let { trialState ->
                ExamTrialContent(trialState = trialState)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Feedback overlay
            AnimatedVisibility(
                visible = state.showFeedback,
                enter = scaleIn(tween(200)) + fadeIn(),
                exit = scaleOut(tween(200)) + fadeOut()
            ) {
                FeedbackOverlay(isCorrect = state.feedbackCorrect)
            }

            // YES / NO buttons
            if (!state.showFeedback) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { onAnswer(true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CorrectGreenDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("YES", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onAnswer(false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IncorrectRedDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("NO", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: com.relationaliq.domain.model.Difficulty) {
    val color = when (difficulty) {
        com.relationaliq.domain.model.Difficulty.BEGINNER -> CorrectGreenDark
        com.relationaliq.domain.model.Difficulty.EASY -> CorrectGreenDark
        com.relationaliq.domain.model.Difficulty.MEDIUM -> XpBlue
        com.relationaliq.domain.model.Difficulty.HARD -> StreakAmber
        com.relationaliq.domain.model.Difficulty.ADVANCED -> IncorrectRedDark
        com.relationaliq.domain.model.Difficulty.EXPERT -> IncorrectRedDark
    }
    Text(
        text = difficulty.name,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun ExamTrialContent(trialState: ExamTrialState) {
    val trial = trialState.trial
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        trial.premises.forEach { premise ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = premise.toDisplayString(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = trial.probeDisplayString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeedbackOverlay(isCorrect: Boolean) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isCorrect) CorrectGreenDark.copy(alpha = 0.9f)
                else IncorrectRedDark.copy(alpha = 0.9f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
            contentDescription = if (isCorrect) "Correct" else "Incorrect",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun ExamResultsContent(
    state: ExamUiState,
    onContinue: () -> Unit,
    onRetry: () -> Unit
) {
    val result = state.examResult

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (result?.passed == true) Icons.Default.EmojiEvents else Icons.Default.Replay,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = if (result?.passed == true) CorrectGreenDark else IncorrectRedDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (result?.passed == true) "Exam Passed!" else "Not Yet...",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (result?.passed == true)
                "You've demonstrated mastery of stages ${result.stageRangeStart}-${result.stageRangeEnd}. Next stages unlocked!"
            else
                "You need 70% to pass. Review the stages and try again.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExamStatCard(
                icon = Icons.Default.Speed,
                label = "Accuracy",
                value = "${((result?.accuracy ?: 0f) * 100).toInt()}%",
                modifier = Modifier.weight(1f)
            )
            ExamStatCard(
                icon = Icons.Default.Star,
                label = "Correct",
                value = "${result?.correctAnswers ?: 0}/${result?.totalQuestions ?: 0}",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExamStatCard(
                icon = Icons.Default.TrendingUp,
                label = "XP Earned",
                value = "+${result?.xpEarned ?: 0}",
                modifier = Modifier.weight(1f),
                valueColor = XpBlue
            )
            ExamStatCard(
                icon = Icons.Default.School,
                label = "Peak Difficulty",
                value = result?.difficultyPath?.maxByOrNull { it.ordinal_level }?.name ?: "N/A",
                modifier = Modifier.weight(1f)
            )
        }

        // Relation type breakdown
        if (state.relationTypeScores.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Relation Type Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    state.relationTypeScores.forEach { (type, score) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${score.correct}/${score.total} (${(score.accuracy * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (score.accuracy >= 0.7f) CorrectGreenDark else IncorrectRedDark
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (result?.passed == true) "Continue" else "Back to Dashboard",
                fontSize = 18.sp
            )
        }

        if (result?.passed != true) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Retry Exam", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun ExamStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
