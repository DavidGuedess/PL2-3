import { Router, Request, Response } from 'express'
import { users } from '../data/users'
import { User, UserRole, UserCategory, UserPublic } from '../types/user'

/**
 * @openapi
 * components:
 *   schemas:
 *     UserPublic:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         employeeNumber:
 *           type: string
 *         name:
 *           type: string
 *         email:
 *           type: string
 *           format: email
 *         role:
 *           type: string
 *           enum: [ADMIN, MANAGER, EMPLOYEE]
 *         category:
 *           type: string
 *           enum: [VETERINARIAN, NURSE, OPERATIONAL, ADMINISTRATIVE]
 *         active:
 *           type: boolean
 *         createdAt:
 *           type: string
 *           format: date-time
 *         updatedAt:
 *           type: string
 *           format: date-time
 *     CreateUserInput:
 *       type: object
 *       required: [name, email, employeeNumber, role, category]
 *       properties:
 *         name:
 *           type: string
 *         email:
 *           type: string
 *           format: email
 *         employeeNumber:
 *           type: string
 *         role:
 *           type: string
 *           enum: [ADMIN, MANAGER, EMPLOYEE]
 *         category:
 *           type: string
 *           enum: [VETERINARIAN, NURSE, OPERATIONAL, ADMINISTRATIVE]
 */

const router = Router()

const toPublic = (user: User): UserPublic => {
  const { passwordHash: _omit, ...publicUser } = user
  return publicUser
}

/**
 * @openapi
 * /users:
 *   get:
 *     tags: [Users]
 *     summary: Listar todos os utilizadores
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Lista de utilizadores
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 $ref: '#/components/schemas/UserPublic'
 */
router.get('/', (_req: Request, res: Response) => {
  res.json(users.map(toPublic))
})

/**
 * @openapi
 * /users:
 *   post:
 *     tags: [Users]
 *     summary: Criar um novo utilizador
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/CreateUserInput'
 *     responses:
 *       201:
 *         description: Utilizador criado
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/UserPublic'
 *       400:
 *         description: Campos obrigatórios em falta ou inválidos
 *       409:
 *         description: Email ou número de funcionário já existe
 */
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
