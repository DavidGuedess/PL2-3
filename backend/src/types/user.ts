export type UserRole = 'ADMIN' | 'MANAGER' | 'EMPLOYEE'

export type ProfessionalCategory = 'FILLER1' | 'FILLER2' | 'FILLER3' | 'FILLER4'

export type User = {
  id: number
  employeeNumber: string
  name: string
  email: string
  passwordHash: string
  role: UserRole
  professionalCategory: ProfessionalCategory
  contact: string
  password: string
  isActive: boolean
  createdAt: string
}
