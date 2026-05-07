import { Router, Request, Response } from 'express'
import { PrismaClient } from '@prisma/client'
import { authenticate, requireRole } from '../middleware/auth'

const router = Router()
const prisma = new PrismaClient()

router.use(authenticate)

function toMinutes(time: string): number {
  const [h, m] = time.split(':').map(Number)
  return h * 60 + m
}

function shiftsOverlap(
  date1: Date, startTime1: string, endTime1: string,
  date2: Date, startTime2: string, endTime2: string
): boolean {
  const MINS_PER_DAY = 24 * 60
  const base1 = date1.getTime() / 60000
  const base2 = date2.getTime() / 60000
  const s1 = toMinutes(startTime1)
  const e1 = toMinutes(endTime1)
  const s2 = toMinutes(startTime2)
  const e2 = toMinutes(endTime2)
  const abs1Start = base1 + s1
  const abs1End   = base1 + (e1 > s1 ? e1 : e1 + MINS_PER_DAY)
  const abs2Start = base2 + s2
  const abs2End   = base2 + (e2 > s2 ? e2 : e2 + MINS_PER_DAY)
  return abs1Start < abs2End && abs2Start < abs1End
}

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

/**
 * @openapi
 * components:
 *   schemas:
 *     Shift:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         userId:
 *           type: integer
 *         shiftTypeId:
 *           type: integer
 *         date:
 *           type: string
 *           format: date
 *         user:
 *           type: object
 *           properties:
 *             id:
 *               type: integer
 *             name:
 *               type: string
 *             employeeNumber:
 *               type: string
 *         shiftType:
 *           $ref: '#/components/schemas/ShiftType'
 *     CreateShiftInput:
 *       type: object
 *       required: [userId, shiftTypeId, date]
 *       properties:
 *         userId:
 *           type: integer
 *           example: 1
 *         shiftTypeId:
 *           type: integer
 *           example: 1
 *         date:
 *           type: string
 *           format: date
 *           example: "2026-03-25"
 */

/**
 * @openapi
 * /shifts:
 *   post:
 *     summary: Atribuir turno a um funcionário numa data (Admin/Gestor)
 *     tags: [Turnos]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/CreateShiftInput'
 *     responses:
 *       201:
 *         description: Turno atribuído
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/Shift'
 *       400:
 *         description: Campos obrigatórios em falta
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN ou MANAGER)
 *       404:
 *         description: Utilizador ou tipo de turno não encontrado
 *       409:
 *         description: Conflito de horário — o turno sobrepõe-se a um turno existente
 */
