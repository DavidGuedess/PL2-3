import { Router, Request, Response, NextFunction } from 'express'
import { PrismaClient } from '@prisma/client'
import { authenticate, requireRole } from '../middleware/auth'

const router = Router()
const prisma = new PrismaClient()

router.use(authenticate)

function getWeekStart(dateStr: string): Date {
  const base = new Date(dateStr)
  const day  = base.getUTCDay()
  const diff = day === 0 ? -6 : 1 - day
  const start = new Date(base)
  start.setUTCDate(base.getUTCDate() + diff)
  start.setUTCHours(0, 0, 0, 0)
  return start
}

// GET /week-assignments?week=YYYY-MM-DD
router.get('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { week } = req.query
    if (!week || typeof week !== 'string') {
      return res.status(400).json({ message: 'Query param week is required (YYYY-MM-DD)' })
    }
    const weekStart = getWeekStart(week)
    const assignments = await prisma.weekAssignment.findMany({
      where: { weekStart },
      include: {
        user: { select: { id: true, name: true, employeeNumber: true, role: true, category: true } }
      }
    })
    res.json(assignments)
  } catch (err) {
    next(err)
  }
})

// POST /week-assignments  { userId, weekStart: "YYYY-MM-DD" }
router.post('/', requireRole('ADMIN', 'MANAGER'), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { userId, weekStart: weekStartStr } = req.body
    if (!userId || !weekStartStr) {
      return res.status(400).json({ message: 'userId and weekStart are required' })
    }
    const user = await prisma.user.findUnique({ where: { id: Number(userId) } })
    if (!user) return res.status(404).json({ message: 'User not found' })

    const weekStart = getWeekStart(weekStartStr)
    const assignment = await prisma.weekAssignment.upsert({
      where: { userId_weekStart: { userId: Number(userId), weekStart } },
      update: {},
      create: { userId: Number(userId), weekStart },
      include: {
        user: { select: { id: true, name: true, employeeNumber: true, role: true, category: true } }
      }
    })
    res.status(201).json(assignment)
  } catch (err) {
    next(err)
  }
})

// DELETE /week-assignments/:id  (também apaga os turnos do utilizador nessa semana)
router.delete('/:id', requireRole('ADMIN', 'MANAGER'), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id)
    const assignment = await prisma.weekAssignment.findUnique({ where: { id } })
    if (!assignment) return res.status(404).json({ message: 'Assignment not found' })

    const weekEnd = new Date(assignment.weekStart)
    weekEnd.setUTCDate(weekEnd.getUTCDate() + 6)
    weekEnd.setUTCHours(23, 59, 59, 999)

    await prisma.shift.deleteMany({
      where: { userId: assignment.userId, date: { gte: assignment.weekStart, lte: weekEnd } }
    })
    await prisma.weekAssignment.delete({ where: { id } })
    res.status(204).send()
  } catch (err) {
    next(err)
  }
})

export default router
