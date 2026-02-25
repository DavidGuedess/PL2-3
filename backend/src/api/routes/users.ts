import { Router, Response } from 'express'
import bcrypt from 'bcryptjs'
import { prisma } from '../../models/prisma'
import { authenticate, requireAdmin, requireManager, AuthRequest } from '../../middleware/auth'

const router = Router()
router.use(authenticate)

router.get('/', requireManager, async (_req, res: Response): Promise<void> => {
  const users = await prisma.user.findMany({
    where: { isActive: true },
    select: {
      id: true, employeeNumber: true, fullName: true, email: true,
      role: true, department: true, isActive: true, createdAt: true,
    },
    orderBy: { fullName: 'asc' },
  })
  res.json(users)
})

router.post('/', requireAdmin, async (req, res: Response): Promise<void> => {
  const { employeeNumber, fullName, email, password, role, department } = req.body

  if (!employeeNumber || !fullName || !email || !password) {
    res.status(400).json({ error: 'employeeNumber, fullName, email and password are required' })
    return
  }

  const exists = await prisma.user.findFirst({
    where: { OR: [{ employeeNumber }, { email }] },
  })
  if (exists) {
    res.status(409).json({ error: 'Employee number or email already in use' })
    return
  }

  const passwordHash = await bcrypt.hash(password, 10)
  const user = await prisma.user.create({
    data: { employeeNumber, fullName, email, passwordHash, role, department },
    select: {
      id: true, employeeNumber: true, fullName: true, email: true,
      role: true, department: true, isActive: true, createdAt: true,
    },
  })
  res.status(201).json(user)
})

router.get('/:id', async (req: AuthRequest, res: Response): Promise<void> => {
  const id = Number(req.params.id)
  const isOwn = req.user!.userId === id
  const isManager = ['ADMIN', 'MANAGER'].includes(req.user!.role)

  if (!isOwn && !isManager) {
    res.status(403).json({ error: 'Forbidden' })
    return
  }

  const user = await prisma.user.findUnique({
    where: { id },
    select: {
      id: true, employeeNumber: true, fullName: true, email: true,
      role: true, department: true, isActive: true, createdAt: true,
    },
  })
  if (!user) { res.status(404).json({ error: 'User not found' }); return }
  res.json(user)
})

router.patch('/:id', requireAdmin, async (req, res: Response): Promise<void> => {
  const id = Number(req.params.id)
  const { fullName, email, role, department, isActive } = req.body

  const user = await prisma.user.findUnique({ where: { id } })
  if (!user) { res.status(404).json({ error: 'User not found' }); return }

  const updated = await prisma.user.update({
    where: { id },
    data: { fullName, email, role, department, isActive },
    select: {
      id: true, employeeNumber: true, fullName: true, email: true,
      role: true, department: true, isActive: true, updatedAt: true,
    },
  })
  res.json(updated)
})

router.delete('/:id', requireAdmin, async (req, res: Response): Promise<void> => {
  const id = Number(req.params.id)
  const user = await prisma.user.findUnique({ where: { id } })
  if (!user) { res.status(404).json({ error: 'User not found' }); return }
  await prisma.user.update({ where: { id }, data: { isActive: false } })
  res.status(204).send()
})

export default router
