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

  const validRoles: UserRole[] = ['ADMIN', 'MANAGER', 'EMPLOYEE']
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  if (typeof fullName !== 'string' || fullName.trim() === '') {
    return res.status(400).json({
      message: 'fullName is required and must be a non-empty string'
    })
  }

  if (typeof email !== 'string' || !emailRegex.test(email.trim().toLowerCase())) {
    return res.status(400).json({
      message: 'Invalid email format'
    })
  }

  if (typeof employeeNumber !== 'string' || employeeNumber.trim() === '') {
    return res.status(400).json({
      message: 'employeeNumber is required and must be a non-empty string'
    })
  }

  if (typeof role !== 'string' || !validRoles.includes(role as UserRole)) {
    return res.status(400).json({
      message: 'Invalid role'
    })
  }

  const normalizedFullName = fullName.trim()
  const normalizedEmail = email.trim().toLowerCase()
  const normalizedEmployeeNumber = employeeNumber.trim()

  const emailAlreadyExists = users.find(function (user) {
    return user.email.toLowerCase() === normalizedEmail
  })

  if (emailAlreadyExists) {
    return res.status(409).json({
      message: 'Email already exists'
    })
  }

  const employeeNumberAlreadyExists = users.find(function (user) {
    return user.employeeNumber === normalizedEmployeeNumber
  })

  if (users.find(u => u.employeeNumber === employeeNumber)) {
    return res.status(409).json({ message: 'Employee number already exists' })
  }

  const newUser: User = {
    id: uuidv4(),
    fullName: normalizedFullName,
    email: normalizedEmail,
    employeeNumber: normalizedEmployeeNumber,
    role: role as UserRole,
    category: category as UserCategory,
    active: true,
    createdAt: new Date(),
    updatedAt: new Date()
  }

  users.push(newUser)

  return res.status(201).json(newUser)
})

router.patch('/:id/deactivate', (req: Request, res: Response) => {
  const { id } = req.params

  const user = users.find(function (user) {
      
      if (!id) {
        return res.status(400).json({
          message: 'User id is required'
        })
      }  

      return user.id === id
  })

  if (!user) {
    return res.status(404).json({
      message: 'User not found'
    })
  }

  if (!user.isActive) {
    return res.status(409).json({
      message: 'User already deactivated'
    })
  }

  user.isActive = false

  return res.status(200).json(user)

})

export default router
