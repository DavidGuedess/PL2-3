import { User } from '../types/user'

export const users: User[] = [
  {
    id: 1,
    employeeNumber: 'ADMIN001',
    name: 'Admin User',
    email: 'admin@miaw.com',
    passwordHash: '$2a$10$YQs7Z5qZ5Z5Z5Z5Z5Z5Z5uK5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z',
    role: 'ADMIN',
    category: 'ADMINISTRATIVE',
    active: true,
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    id: 2,
    employeeNumber: 'MAN001',
    name: 'Manager User',
    email: 'manager@miaw.com',
    passwordHash: '$2a$10$YQs7Z5qZ5Z5Z5Z5Z5Z5Z5uK5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z',
    role: 'MANAGER',
    category: 'ADMINISTRATIVE',
    active: true,
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    id: 3,
    employeeNumber: 'EMP001',
    name: 'Employee User',
    email: 'employee@miaw.com',
    passwordHash: '$2a$10$YQs7Z5qZ5Z5Z5Z5Z5Z5Z5uK5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z',
    role: 'EMPLOYEE',
    category: 'VETERINARIAN',
    active: true,
    createdAt: new Date(),
    updatedAt: new Date()
  }
]
