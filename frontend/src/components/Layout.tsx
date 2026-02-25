import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { Calendar, Users, ClipboardList, LayoutDashboard, LogOut } from 'lucide-react'
import { useAuthStore } from '@/store/auth'

const navItems = [
  { to: '/',         icon: LayoutDashboard, label: 'Dashboard',   exact: true },
  { to: '/schedule', icon: Calendar,        label: 'Escalas' },
  { to: '/requests', icon: ClipboardList,   label: 'Solicitacoes' },
  { to: '/users',    icon: Users,           label: 'Funcionarios', managerOnly: true },
]

export default function Layout() {
  const { user, logout, isManager } = useAuthStore()
  const navigate = useNavigate()

  return (
    <div className="flex h-screen bg-gray-50 overflow-hidden">
      <aside className="w-64 bg-white border-r border-gray-100 flex flex-col shrink-0">
        <div className="p-6 border-b border-gray-100">
          <h1 className="font-display text-xl font-bold text-primary-700">🐾 PawSchedule</h1>
          <p className="text-xs text-gray-400 mt-0.5">Gestao de Turnos</p>
        </div>

        <nav className="flex-1 p-4 space-y-0.5 overflow-y-auto">
          {navItems
            .filter((item) => !item.managerOnly || isManager())
            .map(({ to, icon: Icon, label, exact }) => (
              <NavLink
                key={to}
                to={to}
                end={exact}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-primary-50 text-primary-700'
                      : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                  }`
                }
              >
                <Icon size={17} />
                {label}
              </NavLink>
            ))}
        </nav>

        <div className="p-4 border-t border-gray-100">
          <div className="flex items-center gap-3 px-3 py-2">
            <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 font-semibold text-sm shrink-0">
              {user?.fullName?.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-gray-900 truncate">{user?.fullName}</p>
              <p className="text-xs text-gray-400 capitalize">{user?.role?.toLowerCase()}</p>
            </div>
          </div>
          <button
            onClick={() => { logout(); navigate('/login') }}
            className="flex items-center gap-2 w-full px-3 py-2 text-sm text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors mt-1"
          >
            <LogOut size={15} />
            Sair
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  )
}
