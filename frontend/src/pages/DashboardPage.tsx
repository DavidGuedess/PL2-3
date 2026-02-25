import { useAuthStore } from '@/store/auth'
import { format } from 'date-fns'
import { pt } from 'date-fns/locale'

export default function DashboardPage() {
  const { user } = useAuthStore()

  const greeting = () => {
    const h = new Date().getHours()
    if (h < 12) return 'Bom dia'
    if (h < 19) return 'Boa tarde'
    return 'Boa noite'
  }

  return (
    <div className="p-8">
      <div className="mb-8">
        <h2 className="font-display text-2xl font-bold text-gray-900">
          {greeting()}, {user?.fullName?.split(' ')[0]} 👋
        </h2>
        <p className="text-gray-400 text-sm mt-1 capitalize">
          {format(new Date(), "EEEE, d 'de' MMMM 'de' yyyy", { locale: pt })}
        </p>
      </div>
      <div className="card">
        <p className="text-gray-500 text-sm">Bem-vindo ao PawSchedule. Usa o menu lateral para navegar.</p>
      </div>
    </div>
  )
}
