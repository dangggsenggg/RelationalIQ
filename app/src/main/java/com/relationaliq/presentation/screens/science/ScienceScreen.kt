package com.relationaliq.presentation.screens.science

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScienceScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The Science") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScienceCard(
                title = "Relational Frame Theory (RFT)",
                content = "Human intelligence heavily depends on the learned ability to derive relations between stimuli — same, different/opposite, more/less, before/after, contains, etc. — even when stimuli are arbitrary (nonsense words, abstract shapes). Training these skills via Multiple Exemplar Training (MET) leads to generalized improvements in cognition and academics."
            )

            ScienceCard(
                title = "SMART Training",
                content = "Strengthening Mental Abilities with Relational Training (SMART) uses progressive relational derivation training with nonsense stimuli. Training blocks with feedback are followed by test blocks without feedback. Mastery is required to advance through ~55 stages of increasing complexity."
            )

            ScienceCard(
                title = "How It Works",
                content = "You are presented with premises — relational statements between arbitrary stimuli (e.g., 'GYQ is the same as FYW'). Then you must derive whether a new relation is true or false based on the premises. As you progress, the number of premises increases, new relation types are introduced, and time pressure grows."
            )

            ScienceCard(
                title = "Research Evidence",
                content = "Multiple peer-reviewed studies support significant gains from SMART training, often 0.5–1+ standard deviations in IQ measures and scholastic performance. Key studies include Cassidy et al. (2011, 2016), Colbert et al. (2018), and McLoughlin et al. (2020+)."
            )

            ScienceCard(
                title = "Relation Types",
                content = "• Coordination (Same/Different)\n• Opposition (Opposite)\n• Comparison (More Than/Less Than)\n• Temporal (Before/After)\n• Hierarchical (Contains/Within)\n• Combined (multiple types in one trial)"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Disclaimer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Individual results may vary. This app provides evidence-based cognitive training inspired by Relational Frame Theory and is not a medical intervention. Consult appropriate professionals for clinical concerns.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ScienceCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
