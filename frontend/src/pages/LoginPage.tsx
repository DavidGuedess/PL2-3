import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/auth'
import api from '@/services/api'
import toast from 'react-hot-toast'

export default function LoginPage() {
  const [employeeNumber, setEmployeeNumber] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const { setAuth } = useAuthStore()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      const { data } = await api.post('/api/auth/login', { employeeNumber, password })
      setAuth(data.user, data.accessToken)
      navigate('/')
    } catch {
      toast.error('Numero de funcionario ou palavra-passe incorretos')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 via-white to-gray-50 flex items-center justify-center p-4">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <div className="text-5xl mb-4">🐾</div>
          <h1 className="font-display text-3xl font-bold text-primary-800">PawSchedule</h1>
          <p className="text-gray-400 mt-1 text-sm">Sistema de Gestao de Turnos</p>
        </div>
        <div className="card">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="label">Numero de Funcionario</label>
              <input
                className="input"
                type="text"
                value={employeeNumber}
                onChange={(e) => setEmployeeNumber(e.target.value)}
                placeholder="ex: ADM001"
                required
              />
            </div>
            <div>
              <label className="label">Palavra-passe</label>
              <input
                className="input"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="btn-primary w-full mt-2" disabled={loading}>
              {loading ? 'A entrar...' : 'Entrar'}
            </button>
          </form>
        </div>
        <p className="text-center text-xs text-gray-400 mt-6">
          Clinica Veterinaria · PawSchedule v1.0
        </p>
      </div>
    </div>
  )
}
