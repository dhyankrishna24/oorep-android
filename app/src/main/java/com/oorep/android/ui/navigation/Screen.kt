package com.oorep.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Search : Screen("search", "Search", Icons.Filled.Search, Icons.Outlined.Search)
    object Browse : Screen("browse", "Browse", Icons.Filled.MenuBook, Icons.Outlined.MenuBook)
    object Repertorize : Screen("repertorize", "Repertorize", Icons.Filled.Assessment, Icons.Outlined.Assessment)
    object MateriaMedica : Screen("mm", "Materia Medica", Icons.Filled.LocalHospital, Icons.Outlined.LocalHospital)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)

    companion object {
        val bottomNavItems = listOf(Search, Browse, Repertorize, MateriaMedica, Settings)
    }
}
