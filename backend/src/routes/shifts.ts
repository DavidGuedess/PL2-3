import { Router, Request, Response } from 'express'
import { PrismaClient } from '@prisma/client'

const router = Router()
const prisma = new PrismaClient()

function getWeekRange(dateStr: string): { start: Date; end: Date } {
  const base = new Date(dateStr)
  const day = base.getUTCDay()
  const diff = day === 0 ? -6 : 1 - day
  const start = new Date(base)
  start.setUTCDate(base.getUTCDate() + diff)
  start.setUTCHours(0, 0, 0, 0)
  const end = new Date(start)
  end.setUTCDate(start.getUTCDate() + 6)
  end.setUTCHours(23, 59, 59, 999)
  return { start, end }
}

function getMonthRange(monthStr: string): { start: Date; end: Date } {
  const [year, month] = monthStr.split('-').map(Number)
  const start = new Date(Date.UTC(year, month - 1, 1))
  const end = new Date(Date.UTC(year, month, 0, 23, 59, 59, 999))
  return { start, end }
}

router.post('/', async (req: Request, res: Response) => {
  const { userId, shiftTypeId, date } = req.body

  if (!userId || !shiftTypeId || !date) {
    return res.status(400).json({ message: 'userId, shiftTypeId and date are required' })
  }

  const user = await prisma.user.findUnique({ where: { id: parseInt(userId) } })
  if (!user) {
    return res.status(404).json({ message: 'User not found' })
  }

  const shiftType = await prisma.shiftType.findUnique({ where: { id: parseInt(shiftTypeId) } })
  if (!shiftType) {
    return res.status(404).json({ message: 'Shift type not found' })
  }

  const shiftDate = new Date(date)

  const existing = await prisma.shift.findUnique({
    where: { userId_date: { userId: parseInt(userId), date: shiftDate } }
  })
  if (existing) {
    return res.status(409).json({ message: 'User already has a shift on this date' })
  }

  const shift = await prisma.shift.create({
    data: {
      userId: parseInt(userId),
      shiftTypeId: parseInt(shiftTypeId),
      date: shiftDate
    },
    include: {
      user: { select: { id: true, name: true, employeeNumber: true } },
      shiftType: true
    }
  })

  res.status(201).json(shift)
})

router.get('/', async (req: Request, res: Response) => {
  const { week, month } = req.query

  if (!week && !month) {
    return res.status(400).json({
      message: 'Query param week or month is required (e.g. ?week=2026-03-23 or ?month=2026-03)'
    })
  }

  const { start, end } = week
    ? getWeekRange(week as string)
    : getMonthRange(month as string)

  const shifts = await prisma.shift.findMany({
    where: { date: { gte: start, lte: end } },
    include: {
      user: { select: { id: true, name: true, employeeNumber: true, role: true } },
      shiftType: true
    },
    orderBy: [{ date: 'asc' }, { user: { name: 'asc' } }]
  })

  res.json(shifts)
})

export default router
