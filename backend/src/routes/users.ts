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
