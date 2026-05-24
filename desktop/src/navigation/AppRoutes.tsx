import { useEffect } from "react";
import { Navigate, Route, Routes, useNavigate } from "react-router-dom";

import { tokenManager } from "../storage/tokenManager";
import { listenTokenExpired } from "../state/authEvents";
import { routes } from "./routes";

import { AppLayout } from "../components/AppLayout";
import { LoginScreen } from "../screens/login/LoginScreen";
import { DashboardScreen } from "../screens/dashboard/DashboardScreen";
import { SchedulerScreen } from "../screens/scheduler/SchedulerScreen";
import { InboxScreen } from "../screens/inbox/InboxScreen";
import { TeamScreen } from "../screens/team/TeamScreen";
import { HistoryScreen } from "../screens/history/HistoryScreen";
import { ReportsScreen } from "../screens/reports/ReportsScreen";
import { NotificationsScreen } from "../screens/notifications/NotificationsScreen";
import { ProfileScreen } from "../screens/profile/ProfileScreen";
import { AvailabilityScreen } from "../screens/availability/AvailabilityScreen";
import { CheckInScreen } from "../screens/checkin/CheckInScreen";

function RequireAuth() {
  if (!tokenManager.isLoggedIn()) {
    return <Navigate to={routes.login} replace />;
  }
  return <AppLayout />;
}

export function AppRoutes() {
  const navigate = useNavigate();

  useEffect(() => {
    return listenTokenExpired(() => {
      navigate(routes.login, { replace: true });
    });
  }, [navigate]);

  const startRoute = tokenManager.isLoggedIn() ? routes.dashboard : routes.login;

  return (
    <Routes>
      <Route path="/" element={<Navigate to={startRoute} replace />} />

      <Route
        path={routes.login}
        element={
          <LoginScreen
            onLoginSuccess={() => navigate(routes.dashboard, { replace: true })}
          />
        }
      />

      <Route element={<RequireAuth />}>
        <Route path={routes.dashboard}     element={<DashboardScreen />} />
        <Route path={routes.scheduler}     element={<SchedulerScreen />} />
        <Route path={routes.inbox}         element={<InboxScreen />} />
        <Route path={routes.team}          element={<TeamScreen />} />
        <Route path={routes.history}       element={<HistoryScreen />} />
        <Route path={routes.reports}       element={<ReportsScreen />} />
        <Route path={routes.notifications} element={<NotificationsScreen />} />
        <Route path={routes.profile}       element={<ProfileScreen />} />
        <Route path={routes.availability}  element={<AvailabilityScreen />} />
        <Route path={routes.checkIn}       element={<CheckInScreen />} />
      </Route>
    </Routes>
  );
}
