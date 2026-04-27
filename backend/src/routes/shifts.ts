import { Router, Request, Response } from 'express'
import { PrismaClient } from '@prisma/client'
import { authenticate, requireRole } from '../middleware/auth'

const router = Router()
const prisma = new PrismaClient()

router.use(authenticate)

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
 *         description: Funcionário já tem turno nessa data
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
