package com.relationaliq.presentation.screens.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.relationaliq.presentation.theme.CorrectGreenDark
import com.relationaliq.presentation.theme.IncorrectRedDark
import com.relationaliq.presentation.theme.XpBlue

@Composable
fun SessionSummaryScreen(
    onContinue: () -> Unit,
    onRetry: (Int) -> Unit,
    viewModel: SessionSummaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val session = state.session

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Result icon
        Icon(
            imageVector = if (session?.passed == true) Icons.Default.EmojiEvents else Icons.Default.Replay,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = if (session?.passed == true) CorrectGreenDark else IncorrectRedDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (session?.passed == true) "Stage Complete!" else "Keep Practicing!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (session?.passed == true)
                "Great job! You've mastered this stage."
            else
                "You need ${((session?.accuracy ?: 0f) * 100).toInt()}% but didn't reach the mastery threshold. Try again!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Stats cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Default.CheckCircle,
                label = "Accuracy",
                value = "${((session?.accuracy ?: 0f) * 100).toInt()}%",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.Timer,
                label = "Avg Time",
                value = "${(session?.averageResponseTimeMs ?: 0) / 1000}s",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Default.Star,
                label = "Correct",
                value = "${session?.correctAnswers ?: 0}/${session?.totalTrials ?: 0}",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.Speed,
                label = "XP Earned",
                value = "+${session?.xpEarned ?: 0}",
                modifier = Modifier.weight(1f),
                valueColor = XpBlue
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Continue", fontSize = 18.sp)
        }

        if (session?.passed != true) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { onRetry(session?.stageId ?: 1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Retry Stage", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun StatCard(
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
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = valueColor)
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}
