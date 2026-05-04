import { Router, Request, Response } from 'express'
import { PrismaClient, User } from '@prisma/client'
import { UserRole, UserCategory } from '../types/user'
import { hashPassword } from '../utils/password'
import { authenticate, requireRole } from '../middleware/auth'

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
 *         contact:
 *           type: string
 *           description: Número de telefone ou outro contacto
 *           nullable: true
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
const prisma = new PrismaClient()

router.use(authenticate)

type UserPublic = Omit<User, 'passwordHash'>

const toPublic = (user: User): UserPublic => {
  const { passwordHash: _omit, ...publicUser } = user
  return publicUser
}

async function getAuthenticatedUser(req: Request): Promise<User | null> {
  const userId = req.user?.userId
  if (!userId || isNaN(userId)) return null
  return prisma.user.findUnique({ where: { id: userId } })
}

/**
 * @openapi
 * /users:
 *   get:
 *     tags: [Users]
 *     summary: Listar todos os utilizadores (Admin/Gestor)
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Lista de utilizadores
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN ou MANAGER)
 */
router.get('/', requireRole('ADMIN', 'MANAGER'), async (_req: Request, res: Response) => {
  const allUsers = await prisma.user.findMany({ orderBy: { name: 'asc' } })
  return res.status(200).json(allUsers.map(toPublic))
})

/**
 * @openapi
 * /users/me:
 *   get:
 *     tags: [Users]
 *     summary: Obter o perfil do utilizador autenticado
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Perfil do utilizador autenticado
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/UserPublic'
 *       401:
 *         description: Não autenticado
 */
router.get('/me', async (req: Request, res: Response) => {
  const user = await getAuthenticatedUser(req)
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
 *     summary: Obter um utilizador por id (Admin)
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
router.get('/:id', requireRole('ADMIN'), async (req: Request, res: Response) => {
  const user = await prisma.user.findUnique({ where: { id: Number(req.params.id) } })
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
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN)
 *       409:
 *         description: Email ou número de funcionário já existe
 */
router.post('/', requireRole('ADMIN'), async (req: Request, res: Response) => {
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
  const passwordHash = await hashPassword(password)

  try {
    const newUser = await prisma.user.create({
      data: {
        name: normalizedName,
        email: normalizedEmail,
        employeeNumber: normalizedEmployeeNumber,
        passwordHash,
        role: role as UserRole,
        category: category as UserCategory,
      }
    })
    return res.status(201).json(toPublic(newUser))
  } catch (error: any) {
    if (error.code === 'P2002') {
      const targets: string[] = error.meta?.target ?? []
      if (targets.includes('email')) {
        return res.status(409).json({ message: 'Email already exists' })
      }
      return res.status(409).json({ message: 'Employee number already exists' })
    }
    throw error
  }
})

/**
 * @openapi
 * /users/me:
 *   patch:
 *     tags: [Users]
 *     summary: Atualizar o perfil do utilizador autenticado
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               name:
 *                 type: string
 *               contact:
 *                 type: string
 *                 description: Número de telefone ou outro contacto
 *               password:
 *                 type: string
 *                 format: password
 *                 minLength: 6
 *     responses:
 *       200:
 *         description: Perfil atualizado
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/UserPublic'
 *       400:
 *         description: Campos inválidos ou nenhum campo fornecido
 *       401:
 *         description: Não autenticado
 */
router.patch('/me', async (req: Request, res: Response) => {
  const user = await getAuthenticatedUser(req)
  if (!user) {
    return res.status(401).json({ message: 'Unauthorized: Authenticated user not found' })
  }

  const { name, contact, password } = req.body

  if (name === undefined && contact === undefined && password === undefined) {
    return res.status(400).json({ message: 'At least one field must be provided: name, contact or password' })
  }

  if (name !== undefined) {
    if (typeof name !== 'string' || name.trim() === '') {
      return res.status(400).json({ message: 'name must be a non-empty string' })
    }
  }

  if (contact !== undefined) {
    if (typeof contact !== 'string' || contact.trim() === '') {
      return res.status(400).json({ message: 'contact must be a non-empty string' })
    }
  }

  if (password !== undefined) {
    if (typeof password !== 'string' || password.length < 6) {
      return res.status(400).json({ message: 'password must be at least 6 characters' })
    }
  }

  const data: Record<string, unknown> = {}
  if (name !== undefined) data.name = name.trim()
  if (contact !== undefined) data.contact = contact.trim()
  if (password !== undefined) data.passwordHash = await hashPassword(password)

  const updated = await prisma.user.update({ where: { id: user.id }, data })
  return res.status(200).json(toPublic(updated))
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
 *       403:
 *         description: Sem permissão
 *       404:
 *         description: Utilizador não encontrado
 *       409:
 *         description: Utilizador já está desativado
 */
router.patch('/:id/deactivate', requireRole('ADMIN'), async (req: Request, res: Response) => {
  const userId = Number(req.params.id)
  const user = await prisma.user.findUnique({ where: { id: userId } })

  if (!user) {
    return res.status(404).json({ message: 'User not found' })
  }

  if (!user.active) {
    return res.status(409).json({ message: 'User already deactivated' })
  }

  const updated = await prisma.user.update({ where: { id: userId }, data: { active: false } })
  return res.status(200).json(toPublic(updated))
})

/**
 * @openapi
 * /users/{id}/activate:
 *   patch:
 *     tags: [Users]
 *     summary: Reativar um utilizador
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
 *         description: Utilizador reativado
 *       403:
 *         description: Sem permissão
 *       404:
 *         description: Utilizador não encontrado
 *       409:
 *         description: Utilizador já está ativo
 */
router.patch('/:id/activate', requireRole('ADMIN'), async (req: Request, res: Response) => {
  const userId = Number(req.params.id)
  const user = await prisma.user.findUnique({ where: { id: userId } })

  if (!user) {
    return res.status(404).json({ message: 'User not found' })
  }

  if (user.active) {
    return res.status(409).json({ message: 'User already active' })
  }

  const updated = await prisma.user.update({ where: { id: userId }, data: { active: true } })
  return res.status(200).json(toPublic(updated))
})

export default router
