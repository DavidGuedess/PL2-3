package pt.ualg.miaugenda.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import pt.ualg.miaugenda.MiauGendaApp
import pt.ualg.miaugenda.ui.screen.dashboard.DashboardScreen
import pt.ualg.miaugenda.ui.screen.login.LoginScreen
import pt.ualg.miaugenda.ui.screen.checkin.CheckInScreen
import pt.ualg.miaugenda.ui.screen.profile.ProfileScreen
import pt.ualg.miaugenda.ui.screen.attendancehistory.AttendanceHistoryScreen
import pt.ualg.miaugenda.ui.screen.attendancemonitor.AttendanceMonitorScreen
import pt.ualg.miaugenda.ui.screen.users.UsersScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object CheckIn : Screen("checkin")
    object Profile : Screen("profile")
    object AttendanceHistory : Screen("attendance_history")
    object AttendanceMonitor : Screen("attendance_monitor")
    object Users : Screen("users")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onCheckInClick = {
                    navController.navigate(Screen.CheckIn.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                onAttendanceHistoryClick = {
                    navController.navigate(Screen.AttendanceHistory.route)
                },
                onAttendanceMonitorClick = {
                    navController.navigate(Screen.AttendanceMonitor.route)
                },
                onUsersClick = {
                    navController.navigate(Screen.Users.route)
                }
            )
        }

        composable(Screen.CheckIn.route) {
            CheckInScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AttendanceHistory.route) {
            AttendanceHistoryScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AttendanceMonitor.route) {
            AttendanceMonitorScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Users.route) {
            UsersScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun getStartDestination(): String {
    val context = LocalContext.current
    val tokenManager = MiauGendaApp.getTokenManager(context)
    
    return if (tokenManager.isLoggedIn()) {
        Screen.Dashboard.route
    } else {
        Screen.Login.route
    }
}
