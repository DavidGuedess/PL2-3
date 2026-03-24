import { Router, Request, Response } from 'express'
import { users } from '../data/users'
import { User, UserRole, UserCategory, UserPublic } from '../types/user'
import { hashPassword } from '../utils/password'
import { authenticate } from '../middleware/auth'

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
 *         passwordHash:
 *           type: string
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
 *       required: [name, email, employeeNumber, role, category, password]
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
 *         password:
 *           type: string
 *           format: password
 */

const router = Router()

router.use(authenticate)

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
router.post('/', async (req: Request, res: Response) => {
  const { name, email, employeeNumber, role, category, password } = req.body

  const validRoles: UserRole[] = ['ADMIN', 'MANAGER', 'EMPLOYEE']
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  if (typeof name !== 'string' || name.trim() === '') {
    return res.status(400).json({ message: 'name is required and must be a non-empty string' })
  }

  if (typeof email !== 'string' || !emailRegex.test(email.trim().toLowerCase())) {
    return res.status(400).json({ message: 'Invalid email format' })
  }

  if (typeof employeeNumber !== 'string' || employeeNumber.trim() === '') {
    return res.status(400).json({ message: 'employeeNumber is required and must be a non-empty string' })
  }

  if (typeof role !== 'string' || !validRoles.includes(role as UserRole)) {
    return res.status(400).json({ message: 'Invalid role' })
  }

  const validCategories: UserCategory[] = ['VETERINARIAN', 'NURSE', 'OPERATIONAL', 'ADMINISTRATIVE']

  if (typeof category !== 'string' || !validCategories.includes(category as UserCategory)) {
    return res.status(400).json({ message: 'Invalid category' })
  }

  if (typeof password !== 'string' || password.length < 6) {
    return res.status(400).json({ message: 'password is required and must be at least 6 characters' })
  }

  const normalizedName = name.trim()
  const normalizedEmail = email.trim().toLowerCase()
  const normalizedEmployeeNumber = employeeNumber.trim()

  if (users.find(u => u.email.toLowerCase() === normalizedEmail)) {
    return res.status(409).json({ message: 'Email already exists' })
  }

  if (users.find(u => u.employeeNumber === normalizedEmployeeNumber)) {
    return res.status(409).json({ message: 'Employee number already exists' })
  }

  const newId = users.length > 0 ? Math.max(...users.map(u => u.id)) + 1 : 1
  const passwordHash = await hashPassword(password)

  const newUser: User = {
    id: newId,
    name: normalizedName,
    email: normalizedEmail,
    employeeNumber: normalizedEmployeeNumber,
    passwordHash,
    role: role as UserRole,
    category: category as UserCategory,
    active: true,
    createdAt: new Date(),
    updatedAt: new Date()
  }

  users.push(newUser)

  return res.status(201).json(toPublic(newUser))
})

/**
 * @openapi
 * /users/{id}/deactivate:
 *   patch:
 *     tags: [Users]
 *     summary: Desativar um utilizador
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     responses:
 *       200:
 *         description: Utilizador desativado
 *       404:
 *         description: Utilizador não encontrado
 *       409:
 *         description: Utilizador já está desativado
 */
router.patch('/:id/deactivate', (req: Request, res: Response) => {
  const { id } = req.params

  const user = users.find(u => u.id === Number(id))

  if (!user) {
    return res.status(404).json({ message: 'User not found' })
  }

  if (!user.active) {
    return res.status(409).json({ message: 'User already deactivated' })
  }

  user.active = false

  return res.status(200).json(toPublic(user))
})

export default router
