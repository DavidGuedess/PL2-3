import { Router, Request, Response, NextFunction } from 'express'
import { PrismaClient, User } from '@prisma/client'
import { UserRole, UserCategory } from '../types/user'
import { hashPassword } from '../utils/password'
import { authenticate, requireRole } from '../middleware/auth'
import { validate } from '../middleware/validate'
import { createUserSchema, updateUserMeSchema } from '../schemas'

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
 *     parameters:
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *           minimum: 1
 *         description: Número da página
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           minimum: 1
 *         description: Número de resultados por página
 *       - in: query
 *         name: name
 *         schema:
 *           type: string
 *         description: Filtrar por nome (parcial, insensível a maiúsculas)
 *       - in: query
 *         name: role
 *         schema:
 *           type: string
 *           enum: [ADMIN, MANAGER, EMPLOYEE]
 *         description: Filtrar por papel
 *       - in: query
 *         name: category
 *         schema:
 *           type: string
 *           enum: [VETERINARIAN, NURSE, OPERATIONAL, ADMINISTRATIVE]
 *         description: Filtrar por categoria
 *     responses:
 *       200:
 *         description: Lista de utilizadores (com paginação se page e limit fornecidos)
 *       400:
 *         description: Parâmetros inválidos
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN ou MANAGER)
 */
router.get('/', requireRole('ADMIN', 'MANAGER'), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { page, limit, name, role, category } = req.query

    const validRoles: UserRole[] = ['ADMIN', 'MANAGER', 'EMPLOYEE']
    const validCategories: UserCategory[] = ['VETERINARIAN', 'NURSE', 'OPERATIONAL', 'ADMINISTRATIVE']

    if (role !== undefined && !validRoles.includes(role as UserRole)) {
      return res.status(400).json({ message: 'Invalid role filter' })
    }
    if (category !== undefined && !validCategories.includes(category as UserCategory)) {
      return res.status(400).json({ message: 'Invalid category filter' })
    }

    const pageNum = page !== undefined ? parseInt(page as string, 10) : undefined
    const limitNum = limit !== undefined ? parseInt(limit as string, 10) : undefined

    if (pageNum !== undefined && (isNaN(pageNum) || pageNum < 1)) {
      return res.status(400).json({ message: 'page must be a positive integer' })
    }
    if (limitNum !== undefined && (isNaN(limitNum) || limitNum < 1)) {
      return res.status(400).json({ message: 'limit must be a positive integer' })
    }

    const where: any = {}
    if (name) where.name = { contains: name as string, mode: 'insensitive' }
    if (role) where.role = role as string
    if (category) where.category = category as string

    if (pageNum !== undefined && limitNum !== undefined) {
      const skip = (pageNum - 1) * limitNum
      const [total, users] = await Promise.all([
        prisma.user.count({ where }),
        prisma.user.findMany({ where, orderBy: { name: 'asc' }, skip, take: limitNum })
      ])
      return res.status(200).json({
        data: users.map(toPublic),
        total,
        page: pageNum,
        limit: limitNum,
        totalPages: Math.ceil(total / limitNum)
      })
    }

    const allUsers = await prisma.user.findMany({ where, orderBy: { name: 'asc' } })
    return res.status(200).json(allUsers.map(toPublic))
  } catch (err) {
    next(err)
  }
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
router.get('/me', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const user = await getAuthenticatedUser(req)
    if (!user) {
      return res.status(401).json({ message: 'Unauthorized: Authenticated user not found' })
    }
    return res.status(200).json(toPublic(user))
  } catch (err) {
    next(err)
  }
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
router.get('/:id', requireRole('ADMIN'), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const user = await prisma.user.findUnique({ where: { id: Number(req.params.id) } })
    if (!user) {
      return res.status(404).json({ message: 'User not found' })
    }
    return res.status(200).json(toPublic(user))
  } catch (err) {
    next(err)
  }
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
router.post('/', requireRole('ADMIN'), validate(createUserSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { name, email, employeeNumber, role, category, password } = req.body

    const passwordHash = await hashPassword(password)

    const newUser = await prisma.user.create({
      data: {
        name,
        email,
        employeeNumber,
        passwordHash,
        role: role as UserRole,
        category: category as UserCategory,
      }
    })
    return res.status(201).json(toPublic(newUser))
  } catch (err) {
    next(err)
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
router.patch('/me', validate(updateUserMeSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const user = await getAuthenticatedUser(req)
    if (!user) {
      return res.status(401).json({ message: 'Unauthorized: Authenticated user not found' })
    }

    const { name, contact, password } = req.body

    const data: Record<string, unknown> = {}
    if (name !== undefined) data.name = name
    if (contact !== undefined) data.contact = contact
    if (password !== undefined) data.passwordHash = await hashPassword(password)

    const updated = await prisma.user.update({ where: { id: user.id }, data })
    return res.status(200).json(toPublic(updated))
  } catch (err) {
    next(err)
  }
})

/**
 * @openapi
 * /users/{id}:
 *   patch:
 *     tags: [Users]
 *     summary: Editar informações de um utilizador (Admin)
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
 *         description: Utilizador atualizado
 *       403:
 *         description: Sem permissão
 *       404:
 *         description: Utilizador não encontrado
 */
router.patch('/:id', requireRole('ADMIN'), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const userId = Number(req.params.id)
    const user = await prisma.user.findUnique({ where: { id: userId } })
    if (!user) return res.status(404).json({ message: 'User not found' })
    const { name, email, contact, role, category } = req.body
    const data: Record<string, unknown> = {}
    if (name !== undefined) data.name = name
    if (email !== undefined) data.email = email
    if (contact !== undefined) data.contact = contact
    if (role !== undefined) data.role = role
    if (category !== undefined) data.category = category
    const updated = await prisma.user.update({ where: { id: userId }, data })
    return res.status(200).json(toPublic(updated))
  } catch (err) {
    next(err)
  }
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
router.patch('/:id/deactivate', requireRole('ADMIN'), async (req: Request, res: Response, next: NextFunction) => {
  try {
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
  } catch (err) {
    next(err)
  }
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
router.patch('/:id/activate', requireRole('ADMIN'), async (req: Request, res: Response, next: NextFunction) => {
  try {
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
  } catch (err) {
    next(err)
  }
})

export default router
