import { useLocation, useNavigate, Outlet } from "react-router-dom";
import { Home, Calendar, MessageSquare, Users, BarChart2, Bell, LogOut, PawPrint, PieChart } from "lucide-react";
import { routes } from "../navigation/routes";
import { tokenManager } from "../storage/tokenManager";

const NAV_ITEMS = [
  { path: routes.dashboard,      Icon: Home,          label: "Início" },
  { path: routes.scheduler,      Icon: Calendar,      label: "Agenda" },
  { path: routes.inbox,          Icon: MessageSquare, label: "Caixa" },
  { path: routes.team,           Icon: Users,         label: "Equipa" },
  { path: routes.history,        Icon: BarChart2,     label: "Histórico" },
  { path: routes.reports,        Icon: PieChart,      label: "Relatórios" },
  { path: routes.notifications,  Icon: Bell,          label: "Notificações" },
];

export function AppLayout() {
  const location  = useLocation();
  const navigate  = useNavigate();
  const name      = tokenManager.getUserName() ?? "Utilizador";
  const firstName = name.split(" ")[0] ?? "Utilizador";
  const initial   = firstName.charAt(0).toUpperCase();

  function handleLogout() {
    tokenManager.clearTokens();
    navigate(routes.login, { replace: true });
  }

  const isFullScreen = [routes.scheduler, routes.inbox, routes.history, routes.reports].includes(location.pathname);

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="sidebar-brand-icon"><PawPrint size={16} color="white" /></div>
          MiauGenda
        </div>

        <nav className="sidebar-nav">
          {NAV_ITEMS.map(item => (
            <button
              key={item.path}
              className={`sidebar-item${location.pathname === item.path ? " active" : ""}`}
              onClick={() => navigate(item.path)}
            >
              <item.Icon size={17} className="sidebar-item-icon" />
              {item.label}
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <button
            className={`sidebar-item${location.pathname === routes.profile ? " active" : ""}`}
            onClick={() => navigate(routes.profile)}
          >
            <div className="sidebar-avatar">{initial}</div>
            <span style={{ flex: 1, textAlign: "left", fontSize: 13, color: "var(--text)" }}>{firstName}</span>
          </button>
          <button className="sidebar-item" onClick={handleLogout}>
            <LogOut size={17} className="sidebar-item-icon" />
            Terminar Sessão
          </button>
        </div>
      </aside>

      <div className="main-content">
        {isFullScreen ? (
          <div className="full-screen">
            <Outlet />
          </div>
        ) : (
          <div className="screen-body">
            <Outlet />
          </div>
        )}
      </div>
    </div>
  );
}
