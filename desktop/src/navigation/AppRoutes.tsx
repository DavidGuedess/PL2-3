import { ReactNode, useEffect } from "react";
import { Navigate, Route, Routes, useNavigate } from "react-router-dom";

import { tokenManager } from "../storage/tokenManager";
import { listenTokenExpired } from "../state/authEvents";
import { routes } from "./routes";

import { LoginScreen } from "../screens/login/LoginScreen";
import { DashboardScreen } from "../screens/dashboard/DashboardScreen";
import { CheckInScreen } from "../screens/checkin/CheckInScreen";
import { ProfileScreen } from "../screens/profile/ProfileScreen";
import { AttendanceHistoryScreen } from "../screens/attendancehistory/AttendanceHistoryScreen";
import { AttendanceMonitorScreen } from "../screens/attendancemonitor/AttendanceMonitorScreen";
import { SchedulerScreen } from "../screens/scheduler/SchedulerScreen";
import { NotificationsScreen } from "../screens/notifications/NotificationsScreen";
import { InboxScreen } from "../screens/inbox/InboxScreen";
import { TeamScreen } from "../screens/team/TeamScreen";
import { AvailabilityScreen } from "../screens/availability/AvailabilityScreen";
import { WeekAssignmentsScreen } from "../screens/weekassignments/WeekAssignmentsScreen";

function RequireAuth({ children }: { children: ReactNode }) {
  if (!tokenManager.isLoggedIn()) {
    return <Navigate to={routes.login} replace />;
  }

  return <>{children}</>;
}

export function AppRoutes() {
  const navigate = useNavigate();

  useEffect(() => {
    return listenTokenExpired(() => {
      navigate(routes.login, { replace: true });
    });
  }, [navigate]);

  const startRoute = tokenManager.isLoggedIn()
    ? routes.dashboard
    : routes.login;

  return (
    <Routes>
      <Route path="/" element={<Navigate to={startRoute} replace />} />

      <Route
        path={routes.login}
        element={
          <LoginScreen
            onLoginSuccess={() => {
              navigate(routes.dashboard, { replace: true });
            }}
          />
        }
      />

      <Route
        path={routes.dashboard}
        element={
          <RequireAuth>
            <DashboardScreen />
          </RequireAuth>
        }
      />

      <Route
        path={routes.checkIn}
        element={
          <RequireAuth>
            <CheckInScreen />
          </RequireAuth>
        }
      />

      <Route
        path={routes.profile}
        element={
          <RequireAuth>
            <ProfileScreen />
          </RequireAuth>
        }
      />

      <Route
        path={routes.attendanceHistory}
        element={
          <RequireAuth>
            <AttendanceHistoryScreen />
          </RequireAuth>
        }
      />

      <Route
        path={routes.attendanceMonitor}
        element={
          <RequireAuth>
            <AttendanceMonitorScreen />
          </RequireAuth>
        }
      />

      <Route
        path={routes.scheduler}
        element={
          <RequireAuth>
            <SchedulerScreen />
          </RequireAuth>
        }
      />

      <Route
        path={routes.notifications}
        element={
          <RequireAuth>
            <NotificationsScreen />
          </RequireAuth>
        }
      />

      <Route
        path={routes.inbox}
        element={
          <RequireAuth>
            <InboxScreen />
          </RequireAuth>
        }
      />

      <Route
        path={routes.team}
        element={
          <RequireAuth>
            <TeamScreen />
          </RequireAuth>
        }
      />

      <Route
        path={routes.availability}
        element={
          <RequireAuth>
            <AvailabilityScreen />
          </RequireAuth>
        }
      />

      <Route
        path={routes.weekAssignments}
        element={
          <RequireAuth>
            <WeekAssignmentsScreen />
          </RequireAuth>
        }
      />
    </Routes>
  );
}
