package com.oorep.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oorep.android.data.db.DatabaseHelper
import com.oorep.android.data.model.CaseRubric
import com.oorep.android.data.model.RepertorizationScore
import com.oorep.android.domain.RepertorizationEngine
import com.oorep.android.ui.theme.Grade3Color
import com.oorep.android.ui.theme.Grade4Color
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepertorizeScreen(
    dbHelper: DatabaseHelper,
    engine: RepertorizationEngine,
    caseRubrics: List<CaseRubric>,
    onDeleteRubric: (Long) -> Unit,
    onUpdateRubric: (Long, Int, Boolean) -> Unit,
    onClearAll: () -> Unit,
    onNavigateToMM: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var scores by remember { mutableStateOf<List<RepertorizationScore>>(emptyList()) }
    var isCalculating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(caseRubrics) {
        if (caseRubrics.isNotEmpty()) {
            isCalculating = true
            scores = engine.repertorize(caseRubrics)
            isCalculating = false
        } else {
            scores = emptyList()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Repertorization",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "${caseRubrics.size} active rubrics",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        if (caseRubrics.isNotEmpty()) {
                            TextButton(onClick = onClearAll) {
                                Text("Clear", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Rubrics (${caseRubrics.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Remedy Matrix (${scores.size})") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (caseRubrics.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Text(text = "📋", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Rubrics in Case",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Search or browse rubrics and tap '+' to add them to your case for repertorization.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else if (selectedTab == 0) {
                // Rubrics Tab
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(caseRubrics, key = { it.id }) { item ->
                        ElevatedCard(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onDeleteRubric(item.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Weight Selector
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Weight: ",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        listOf(1, 2, 3, 4).forEach { w ->
                                            FilterChip(
                                                selected = item.weight == w,
                                                onClick = { onUpdateRubric(item.id, w, item.isEliminating) },
                                                label = { Text("${w}x") },
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                        }
                                    }

                                    // Eliminating toggle
                                    FilterChip(
                                        selected = item.isEliminating,
                                        onClick = { onUpdateRubric(item.id, item.weight, !item.isEliminating) },
                                        label = { Text("Key") },
                                        leadingIcon = if (item.isEliminating) {
                                            { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Remedy Matrix Tab
                if (isCalculating) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(scores) { score ->
                            val coverageFraction = score.coveredRubricsCount.toFloat() / caseRubrics.size.toFloat()

                            ElevatedCard(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onNavigateToMM(score.nameAbbrev.replace(".", ""))
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text(
                                                text = score.nameAbbrev,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            score.nameLong?.let { longName ->
                                                Text(
                                                    text = longName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.padding(end = 8.dp)
                                            ) {
                                                Text(
                                                    text = "${score.coveredRubricsCount}/${caseRubrics.size}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.tertiaryContainer
                                            ) {
                                                Text(
                                                    text = "Score: ${score.totalScore}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Coverage Progress Bar
                                    LinearProgressIndicator(
                                        progress = { coverageFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (coverageFraction >= 0.8f) Grade4Color else Grade3Color
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
