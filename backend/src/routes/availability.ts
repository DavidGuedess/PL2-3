import { Router } from 'express'
import { authenticate } from '../middleware/auth'
import { PrismaClient } from '@prisma/client'
import { createAvailabilitySchema } from '../schemas'

const prisma = new PrismaClient()

const router = Router()

// GET /availability - ADMIN/MANAGER see all, EMPLOYEE sees own
router.get('/', authenticate, async (req, res, next) => {
  try {
    const user = (req as any).user
    const { userId, startDate, endDate } = req.query

    const dateFilter: any = {}
    if (startDate) dateFilter.gte = new Date(startDate as string)
    if (endDate) dateFilter.lte = new Date(endDate as string)

    const where: any = {}
    if (user.role === 'EMPLOYEE') {
      where.userId = user.userId
    } else if (userId) {
      where.userId = parseInt(userId as string)
    }
    if (startDate || endDate) {
      where.date = dateFilter
    }

    const availabilities = await prisma.availability.findMany({
      where,
      include: { user: { select: { id: true, name: true, employeeNumber: true } } },
      orderBy: { date: 'asc' }
    })

    res.json(availabilities)
  } catch (err) {
    next(err)
  }
})

// POST /availability - any authenticated user (creates/updates own availability)
router.post('/', authenticate, async (req, res, next) => {
  try {
    const user = (req as any).user
    const parsed = createAvailabilitySchema.safeParse(req.body)
    if (!parsed.success) {
      return res.status(400).json({ error: parsed.error.errors[0].message })
    }

    const { date, type, note } = parsed.data
    const dateObj = new Date(date)

    const availability = await prisma.availability.upsert({
      where: { userId_date: { userId: user.userId, date: dateObj } },
      update: { type, note },
      create: { userId: user.userId, date: dateObj, type, note }
    })

    res.status(201).json(availability)
  } catch (err) {
    next(err)
  }
})

// DELETE /availability/:id - own record or ADMIN/MANAGER
router.delete('/:id', authenticate, async (req, res, next) => {
  try {
    const user = (req as any).user
    const id = parseInt(req.params.id as string)

    const existing = await prisma.availability.findUnique({ where: { id } })
    if (!existing) return res.status(404).json({ error: 'Availability not found' })

    if (user.role === 'EMPLOYEE' && existing.userId !== user.userId) {
      return res.status(403).json({ error: 'Forbidden' })
    }

    await prisma.availability.delete({ where: { id } })
    res.status(204).send()
  } catch (err) {
    next(err)
  }
})

export default router
