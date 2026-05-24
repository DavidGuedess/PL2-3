import { Router, Request, Response, NextFunction } from 'express'
import { PrismaClient } from '@prisma/client'
import { authenticate, requireRole } from '../middleware/auth'
import { validate } from '../middleware/validate'
import { createTimeOffRequestSchema, updateRequestStatusSchema } from '../schemas'

const router = Router()
const prisma = new PrismaClient()

router.use(authenticate)

router.use((req, _res, next) => {
  if (req.method !== 'GET') {
    console.log(`[time-off-requests] ${req.method} ${req.path} | body: ${JSON.stringify(req.body)}`)
  }
  next()
})

// GET /time-off-requests
// EMPLOYEE: only own; ADMIN/MANAGER: all (with optional ?userId, ?from, ?to filters)
router.get('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { userId, role } = req.user!
    const isManager = role === 'ADMIN' || role === 'MANAGER'

    const where: any = isManager ? {} : { userId }

    if (isManager && req.query.userId) {
      where.userId = parseInt(req.query.userId as string)
    }

    if (req.query.from || req.query.to) {
      where.createdAt = {}
      if (req.query.from) where.createdAt.gte = new Date(req.query.from as string)
      if (req.query.to) {
        const toDate = new Date(req.query.to as string)
        toDate.setUTCHours(23, 59, 59, 999)
        where.createdAt.lte = toDate
      }
    }

    const requests = await prisma.timeOffRequest.findMany({
      where,
      include: {
        user: { select: { id: true, name: true, employeeNumber: true } }
      },
      orderBy: { createdAt: 'desc' }
    })

    res.json(requests)
  } catch (err) {
    next(err)
  }
})

// POST /time-off-requests
router.post('/', validate(createTimeOffRequestSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const userId = req.user!.userId
    const { startDate, endDate, allDay, reason } = req.body

    const request = await prisma.timeOffRequest.create({
      data: {
        userId,
        startDate: new Date(startDate),
        endDate: new Date(endDate),
        allDay: allDay ?? true,
        reason: reason || null
      },
      include: {
        user: { select: { id: true, name: true, employeeNumber: true } }
      }
    })

    res.status(201).json(request)
  } catch (err) {
    next(err)
  }
})

// PATCH /time-off-requests/:id/status  (ADMIN/MANAGER only)
router.patch('/:id/status', requireRole('ADMIN', 'MANAGER'), validate(updateRequestStatusSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id as string)
    const { status } = req.body
    const approverId = req.user!.userId

    const existing = await prisma.timeOffRequest.findUnique({ where: { id } })
    if (!existing) {
      return res.status(404).json({ message: 'Time-off request not found' })
    }

    const approver = await prisma.user.findUnique({ where: { id: approverId } })

    const updated = await prisma.timeOffRequest.update({
      where: { id },
      data: { status, approvedByName: approver?.name ?? null },
      include: {
        user: { select: { id: true, name: true, employeeNumber: true } }
      }
    })

    res.json(updated)
  } catch (err) {
    next(err)
  }
})

// DELETE /time-off-requests/:id  (own PENDING or ADMIN/MANAGER)
router.delete('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id as string)
    const { userId, role } = req.user!

    const existing = await prisma.timeOffRequest.findUnique({ where: { id } })
    if (!existing) {
      return res.status(404).json({ message: 'Time-off request not found' })
    }

    const isOwner = existing.userId === userId
    const isManager = role === 'ADMIN' || role === 'MANAGER'

    if (!isOwner && !isManager) {
      return res.status(403).json({ message: 'Forbidden' })
    }

    if (isOwner && !isManager && existing.status !== 'PENDING') {
      return res.status(409).json({ message: 'Cannot delete a request that has already been processed' })
    }

    await prisma.timeOffRequest.delete({ where: { id } })
    res.status(204).send()
  } catch (err) {
    next(err)
  }
})

export default router
