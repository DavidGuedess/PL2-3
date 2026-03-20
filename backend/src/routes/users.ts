import { Router, Request, Response } from 'express'
import { users } from '../data/users'
import { User, UserRole, UserCategory, UserPublic } from '../types/user'

const router = Router()

const toPublic = (user: User): UserPublic => {
  const { passwordHash: _omit, ...publicUser } = user
  return publicUser
}

router.get('/', (_req: Request, res: Response) => {
  res.json(users.map(toPublic))
})

router.post('/', (req: Request, res: Response) => {
  const { name, email, employeeNumber, role, category } = req.body

  if (!name || !email || !employeeNumber || !role || !category) {
    return res.status(400).json({
      message: 'name, email, employeeNumber, role and category are required'
    })
  }

  const validRoles: UserRole[] = ['ADMIN', 'MANAGER', 'EMPLOYEE']
  if (!validRoles.includes(role)) {
    return res.status(400).json({ message: 'Invalid role' })
  }

  const validCategories: UserCategory[] = ['VETERINARIAN', 'NURSE', 'OPERATIONAL', 'ADMINISTRATIVE']
  if (!validCategories.includes(category)) {
    return res.status(400).json({ message: 'Invalid category' })
  }

  if (users.find(u => u.email === email)) {
    return res.status(409).json({ message: 'Email already exists' })
  }

  if (users.find(u => u.employeeNumber === employeeNumber)) {
    return res.status(409).json({ message: 'Employee number already exists' })
  }

  const newUser: User = {
    id: users.length + 1,
    employeeNumber,
    name,
    email,
    passwordHash: '',
    role: role as UserRole,
    category: category as UserCategory,
    active: true,
    createdAt: new Date(),
    updatedAt: new Date()
  }

  users.push(newUser)
  res.status(201).json(toPublic(newUser))
})

export default router
