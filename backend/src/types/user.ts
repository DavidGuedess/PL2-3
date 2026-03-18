export type UserRole = 'ADMIN' | 'MANAGER' | 'EMPLOYEE'

export type User = {
  id: string
  fullName: string
  email: string
  employeeNumber: string
  role: UserRole
  isActive: boolean
  createdAt: string
}