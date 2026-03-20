export type UserRole = 'ADMIN' | 'MANAGER' | 'EMPLOYEE'

export type UserCategory = 'VETERINARIAN' | 'NURSE' | 'OPERATIONAL' | 'ADMINISTRATIVE'

export type User = {
  id: number
  employeeNumber: string
  name: string
  email: string
  passwordHash: string
  role: UserRole
  category: UserCategory
  active: boolean
  createdAt: Date
  updatedAt: Date
}

export type UserPublic = Omit<User, 'passwordHash'>