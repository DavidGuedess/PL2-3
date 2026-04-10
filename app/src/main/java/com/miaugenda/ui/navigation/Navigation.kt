package com.miaugenda.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class Screen(val route: String, val label: String) {
    object Login : Screen("login", "Login")
    object Dashboard : Screen("dashboard", "Dashboard")
    object Shifts : Screen("shifts", "Turnos")
    object Profile : Screen("profile", "Perfil")
    object Attendance : Screen("attendance", "Ponto")
}

@Composable
fun AppNavHost(navController: NavHostController, isLoggedIn: Boolean) {
    val startDestination = if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            // LoginScreen vai aqui — implementado pelo Diogo
        }
        composable(Screen.Dashboard.route) {
            // DashboardScreen vai aqui — implementado pelo Xavier
        }
        composable(Screen.Shifts.route) {
            // ShiftsScreen vai aqui — sprint seguinte
        }
        composable(Screen.Profile.route) {
            // ProfileScreen vai aqui — sprint seguinte
        }
        composable(Screen.Attendance.route) {
            // AttendanceScreen vai aqui — implementado pelo Leo
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(Screen.Dashboard, Screen.Shifts, Screen.Attendance, Screen.Profile)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = when (screen) {
                            Screen.Dashboard -> Icons.Filled.Home
                            Screen.Shifts -> Icons.Filled.DateRange
                            Screen.Profile -> Icons.Filled.Person
                            Screen.Attendance -> Icons.Filled.DateRange
                            else -> Icons.Filled.Home
                        },
                        contentDescription = screen.label
                    )
                },
                label = { Text(screen.label) }
            )
        }
    }
}
