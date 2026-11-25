package com.oorep.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oorep.android.data.model.RemedyGrade
import com.oorep.android.data.model.Rubric

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RubricCard(
    rubric: Rubric,
    isAddedToCase: Boolean,
    onAddToCase: () -> Unit,
    onLoadRemedies: suspend () -> List<RemedyGrade>,
    onRemedyClick: (RemedyGrade) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var remedies by remember { mutableStateOf<List<RemedyGrade>>(emptyList()) }
    var isLoadingRemedies by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded && remedies.isEmpty()) {
            isLoadingRemedies = true
            try {
                remedies = onLoadRemedies()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingRemedies = false
            }
        }
    }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Repertory Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = rubric.abbrev.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Rubric Path
                    Text(
                        text = rubric.fullPath,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }

                // Add to Case Button
                IconButton(
                    onClick = onAddToCase,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isAddedToCase) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isAddedToCase) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = if (isAddedToCase) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "Add to case"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expand / Collapse Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (expanded && remedies.isNotEmpty()) "${remedies.size} remedies" else "Tap to view remedies",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded Remedies Flow
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoadingRemedies) {
                        Text(
                            text = "Loading remedies...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (remedies.isEmpty()) {
                        Text(
                            text = "No remedies found for this rubric.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            remedies.forEach { remedy ->
                                RemedyChip(
                                    remedy = remedy,
                                    onClick = { onRemedyClick(remedy) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