router.post('/', requireRole('ADMIN', 'MANAGER'), async (req: Request, res: Response) => {
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

  const dayBefore = new Date(shiftDate)
  dayBefore.setUTCDate(shiftDate.getUTCDate() - 1)
  const dayAfter = new Date(shiftDate)
  dayAfter.setUTCDate(shiftDate.getUTCDate() + 1)

  const nearbyShifts = await prisma.shift.findMany({
    where: { userId: parseInt(userId), date: { gte: dayBefore, lte: dayAfter } },
    include: { shiftType: true }
  })

  const conflict = nearbyShifts.find(s =>
    shiftsOverlap(shiftDate, shiftType.startTime, shiftType.endTime, s.date, s.shiftType.startTime, s.shiftType.endTime)
  )

  if (conflict) {
    const conflictDate = new Date(conflict.date).toISOString().slice(0, 10)
    return res.status(409).json({
      message: `Shift conflict: '${shiftType.name}' (${shiftType.startTime}–${shiftType.endTime}) overlaps with existing shift '${conflict.shiftType.name}' (${conflict.shiftType.startTime}–${conflict.shiftType.endTime}) on ${conflictDate}`
    })
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

/**
 * @openapi
 * /shifts/me:
 *   get:
 *     summary: Consultar os próprios turnos (semana ou mês)
 *     tags: [Turnos]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: week
 *         schema:
 *           type: string
 *           format: date
 *         description: Qualquer data da semana pretendida (ex. 2026-03-23)
 *       - in: query
 *         name: month
 *         schema:
 *           type: string
 *         description: Mês no formato YYYY-MM (ex. 2026-03)
 *     responses:
 *       200:
 *         description: Lista dos turnos do utilizador autenticado no período
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 $ref: '#/components/schemas/Shift'
 *       400:
 *         description: Parâmetro week ou month obrigatório
 *       401:
 *         description: Não autenticado
 */
router.get('/me', async (req: Request, res: Response) => {
  const { week, month } = req.query

  if (!week && !month) {
    return res.status(400).json({
      message: 'Query param week or month is required (e.g. ?week=2026-03-23 or ?month=2026-03)'
    })
  }

  const { start, end } = week
    ? getWeekRange(week as string)
    : getMonthRange(month as string)

  const userId = req.user!.userId

  const shifts = await prisma.shift.findMany({
    where: { userId, date: { gte: start, lte: end } },
    include: { shiftType: true },
    orderBy: { date: 'asc' }
  })

  res.json(shifts)
})

/**
 * @openapi
 * /shifts:
 *   get:
 *     summary: Consultar escala semanal ou mensal
 *     tags: [Turnos]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: week
 *         schema:
 *           type: string
 *           format: date
 *         description: Qualquer data da semana pretendida (ex. 2026-03-23)
 *       - in: query
 *         name: month
 *         schema:
 *           type: string
 *         description: Mês no formato YYYY-MM (ex. 2026-03)
 *     responses:
 *       200:
 *         description: Lista de turnos no período
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 $ref: '#/components/schemas/Shift'
 *       400:
 *         description: Parâmetro week ou month obrigatório
 *       401:
 *         description: Não autenticado
 */
router.get('/', async (req: Request, res: Response) => {
  const { week, month } = req.query

  if (!week && !month) {
    return res.status(400).json({
      message: 'Query param week or month is required (e.g. ?week=2026-03-23 or ?month=2026-03)'
    })
  }

  if (week && typeof week !== 'string') {
    return res.status(400).json({ message: 'week must be a string in format YYYY-MM-DD' })
  }

  if (month && typeof month !== 'string') {
    return res.status(400).json({ message: 'month must be a string in format YYYY-MM' })
  }

  let start: Date
  let end: Date

  if (week) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(week)) {
      return res.status(400).json({ message: 'Invalid week format. Use YYYY-MM-DD' })
    }

    const range = getWeekRange(week)

    if (isNaN(range.start.getTime()) || isNaN(range.end.getTime())) {
      return res.status(400).json({ message: 'Invalid week date' })
    }

    start = range.start
    end = range.end
  } else {
    if (!/^\d{4}-\d{2}$/.test(month as string)) {
      return res.status(400).json({ message: 'Invalid month format. Use YYYY-MM' })
    }

    const range = getMonthRange(month as string)

    if (isNaN(range.start.getTime()) || isNaN(range.end.getTime())) {
      return res.status(400).json({ message: 'Invalid month date' })
    }

    start = range.start
    end = range.end
  }

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

/**
 * @openapi
 * /shifts/{id}:
 *   patch:
 *     summary: Editar um turno (Admin/Gestor)
 *     tags: [Turnos]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     requestBody:
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               shiftTypeId:
 *                 type: integer
 *                 example: 2
 *               date:
 *                 type: string
 *                 format: date
 *                 example: "2026-04-30"
 *     responses:
 *       200:
 *         description: Turno atualizado
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/Shift'
 *       400:
 *         description: Nenhum campo fornecido para atualizar
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN ou MANAGER)
 *       404:
 *         description: Turno ou tipo de turno não encontrado
 *       409:
 *         description: Funcionário já tem turno nessa data
 */
router.patch('/:id', requireRole('ADMIN', 'MANAGER'), async (req: Request, res: Response) => {
  const id = parseInt(req.params.id)
  const { shiftTypeId, date } = req.body

  if (!shiftTypeId && !date) {
    return res.status(400).json({ message: 'shiftTypeId or date is required' })
  }

  const existing = await prisma.shift.findUnique({ where: { id } })
  if (!existing) {
    return res.status(404).json({ message: 'Shift not found' })
  }

  if (shiftTypeId) {
    const shiftType = await prisma.shiftType.findUnique({ where: { id: parseInt(shiftTypeId) } })
    if (!shiftType) {
      return res.status(404).json({ message: 'Shift type not found' })
    }
  }

  if (date) {
    const shiftDate = new Date(date)
    const conflict = await prisma.shift.findUnique({
      where: { userId_date: { userId: existing.userId, date: shiftDate } }
    })
    if (conflict && conflict.id !== id) {
      return res.status(409).json({ message: 'User already has a shift on this date' })
    }
  }

  const updateData: { shiftTypeId?: number; date?: Date } = {}
  if (shiftTypeId) updateData.shiftTypeId = parseInt(shiftTypeId)
  if (date) updateData.date = new Date(date)

  const shift = await prisma.shift.update({
    where: { id },
    data: updateData,
    include: {
      user: { select: { id: true, name: true, employeeNumber: true } },
      shiftType: true
    }
  })

  res.json(shift)
})

/**
 * @openapi
 * /shifts/{id}:
 *   delete:
 *     summary: Remover um turno (Admin/Gestor)
 *     tags: [Turnos]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     responses:
 *       204:
 *         description: Turno removido
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN ou MANAGER)
 *       404:
 *         description: Turno não encontrado
 */
router.delete('/:id', requireRole('ADMIN', 'MANAGER'), async (req: Request, res: Response) => {
  const id = parseInt(req.params.id)

  const existing = await prisma.shift.findUnique({ where: { id } })
  if (!existing) {
    return res.status(404).json({ message: 'Shift not found' })
  }

  await prisma.shift.delete({ where: { id } })
  res.status(204).send()
})

export default router
