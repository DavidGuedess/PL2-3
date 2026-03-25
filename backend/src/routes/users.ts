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

function requireAdmin(req: Request, res: Response, next: Function) {
  if (req.headers['x-user-role'] !== 'ADMIN') {
    return res.status(403).json({ message: 'Forbidden: Admins only' })
  }
  next()
}

function getAuthenticatedUser(req: Request) {
  const userId = req.user?.userId
  if (!userId || isNaN(userId)) return null
  return users.find(u => u.id === userId) || null
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
 */
router.get('/', (_req: Request, res: Response) => {
  return res.status(200).json(users.map(toPublic))
})

/**
 * @openapi
 * /users/me:
 *   get:
 *     tags: [Users]
 *     summary: Obter o utilizador autenticado
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Utilizador autenticado
 *       401:
 *         description: Não autenticado
 */
router.get('/me', (req: Request, res: Response) => {
  const user = getAuthenticatedUser(req)
  if (!user) {
    return res.status(401).json({ message: 'Unauthorized: Authenticated user not found' })
  }
  return res.status(200).json(toPublic(user))
})

/**
 * @openapi
 * /users/{id}:
 *   get:
 *     tags: [Users]
 *     summary: Obter um utilizador por id (admin)
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
 *         description: Utilizador encontrado
 *       403:
 *         description: Sem permissão
 *       404:
 *         description: Utilizador não encontrado
 */
router.get('/:id', requireAdmin, (req: Request, res: Response) => {
  const user = users.find(u => u.id === Number(req.params.id))
  if (!user) {
    return res.status(404).json({ message: 'User not found' })
  }
  return res.status(200).json(toPublic(user))
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
router.post('/', requireAdmin, async (req: Request, res: Response) => {
  const { name, email, employeeNumber, role, category, password } = req.body

  const validRoles: UserRole[] = ['ADMIN', 'MANAGER', 'EMPLOYEE']
  const validCategories: UserCategory[] = ['VETERINARIAN', 'NURSE', 'OPERATIONAL', 'ADMINISTRATIVE']
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
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }

  users.push(newUser)
  return res.status(201).json(toPublic(newUser))
})

/**
 * @openapi
 * /users/me:
 *   patch:
 *     tags: [Users]
 *     summary: Atualizar o perfil do utilizador autenticado
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Perfil atualizado
 *       400:
 *         description: Campos inválidos
 *       401:
 *         description: Não autenticado
 */
router.patch('/me', async (req: Request, res: Response) => {
  const user = getAuthenticatedUser(req)
  if (!user) {
    return res.status(401).json({ message: 'Unauthorized: Authenticated user not found' })
  }

  const { name, password } = req.body

  if (name === undefined && password === undefined) {
    return res.status(400).json({ message: 'At least one field must be provided: name or password' })
  }

  if (name !== undefined) {
    if (typeof name !== 'string' || name.trim() === '') {
      return res.status(400).json({ message: 'name must be a non-empty string' })
    }
    user.name = name.trim()
  }

  if (password !== undefined) {
    if (typeof password !== 'string' || password.length < 6) {
      return res.status(400).json({ message: 'password must be at least 6 characters' })
    }
    user.passwordHash = await hashPassword(password)
  }

  user.updatedAt = new Date().toISOString()
  return res.status(200).json(toPublic(user))
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
router.patch('/:id/deactivate', requireAdmin, (req: Request, res: Response) => {
  const user = users.find(u => u.id === Number(req.params.id))

  if (!user) {
    return res.status(404).json({ message: 'User not found' })
  }

  if (!user.active) {
    return res.status(409).json({ message: 'User already deactivated' })
  }

  user.active = false
  user.updatedAt = new Date().toISOString()
  return res.status(200).json(toPublic(user))
})

export default router
