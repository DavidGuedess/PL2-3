import { User } from '../types/user'

import { v4 as uuidv4 } from 'uuid'


export const users: User[] = [
  {
    id: uuidv4(),
    fullName: 'Admin User',
    email: 'admin@miaw.com',
    employeeNumber: 'ADMIN001',
    role: 'ADMIN',
    professionalCategory: 'FILLER1',
    contact: '123456789',
    password: 'Password',
    isActive: true,
    createdAt: new Date().toISOString()
  },
  {
    id: uuidv4(),
    fullName: 'Manager User',
    email: 'manager@miaw.com',
    employeeNumber: 'MAN001',
    role: 'MANAGER',
    professionalCategory: 'FILLER2',
    contact: '123456789',
    password: 'Password',
    isActive: true,
    createdAt: new Date().toISOString()
  },
  {
    id: uuidv4(),
    fullName: 'Employee User',
    email: 'employee@miaw.com',
    employeeNumber: 'EMP001',
    role: 'EMPLOYEE',
    professionalCategory: 'FILLER3',
    contact: '123456789',
    password: 'Password',
    isActive: true,
    createdAt: new Date().toISOString()
  }
]