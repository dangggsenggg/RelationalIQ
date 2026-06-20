package com.relationaliq.presentation.screens.training

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
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
import com.relationaliq.domain.model.BlockType
import com.relationaliq.domain.model.Trial
import com.relationaliq.presentation.theme.CorrectGreenDark
import com.relationaliq.presentation.theme.IncorrectRedDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    stageId: Int,
    onSessionComplete: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: TrainingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (state.sessionComplete) {
        onSessionComplete(state.sessionId)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.currentBlockType == BlockType.TRAINING) "Training" else "Test",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Timer
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = "Time remaining", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${state.timeRemainingSeconds}s",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (state.timeRemainingSeconds <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.error ?: "Error", color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress bar
            val progress by animateFloatAsState(
                targetValue = if (state.totalTrials > 0)
                    (state.currentTrialIndex + 1).toFloat() / state.totalTrials
                else 0f,
                animationSpec = tween(300),
                label = "progress"
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Stage ${state.stage?.id ?: stageId} • Trial ${state.currentTrialIndex + 1} / ${state.totalTrials}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Trial content
            state.currentTrial?.let { trial ->
                TrialContent(trial = trial)
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
                        onClick = { viewModel.submitAnswer(true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CorrectGreenDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("YES", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.submitAnswer(false) },
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
private fun TrialContent(trial: Trial) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Premises as cards
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

        // Probe question
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
