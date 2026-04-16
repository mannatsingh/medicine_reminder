package com.mannat.medicine_reminder.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mannat.medicine_reminder.ui.screen.addeditmedicine.AddEditMedicineScreen
import com.mannat.medicine_reminder.ui.screen.history.HistoryScreen
import com.mannat.medicine_reminder.ui.screen.home.HomeScreen
import com.mannat.medicine_reminder.ui.screen.medicines.MedicinesScreen
import com.mannat.medicine_reminder.ui.screen.settings.SettingsScreen

@Composable
fun MedicineReminderNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomBarRoutes = BottomNavItem.entries.map { it.screen.route }
    val showBottomBar = currentDestination?.route in bottomBarRoutes

    val showFab = currentDestination?.route in listOf(
        Screen.Home.route,
        Screen.Medicines.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavItem.entries.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddMedicine.route) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add medicine")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onEditMedicine = { medicineId ->
                        navController.navigate(Screen.EditMedicine.createRoute(medicineId))
                    }
                )
            }

            composable(Screen.Medicines.route) {
                MedicinesScreen(
                    onEditMedicine = { medicineId ->
                        navController.navigate(Screen.EditMedicine.createRoute(medicineId))
                    },
                    onAddMedicine = {
                        navController.navigate(Screen.AddMedicine.route)
                    }
                )
            }

            composable(Screen.AddMedicine.route) {
                AddEditMedicineScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditMedicine.route,
                arguments = listOf(
                    navArgument("medicineId") { type = NavType.LongType }
                )
            ) {
                AddEditMedicineScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
