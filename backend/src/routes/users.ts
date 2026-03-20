import { Router, Request, Response } from 'express'
import { v4 as uuidv4 } from 'uuid'
import { users } from '../data/users'
import { User, UserRole } from '../types/user'

const router = Router()

router.get('/', (_req: Request, res: Response) => {
  res.json(users)
})

router.post('/', (req: Request, res: Response) => {
  const { fullName, email, employeeNumber, role } = req.body

  if (!fullName || !email || !employeeNumber || !role) {
    return res.status(400).json({
      message: 'fullName, email, employeeNumber and role are required'
    })
  }

  const validRoles: UserRole[] = ['ADMIN', 'MANAGER', 'EMPLOYEE']

  if (!validRoles.includes(role)) {
    return res.status(400).json({
      message: 'Invalid role'
    })
  }

  const emailAlreadyExists = users.find(function (user) {
    return user.email === email
  })

  if (emailAlreadyExists) {
    return res.status(409).json({
      message: 'Email already exists'
    })
  }

  const employeeNumberAlreadyExists = users.find(function (user) {
    return user.employeeNumber === employeeNumber
  })

  if (employeeNumberAlreadyExists) {
    return res.status(409).json({
      message: 'Employee number already exists'
    })
  }

  const newUser: User = {
    id: uuidv4(),
    fullName: fullName,
    email: email,
    employeeNumber: employeeNumber,
    role: role as UserRole,
    isActive: true,
    createdAt: new Date().toISOString()
  }

  users.push(newUser)

  res.status(201).json(newUser)
})

export default router