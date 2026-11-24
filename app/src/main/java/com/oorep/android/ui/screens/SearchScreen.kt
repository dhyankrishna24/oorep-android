package com.oorep.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import com.oorep.android.data.model.RemedyGrade
import com.oorep.android.data.model.Rubric
import com.oorep.android.ui.components.RubricCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    dbHelper: DatabaseHelper,
    activeCaseRubrics: List<CaseRubric>,
    onAddRubricToCase: (Rubric) -> Unit,
    onNavigateToMM: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("headache morning") }
    var selectedRepertory by remember { mutableStateOf<String?>("publicum") }
    var rubrics by remember { mutableStateOf<List<Rubric>>(emptyList()) }
    var remediesMap by remember { mutableStateOf<Map<Int, List<RemedyGrade>>>(emptyMap()) }
    var isSearching by remember { mutableStateOf(false) }

    var selectedRemedyForModal by remember { mutableStateOf<RemedyGrade?>(null) }
    val scope = rememberCoroutineScope()

    // Trigger search
    fun performSearch() {
        if (searchQuery.isBlank()) {
            rubrics = emptyList()
            return
        }
        isSearching = true
        scope.launch {
            val results = dbHelper.searchRubrics(searchQuery, selectedRepertory, null, limit = 50)
            rubrics = results
            isSearching = false

            // Preload remedies for visible results
            val newMap = mutableMapOf<Int, List<RemedyGrade>>()
            for (r in results.take(15)) {
                newMap[r.id] = dbHelper.getRemediesForRubric(r.abbrev, r.id)
            }
            remediesMap = newMap
        }
    }

    LaunchedEffect(selectedRepertory) {
        performSearch()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        performSearch()
                    },
                    placeholder = { Text("Search symptoms (e.g. headache morning)") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                rubrics = emptyList()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Repertory Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(
                        selected = selectedRepertory == null,
                        onClick = { selectedRepertory = null },
                        label = { Text("All Repertories") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = selectedRepertory == "publicum",
                        onClick = { selectedRepertory = "publicum" },
                        label = { Text("Repertorium Publicum") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = selectedRepertory == "kent-de",
                        onClick = { selectedRepertory = "kent-de" },
                        label = { Text("Kent (German)") },
                        modifier = Modifier.padding(end = 8.dp)
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
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (rubrics.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Text(
                        text = "🌿",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Instant Repertory Search",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Type any symptom terms to search 146,000+ rubrics across open homeopathic repertories.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(rubrics, key = { "${it.abbrev}_${it.id}" }) { rubric ->
                        // Load remedies on demand if not yet cached
                        val remedies = remediesMap[rubric.id] ?: run {
                            LaunchedEffect(rubric.id) {
                                val loaded = dbHelper.getRemediesForRubric(rubric.abbrev, rubric.id)
                                remediesMap = remediesMap + (rubric.id to loaded)
                            }
                            emptyList()
                        }

                        val isAdded = activeCaseRubrics.any { it.abbrev == rubric.abbrev && it.rubricId == rubric.id }

                        RubricCard(
                            rubric = rubric,
                            remedies = remedies,
                            isAddedToCase = isAdded,
                            onAddToCase = { onAddRubricToCase(rubric) },
                            onRemedyClick = { selectedRemedyForModal = it }
                        )
                    }
                }
            }

            // Remedy Quick Action Bottom Sheet
            selectedRemedyForModal?.let { remedy ->
                ModalBottomSheet(
                    onDismissRequest = { selectedRemedyForModal = null }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = remedy.nameAbbrev,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        remedy.nameLong?.let { longName ->
                            Text(
                                text = longName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Repertory Grade: ${remedy.weight}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val name = remedy.nameAbbrev.replace(".", "")
                                selectedRemedyForModal = null
                                onNavigateToMM(name)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View in Materia Medica")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
