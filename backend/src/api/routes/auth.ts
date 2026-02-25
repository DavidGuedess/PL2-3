import { Router, Request, Response } from 'express'
import bcrypt from 'bcryptjs'
import { prisma } from '../../models/prisma'
import { signToken } from '../../services/jwt'
import { authenticate, AuthRequest } from '../../middleware/auth'

const router = Router()

router.post('/login', async (req: Request, res: Response): Promise<void> => {
  const { employeeNumber, password } = req.body

  if (!employeeNumber || !password) {
    res.status(400).json({ error: 'employeeNumber and password are required' })
    return
  }

  const user = await prisma.user.findUnique({ where: { employeeNumber } })

  if (!user || !user.isActive) {
    res.status(401).json({ error: 'Invalid credentials' })
    return
  }

  const valid = await bcrypt.compare(password, user.passwordHash)
  if (!valid) {
    res.status(401).json({ error: 'Invalid credentials' })
    return
  }

  const token = signToken({ userId: user.id, role: user.role })
  const { passwordHash: _, ...safeUser } = user
  res.json({ accessToken: token, user: safeUser })
})

router.get('/me', authenticate, async (req: AuthRequest, res: Response): Promise<void> => {
  const user = await prisma.user.findUnique({
    where: { id: req.user!.userId },
    select: {
      id: true, employeeNumber: true, fullName: true, email: true,
      role: true, department: true, isActive: true, createdAt: true,
    },
  })
  if (!user) { res.status(404).json({ error: 'User not found' }); return }
  res.json(user)
})

export default router
