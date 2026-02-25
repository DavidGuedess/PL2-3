import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { User } from '@/types'

interface AuthState {
  user: User | null
  token: string | null
  setAuth: (user: User, token: string) => void
  logout: () => void
  isAdmin: () => boolean
  isManager: () => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      setAuth: (user, token) => set({ user, token }),
      logout: () => set({ user: null, token: null }),
      isAdmin: () => get().user?.role === 'ADMIN',
      isManager: () => ['ADMIN', 'MANAGER'].includes(get().user?.role ?? ''),
    }),
    { name: 'pawschedule-auth' }
  )
)
