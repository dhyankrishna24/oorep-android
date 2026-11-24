package com.oorep.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oorep.android.data.db.DatabaseHelper
import com.oorep.android.data.model.CaseRubric
import com.oorep.android.data.model.Chapter
import com.oorep.android.data.model.Rubric
import com.oorep.android.ui.components.RubricCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    dbHelper: DatabaseHelper,
    activeCaseRubrics: List<CaseRubric>,
    onAddRubricToCase: (Rubric) -> Unit
) {
    var chapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var selectedChapter by remember { mutableStateOf<Chapter?>(null) }
    var chapterRubrics by remember { mutableStateOf<List<Rubric>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        chapters = dbHelper.getChapters("publicum")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedChapter?.name ?: "Browse Chapters",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (selectedChapter != null) {
                        IconButton(onClick = { selectedChapter = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to chapters")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedChapter == null) {
                // Chapter List
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(chapters) { chapter ->
                        ElevatedCard(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedChapter = chapter
                                    isLoading = true
                                    scope.launch {
                                        chapterRubrics = dbHelper.searchRubrics("", chapter.abbrev, chapter.id, limit = 100)
                                        isLoading = false
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = chapter.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                // Rubrics inside selected chapter
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(chapterRubrics) { rubric ->
                            val isAdded = activeCaseRubrics.any { it.abbrev == rubric.abbrev && it.rubricId == rubric.id }
                            RubricCard(
                                rubric = rubric,
                                remedies = emptyList(),
                                isAddedToCase = isAdded,
                                onAddToCase = { onAddRubricToCase(rubric) }
                            )
                        }
                    }
                }
            }
        }
    }
}
