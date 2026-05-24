import { Router, Request, Response, NextFunction } from 'express'
import { PrismaClient } from '@prisma/client'
import { authenticate, requireRole } from '../middleware/auth'
import { validate } from '../middleware/validate'
import { createShiftSwapRequestSchema, updateRequestStatusSchema } from '../schemas'

const router = Router()
const prisma = new PrismaClient()

router.use(authenticate)

router.use((req, _res, next) => {
  if (req.method !== 'GET') {
    console.log(`[shift-swap-requests] ${req.method} ${req.path} | body: ${JSON.stringify(req.body)}`)
  }
  next()
})

const shiftInclude = {
  user: { select: { id: true, name: true, employeeNumber: true, role: true, category: true } },
  shiftType: true
}

// GET /shift-swap-requests
// EMPLOYEE: own (as requester) + requests targeting their shifts
// ADMIN/MANAGER: all (with optional ?userId, ?from, ?to filters)
router.get('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { userId, role } = req.user!
    const isManager = role === 'ADMIN' || role === 'MANAGER'

    let where: any = isManager
      ? {}
      : { OR: [{ requesterId: userId }, { targetShift: { userId } }] }

    if (isManager && req.query.userId) {
      const filterUserId = parseInt(req.query.userId as string)
      where = { OR: [{ requesterId: filterUserId }, { targetShift: { userId: filterUserId } }] }
    }

    if (req.query.from || req.query.to) {
      const dateFilter: any = {}
      if (req.query.from) dateFilter.gte = new Date(req.query.from as string)
      if (req.query.to) {
        const toDate = new Date(req.query.to as string)
        toDate.setUTCHours(23, 59, 59, 999)
        dateFilter.lte = toDate
      }
      where.createdAt = dateFilter
    }

    const requests = await prisma.shiftSwapRequest.findMany({
      where,
      include: {
        requester: { select: { id: true, name: true, employeeNumber: true } },
        requesterShift: { include: shiftInclude },
        targetShift:    { include: shiftInclude }
      },
      orderBy: { createdAt: 'desc' }
    })

    res.json(requests)
  } catch (err) {
    next(err)
  }
})

// POST /shift-swap-requests
router.post('/', validate(createShiftSwapRequestSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const requesterId = req.user!.userId
    const { requesterShiftId, targetShiftId, reason } = req.body

    const requesterShift = await prisma.shift.findUnique({ where: { id: requesterShiftId } })
    if (!requesterShift) return res.status(404).json({ message: 'Requester shift not found' })
    if (requesterShift.userId !== requesterId) return res.status(403).json({ message: 'You can only swap your own shifts' })
    if (!requesterShift.published) return res.status(400).json({ message: 'Your shift must be published to be swapped' })

    const targetShift = await prisma.shift.findUnique({ where: { id: targetShiftId } })
    if (!targetShift) return res.status(404).json({ message: 'Target shift not found' })
    if (!targetShift.published) return res.status(400).json({ message: 'The target shift must be published to be swapped' })

    const request = await prisma.shiftSwapRequest.create({
      data: { requesterId, requesterShiftId, targetShiftId, reason: reason || null },
      include: {
        requester:      { select: { id: true, name: true, employeeNumber: true } },
        requesterShift: { include: shiftInclude },
        targetShift:    { include: shiftInclude }
      }
    })

    res.status(201).json(request)
  } catch (err) {
    next(err)
  }
})

// PATCH /shift-swap-requests/:id/target-response
// Only the owner of the targetShift can call this
router.patch('/:id/target-response', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id as string)
    const { accept } = req.body
    const userId = req.user!.userId

    if (typeof accept !== 'boolean') {
      return res.status(400).json({ message: 'accept must be a boolean' })
    }

    const existing = await prisma.shiftSwapRequest.findUnique({
      where: { id },
      include: { targetShift: true }
    })
    if (!existing) return res.status(404).json({ message: 'Shift swap request not found' })

    if (existing.targetShift.userId !== userId) {
      return res.status(403).json({ message: 'Only the target shift owner can respond' })
    }

    if (existing.targetAccepted !== null) {
      return res.status(409).json({ message: 'Already responded to this request' })
    }

    const updated = await prisma.shiftSwapRequest.update({
      where: { id },
      data: {
        targetAccepted: accept,
        ...(accept ? {} : { status: 'REJECTED' })
      },
      include: {
        requester:      { select: { id: true, name: true, employeeNumber: true } },
        requesterShift: { include: shiftInclude },
        targetShift:    { include: shiftInclude }
      }
    })

    res.json(updated)
  } catch (err) {
    next(err)
  }
})

// PATCH /shift-swap-requests/:id/status  (ADMIN/MANAGER only)
// Only allowed after the target has accepted (targetAccepted == true)
router.patch('/:id/status', requireRole('ADMIN', 'MANAGER'), validate(updateRequestStatusSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id as string)
    const { status } = req.body

    const existing = await prisma.shiftSwapRequest.findUnique({
      where: { id },
      include: { requesterShift: true, targetShift: true }
    })
    if (!existing) return res.status(404).json({ message: 'Shift swap request not found' })
    if (existing.status !== 'PENDING') return res.status(409).json({ message: 'Request has already been processed' })
    if (existing.targetAccepted !== true) return res.status(409).json({ message: 'Target employee has not accepted the swap yet' })

    if (status === 'APPROVED') {
      await prisma.$transaction([
        prisma.shift.update({
          where: { id: existing.requesterShiftId },
          data:  { userId: existing.targetShift.userId }
        }),
        prisma.shift.update({
          where: { id: existing.targetShiftId },
          data:  { userId: existing.requesterShift.userId }
        })
      ])
    }

    const approver = await prisma.user.findUnique({ where: { id: req.user!.userId } })

    const updated = await prisma.shiftSwapRequest.update({
      where: { id },
      data:  { status, approvedByName: approver?.name ?? null },
      include: {
        requester:      { select: { id: true, name: true, employeeNumber: true } },
        requesterShift: { include: shiftInclude },
        targetShift:    { include: shiftInclude }
      }
    })

    res.json(updated)
  } catch (err) {
    next(err)
  }
})

// DELETE /shift-swap-requests/:id
router.delete('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id as string)
    const { userId, role } = req.user!

    const existing = await prisma.shiftSwapRequest.findUnique({ where: { id } })
    if (!existing) return res.status(404).json({ message: 'Shift swap request not found' })

    const isOwner   = existing.requesterId === userId
    const isManager = role === 'ADMIN' || role === 'MANAGER'

    if (!isOwner && !isManager) return res.status(403).json({ message: 'Forbidden' })
    if (isOwner && !isManager && existing.status !== 'PENDING') {
      return res.status(409).json({ message: 'Cannot delete a request that has already been processed' })
    }

    await prisma.shiftSwapRequest.delete({ where: { id } })
    res.status(204).send()
  } catch (err) {
    next(err)
  }
})

export default router
