package com.mannat.medicine_reminder.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Medicines : Screen("medicines")
    data object AddMedicine : Screen("add_medicine")
    data object EditMedicine : Screen("edit_medicine/{medicineId}") {
        fun createRoute(medicineId: Long) = "edit_medicine/$medicineId"
    }
    data object History : Screen("history")
    data object Settings : Screen("settings")
}

enum class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
) {
    HOME(Screen.Home, Icons.Default.Home, "Home"),
    MEDICINES(Screen.Medicines, Icons.Default.Medication, "Medicines"),
    HISTORY(Screen.History, Icons.Default.History, "History"),
    SETTINGS(Screen.Settings, Icons.Default.Settings, "Settings")
}
