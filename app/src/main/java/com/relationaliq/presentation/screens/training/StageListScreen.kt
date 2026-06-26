package com.relationaliq.presentation.screens.training

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.relationaliq.domain.model.Stage
import com.relationaliq.presentation.theme.CorrectGreenDark

private val MODULE_DISPLAY_NAMES = mapOf(
    "M1_Coordination" to "M1: Coordination (Same/Different)",
    "M2_Comparison" to "M2: Comparison (More/Less)",
    "M3_Opposition" to "M3: Opposition (Opposite)",
    "M4_Temporal" to "M4: Temporal (Before/After)",
    "M5_Containment" to "M5: Containment (Contains/Within)",
    "M6_Mixed" to "M6: Mixed & Advanced"
)

private val MODULE_ORDER = listOf(
    "M1_Coordination", "M2_Comparison", "M3_Opposition",
    "M4_Temporal", "M5_Containment", "M6_Mixed"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StageListScreen(
    onStageSelected: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: StageListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val expandedModules = remember { mutableStateMapOf<String, Boolean>() }

    val groupedStages = state.stages.groupBy { it.module.ifEmpty { "Other" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training Stages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val orderedModules = MODULE_ORDER.filter { groupedStages.containsKey(it) } +
                groupedStages.keys.filter { it !in MODULE_ORDER }

            for (module in orderedModules) {
                val stages = groupedStages[module] ?: continue
                val isExpanded = expandedModules[module] ?: true
                val completedCount = stages.count { state.completedStageIds.contains(it.id) }

                item(key = "header_$module") {
                    ModuleHeader(
                        moduleName = MODULE_DISPLAY_NAMES[module] ?: module,
                        completedCount = completedCount,
                        totalCount = stages.size,
                        isExpanded = isExpanded,
                        onClick = { expandedModules[module] = !isExpanded }
                    )
                }

                if (isExpanded) {
                    items(stages, key = { it.id }) { stage ->
                        val isUnlocked = state.unlockedStageIds.contains(stage.id)
                        val isCompleted = state.completedStageIds.contains(stage.id)
                        StageCard(
                            stage = stage,
                            isUnlocked = isUnlocked,
                            isCompleted = isCompleted,
                            onClick = { if (isUnlocked) onStageSelected(stage.id) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ModuleHeader(
    moduleName: String,
    completedCount: Int,
    totalCount: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = moduleName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$completedCount / $totalCount stages completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CorrectGreenDark,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun StageCard(
    stage: Stage,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = isUnlocked,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> CorrectGreenDark.copy(alpha = 0.15f)
                isUnlocked -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    isCompleted -> Icons.Default.CheckCircle
                    isUnlocked -> Icons.Default.PlayArrow
                    else -> Icons.Default.Lock
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = when {
                    isCompleted -> CorrectGreenDark
                    isUnlocked -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stage ${stage.id}: ${stage.title}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                val metaInfo = buildString {
                    append(stage.difficulty.label)
                    if (stage.frameType.isNotEmpty()) {
                        append(" \u2022 ")
                        append(stage.frameType)
                    }
                    if (stage.derivationDepth > 1) {
                        append(" \u2022 Depth ${stage.derivationDepth}")
                    }
                }
                Text(
                    text = metaInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
