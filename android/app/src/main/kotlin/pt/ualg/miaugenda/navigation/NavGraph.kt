package pt.ualg.miaugenda.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import pt.ualg.miaugenda.AppState
import pt.ualg.miaugenda.MiauGendaApp
import pt.ualg.miaugenda.ui.screen.dashboard.DashboardScreen
import pt.ualg.miaugenda.ui.screen.login.LoginScreen
import pt.ualg.miaugenda.ui.screen.checkin.CheckInScreen
import pt.ualg.miaugenda.ui.screen.profile.ProfileScreen
import pt.ualg.miaugenda.ui.screen.attendancehistory.AttendanceHistoryScreen
import pt.ualg.miaugenda.ui.screen.attendancemonitor.AttendanceMonitorScreen
import pt.ualg.miaugenda.ui.screen.notifications.NotificationsScreen
import pt.ualg.miaugenda.ui.screen.scheduler.SchedulerScreen
import pt.ualg.miaugenda.ui.screen.inbox.InboxScreen
import pt.ualg.miaugenda.ui.screen.team.TeamScreen
import pt.ualg.miaugenda.ui.screen.dashboard.ScreenDisponibilidadePreferencia

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object CheckIn : Screen("checkin")
    object Profile : Screen("profile")
    object AttendanceHistory : Screen("attendance_history")
    object AttendanceMonitor : Screen("attendance_monitor")
    object Scheduler : Screen("scheduler")
    object Notifications : Screen("notifications")
    object Inbox : Screen("inbox")
    object Team : Screen("team")
    object Availability : Screen("availability")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    LaunchedEffect(Unit) {
        AppState.tokenExpired.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

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
                onSchedulerClick = {
                    navController.navigate(Screen.Scheduler.route)
                },
                onNotificationsClick = {
                    navController.navigate(Screen.Notifications.route)
                },
                onInboxClick = {
                    navController.navigate(Screen.Inbox.route)
                },
                onEquipaClick = {
                    navController.navigate(Screen.Team.route)
                },
                onAvailabilityClick = {
                    navController.navigate(Screen.Availability.route)
                }
            )
        }

        composable(Screen.Inbox.route) {
            InboxScreen(
                onSchedulerClick = {
                    navController.navigate(Screen.Scheduler.route) {
                        popUpTo(Screen.Scheduler.route) { inclusive = true }
                    }
                },
                onNotificationsClick = {
                    navController.navigate(Screen.Notifications.route) {
                        popUpTo(Screen.Notifications.route) { inclusive = true }
                    }
                },
                onHomeClick = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onEquipaClick = {
                    navController.navigate(Screen.Team.route) {
                        popUpTo(Screen.Team.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Scheduler.route) {
            SchedulerScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onNavigateToInbox = {
                    navController.navigate(Screen.Inbox.route) {
                        popUpTo(Screen.Inbox.route) { inclusive = true }
                    }
                },
                onNavigateToEquipa = {
                    navController.navigate(Screen.Team.route) {
                        popUpTo(Screen.Team.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Notifications.route) {
            val context = LocalContext.current
            val tokenManager = MiauGendaApp.getTokenManager(context)
            NotificationsScreen(
                currentUserId = tokenManager.getUserId(),
                currentRole = tokenManager.getUserRole() ?: "EMPLOYEE",
                currentUserName = tokenManager.getUserName() ?: "",
                onNavigateToScheduler = {
                    navController.navigate(Screen.Scheduler.route) {
                        popUpTo(Screen.Scheduler.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToInbox = {
                    navController.navigate(Screen.Inbox.route) {
                        popUpTo(Screen.Inbox.route) { inclusive = true }
                    }
                },
                onNavigateToEquipa = {
                    navController.navigate(Screen.Team.route) {
                        popUpTo(Screen.Team.route) { inclusive = true }
                    }
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
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onAvailabilityClick = {
                    navController.navigate(Screen.Availability.route)
                }
            )
        }

        composable(Screen.Availability.route) {
            ScreenDisponibilidadePreferencia(
                onBack = { navController.popBackStack() }
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

        composable(Screen.Team.route) {
            TeamScreen(
                onNavigateToHome          = { navController.navigate(Screen.Dashboard.route) { launchSingleTop = true } },
                onNavigateToScheduler     = { navController.navigate(Screen.Scheduler.route) { launchSingleTop = true } },
                onNavigateToInbox         = { navController.navigate(Screen.Inbox.route) { launchSingleTop = true } },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) { launchSingleTop = true } }
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
