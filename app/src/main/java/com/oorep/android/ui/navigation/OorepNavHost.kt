package com.oorep.android.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.oorep.android.OorepApplication
import com.oorep.android.data.model.CaseRubric
import com.oorep.android.data.model.PatientCase
import com.oorep.android.ui.screens.*
import kotlinx.coroutines.launch

@Composable
fun OorepNavHost() {
    val navController = rememberNavController()
    val app = OorepApplication.instance
    val dbHelper = app.databaseHelper
    val engine = app.repertorizationEngine
    val scope = rememberCoroutineScope()

    var activeCase by remember { mutableStateOf<PatientCase?>(null) }
    var activeCaseRubrics by remember { mutableStateOf<List<CaseRubric>>(emptyList()) }
    var mmSearchQuery by remember { mutableStateOf("") }
    val isDbReady by dbHelper.isReady.collectAsState()

    // Load or initialize default patient case
    LaunchedEffect(isDbReady) {
        if (isDbReady) {
            val caze = dbHelper.getOrCreateDefaultCase()
            activeCase = caze
            activeCaseRubrics = dbHelper.getCaseRubrics(caze.id)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Search.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Search.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Search.route) {
                SearchScreen(
                    dbHelper = dbHelper,
                    activeCaseRubrics = activeCaseRubrics,
                    onAddRubricToCase = { rubric ->
                        activeCase?.let { caze ->
                            scope.launch {
                                dbHelper.addRubricToCase(caze.id, rubric)
                                activeCaseRubrics = dbHelper.getCaseRubrics(caze.id)
                            }
                        }
                    },
                    onNavigateToMM = { remedyName ->
                        mmSearchQuery = remedyName
                        navController.navigate(Screen.MateriaMedica.route)
                    }
                )
            }

            composable(Screen.Browse.route) {
                BrowseScreen(
                    dbHelper = dbHelper,
                    activeCaseRubrics = activeCaseRubrics,
                    onAddRubricToCase = { rubric ->
                        activeCase?.let { caze ->
                            scope.launch {
                                dbHelper.addRubricToCase(caze.id, rubric)
                                activeCaseRubrics = dbHelper.getCaseRubrics(caze.id)
                            }
                        }
                    }
                )
            }

            composable(Screen.Repertorize.route) {
                RepertorizeScreen(
                    dbHelper = dbHelper,
                    engine = engine,
                    caseRubrics = activeCaseRubrics,
                    onDeleteRubric = { id ->
                        scope.launch {
                            dbHelper.deleteCaseRubric(id)
                            activeCase?.let { caze ->
                                activeCaseRubrics = dbHelper.getCaseRubrics(caze.id)
                            }
                        }
                    },
                    onUpdateRubric = { id, weight, isEliminating ->
                        scope.launch {
                            dbHelper.updateCaseRubric(id, weight, isEliminating)
                            activeCase?.let { caze ->
                                activeCaseRubrics = dbHelper.getCaseRubrics(caze.id)
                            }
                        }
                    },
                    onClearAll = {
                        scope.launch {
                            activeCase?.let { caze ->
                                dbHelper.clearCaseRubrics(caze.id)
                                activeCaseRubrics = emptyList()
                            }
                        }
                    },
                    onNavigateToMM = { remedyName ->
                        mmSearchQuery = remedyName
                        navController.navigate(Screen.MateriaMedica.route)
                    }
                )
            }

            composable(Screen.MateriaMedica.route) {
                MMScreen(
                    dbHelper = dbHelper,
                    initialQuery = mmSearchQuery
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
