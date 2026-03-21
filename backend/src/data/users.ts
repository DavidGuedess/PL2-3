import { User } from '../types/user'

export const users: User[] = [
  {
    id: 1,
    employeeNumber: 'ADMIN001',
    name: 'Admin User',
    email: 'admin@miaw.com',
    passwordHash: '$2a$10$YQs7Z5qZ5Z5Z5Z5Z5Z5Z5uK5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z',
    role: 'ADMIN',
    professionalCategory: 'FILLER1',
    contact: '123456789',
    password: 'Password',
    isActive: true,
    createdAt: new Date().toISOString()
  },
  {
    id: 2,
    employeeNumber: 'MAN001',
    name: 'Manager User',
    email: 'manager@miaw.com',
    passwordHash: '$2a$10$YQs7Z5qZ5Z5Z5Z5Z5Z5Z5uK5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z',
    role: 'MANAGER',
    professionalCategory: 'FILLER2',
    contact: '123456789',
    password: 'Password',
    isActive: true,
    createdAt: new Date().toISOString()
  },
  {
    id: 3,
    employeeNumber: 'EMP001',
    name: 'Employee User',
    email: 'employee@miaw.com',
    passwordHash: '$2a$10$YQs7Z5qZ5Z5Z5Z5Z5Z5Z5uK5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z',
    role: 'EMPLOYEE',
    professionalCategory: 'FILLER3',
    contact: '123456789',
    password: 'Password',
    isActive: true,
    createdAt: new Date().toISOString()
  }
]
