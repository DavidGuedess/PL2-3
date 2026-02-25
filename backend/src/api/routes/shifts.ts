import { Router, Response } from 'express'
import { prisma } from '../../models/prisma'
import { authenticate, requireAdmin, requireManager, AuthRequest } from '../../middleware/auth'

const router = Router()
router.use(authenticate)

router.get('/templates', async (_req, res: Response): Promise<void> => {
  const templates = await prisma.shiftTemplate.findMany({
    where: { isActive: true },
    orderBy: { shiftType: 'asc' },
  })
  res.json(templates)
})

router.post('/templates', requireAdmin, async (req, res: Response): Promise<void> => {
  const { name, shiftType, startTime, endTime, description } = req.body
  if (!name || !shiftType || !startTime || !endTime) {
    res.status(400).json({ error: 'name, shiftType, startTime and endTime are required' })
    return
  }
  const template = await prisma.shiftTemplate.create({
    data: { name, shiftType, startTime, endTime, description },
  })
  res.status(201).json(template)
})

router.get('/assignments', async (req: AuthRequest, res: Response): Promise<void> => {
  const { employeeId, startDate, endDate } = req.query
  const isManager = ['ADMIN', 'MANAGER'].includes(req.user!.role)

  const where: Record<string, unknown> = {}
  where.employeeId = isManager && employeeId
    ? Number(employeeId)
    : isManager ? undefined : req.user!.userId

  if (startDate) where.date = { ...(where.date as object ?? {}), gte: new Date(startDate as string) }
  if (endDate) where.date = { ...(where.date as object ?? {}), lte: new Date(endDate as string) }

  const assignments = await prisma.shiftAssignment.findMany({
    where,
    include: {
      shiftTemplate: true,
      employee: { select: { id: true, fullName: true, employeeNumber: true } },
    },
    orderBy: { date: 'asc' },
  })
  res.json(assignments)
})

router.post('/assignments', requireManager, async (req: AuthRequest, res: Response): Promise<void> => {
  const { employeeId, shiftTemplateId, date, notes } = req.body
  if (!employeeId || !shiftTemplateId || !date) {
    res.status(400).json({ error: 'employeeId, shiftTemplateId and date are required' })
    return
  }
  try {
    const assignment = await prisma.shiftAssignment.create({
      data: {
        employeeId: Number(employeeId),
        shiftTemplateId: Number(shiftTemplateId),
        date: new Date(date),
        notes,
        assignedById: req.user!.userId,
      },
      include: { shiftTemplate: true },
    })
    res.status(201).json(assignment)
  } catch (e: unknown) {
    if ((e as { code?: string }).code === 'P2002') {
      res.status(409).json({ error: 'Employee already has a shift on this date' })
      return
    }
    throw e
  }
})

router.delete('/assignments/:id', requireManager, async (req, res: Response): Promise<void> => {
  const id = Number(req.params.id)
  const exists = await prisma.shiftAssignment.findUnique({ where: { id } })
  if (!exists) { res.status(404).json({ error: 'Assignment not found' }); return }
  await prisma.shiftAssignment.delete({ where: { id } })
  res.status(204).send()
})

router.get('/requests', async (req: AuthRequest, res: Response): Promise<void> => {
  const isManager = ['ADMIN', 'MANAGER'].includes(req.user!.role)
  const where = isManager ? {} : { requesterId: req.user!.userId }

  const requests = await prisma.shiftRequest.findMany({
    where,
    include: {
      requester: { select: { id: true, fullName: true, employeeNumber: true } },
      targetEmployee: { select: { id: true, fullName: true, employeeNumber: true } },
      originalAssignment: { include: { shiftTemplate: true } },
    },
    orderBy: { createdAt: 'desc' },
  })
  res.json(requests)
})

router.post('/requests', async (req: AuthRequest, res: Response): Promise<void> => {
  const { requestType, originalAssignmentId, targetEmployeeId, startDate, endDate, justification } = req.body
  if (!requestType) {
    res.status(400).json({ error: 'requestType is required' })
    return
  }
  const request = await prisma.shiftRequest.create({
    data: {
      requestType,
      requesterId: req.user!.userId,
      originalAssignmentId: originalAssignmentId ? Number(originalAssignmentId) : undefined,
      targetEmployeeId: targetEmployeeId ? Number(targetEmployeeId) : undefined,
      startDate: startDate ? new Date(startDate) : undefined,
      endDate: endDate ? new Date(endDate) : undefined,
      justification,
    },
  })
  res.status(201).json(request)
})

router.patch('/requests/:id/review', requireManager, async (req: AuthRequest, res: Response): Promise<void> => {
  const id = Number(req.params.id)
  const { status, managerComment } = req.body

  const exists = await prisma.shiftRequest.findUnique({ where: { id } })
  if (!exists) { res.status(404).json({ error: 'Request not found' }); return }

  const updated = await prisma.shiftRequest.update({
    where: { id },
    data: { status, managerComment, reviewerId: req.user!.userId, reviewedAt: new Date() },
  })
  res.json(updated)
})

router.post('/attendance/clock-in', async (req: AuthRequest, res: Response): Promise<void> => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const existing = await prisma.attendanceRecord.findFirst({
    where: { employeeId: req.user!.userId, date: today },
  })
  if (existing && !existing.clockOut) {
    res.status(409).json({ error: 'Already clocked in' })
    return
  }

  const record = await prisma.attendanceRecord.create({
    data: { employeeId: req.user!.userId, date: today, clockIn: new Date() },
  })
  res.status(201).json(record)
})

router.patch('/attendance/clock-out', async (req: AuthRequest, res: Response): Promise<void> => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const record = await prisma.attendanceRecord.findFirst({
    where: { employeeId: req.user!.userId, date: today, clockOut: null },
    orderBy: { clockIn: 'desc' },
  })
  if (!record) { res.status(404).json({ error: 'No active clock-in found' }); return }

  const updated = await prisma.attendanceRecord.update({
    where: { id: record.id },
    data: { clockOut: new Date() },
  })
  res.json(updated)
})

export default router
