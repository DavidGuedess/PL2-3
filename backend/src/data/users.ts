import { User } from '../types/user'

import { v4 as uuidv4 } from 'uuid'


export const users: User[] = [
  {
    id: uuidv4(),
    fullName: 'Admin User',
    email: 'admin@miaw.com',
    employeeNumber: 'ADMIN001',
    role: 'ADMIN',
    isActive: true,
    createdAt: new Date().toISOString()
  },
  {
    id: uuidv4(),
    fullName: 'Manager User',
    email: 'manager@miaw.com',
    employeeNumber: 'MAN001',
    role: 'MANAGER',
    isActive: true,
    createdAt: new Date().toISOString()
  },
  {
    id: uuidv4(),
    fullName: 'Employee User',
    email: 'employee@miaw.com',
    employeeNumber: 'EMP001',
    role: 'EMPLOYEE',
    isActive: true,
    createdAt: new Date().toISOString()
  }
]