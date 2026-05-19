import { Router, Request, Response } from 'express'
import { PrismaClient } from '@prisma/client'
import { authenticate, requireRole } from '../middleware/auth'
import { createAttendance, getMyAttendance, getAttendanceReport, getAttendanceStats } from '../controllers/attendanceController'

const router = Router()
const prisma = new PrismaClient()

router.use(authenticate)

/**
 * @openapi
 * components:
 *   schemas:
 *     AttendanceRecord:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         userId:
 *           type: integer
 *         type:
 *           type: string
 *           enum: [IN, OUT]
 *         timestamp:
 *           type: string
 *           format: date-time
 *         user:
 *           type: object
 *           properties:
 *             id:
 *               type: integer
 *             name:
 *               type: string
 *             employeeNumber:
 *               type: string
 *     CreateAttendanceInput:
 *       type: object
 *       required: [type]
 *       properties:
 *         type:
 *           type: string
 *           enum: [IN, OUT]
 *           example: "IN"
 */

/**
 * @openapi
 * /attendance:
 *   post:
 *     summary: Registar entrada ou saída do funcionário autenticado
 *     tags: [Registos de Ponto]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/CreateAttendanceInput'
 *     responses:
 *       201:
 *         description: Registo criado
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/AttendanceRecord'
 *       400:
 *         description: Campo type em falta ou inválido
 *       401:
 *         description: Não autenticado
 */
router.post('/', createAttendance)

/**
 * @openapi
 * /attendance/me:
 *   get:
 *     summary: Consultar o próprio histórico de registos de ponto
 *     tags: [Registos de Ponto]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: from
 *         schema:
 *           type: string
 *           format: date
 *         description: Data de início (ex. 2026-04-01)
 *       - in: query
 *         name: to
 *         schema:
 *           type: string
 *           format: date
 *         description: Data de fim (ex. 2026-04-30)
 *     responses:
 *       200:
 *         description: Lista de registos de ponto do utilizador autenticado
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 $ref: '#/components/schemas/AttendanceRecord'
 *       401:
 *         description: Não autenticado
 */
router.get('/me', getMyAttendance)

/**
 * @openapi
 * /attendance/report:
 *   get:
 *     summary: Relatório de presenças e ausências por utilizador (Admin/Gestor)
 *     tags: [Registos de Ponto]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: from
 *         schema:
 *           type: string
 *           format: date
 *         description: Data de início (ex. 2026-04-01)
 *       - in: query
 *         name: to
 *         schema:
 *           type: string
 *           format: date
 *         description: Data de fim (ex. 2026-04-30)
 *       - in: query
 *         name: userId
 *         schema:
 *           type: integer
 *         description: Filtrar por utilizador específico (opcional)
 *     responses:
 *       200:
 *         description: Relatório de presenças agrupado por utilizador
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 type: object
 *                 properties:
 *                   userId:
 *                     type: integer
 *                   name:
 *                     type: string
 *                   employeeNumber:
 *                     type: string
 *                   totalScheduledDays:
 *                     type: integer
 *                   daysPresent:
 *                     type: integer
 *                   daysAbsent:
 *                     type: integer
 *       400:
 *         description: userId inválido
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN ou MANAGER)
 *       404:
 *         description: Utilizador não encontrado
 */
router.get('/report', requireRole('ADMIN', 'MANAGER'), getAttendanceReport)

/**
 * @openapi
 * /attendance/stats:
 *   get:
 *     summary: Estatísticas de horas trabalhadas por utilizador (Admin/Gestor)
 *     tags: [Registos de Ponto]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: userId
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID do utilizador
 *       - in: query
 *         name: from
 *         schema:
 *           type: string
 *           format: date
 *         description: Data de início (ex. 2026-04-01)
 *       - in: query
 *         name: to
 *         schema:
 *           type: string
 *           format: date
 *         description: Data de fim (ex. 2026-04-30)
 *     responses:
 *       200:
 *         description: Estatísticas de horas trabalhadas
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 userId:
 *                   type: integer
 *                 name:
 *                   type: string
 *                 employeeNumber:
 *                   type: string
 *                 totalHoursWorked:
 *                   type: number
 *                   description: Total de horas trabalhadas (soma dos pares IN/OUT)
 *                 daysWorked:
 *                   type: integer
 *                   description: Número de dias com pelo menos um par IN/OUT completo
 *                 overtimeHours:
 *                   type: number
 *                   description: Horas extra além da duração do turno atribuído
 *       400:
 *         description: userId em falta ou inválido
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN ou MANAGER)
 *       404:
 *         description: Utilizador não encontrado
 */
router.get('/stats', requireRole('ADMIN', 'MANAGER'), getAttendanceStats)

/**
 * @openapi
 * /attendance:
 *   get:
 *     summary: Monitorizar registos de ponto de um funcionário (Admin/Gestor)
 *     tags: [Registos de Ponto]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: userId
 *         schema:
 *           type: integer
 *         description: ID do utilizador a consultar
 *       - in: query
 *         name: from
 *         schema:
 *           type: string
 *           format: date
 *         description: Data de início (ex. 2026-04-01)
 *       - in: query
 *         name: to
 *         schema:
 *           type: string
 *           format: date
 *         description: Data de fim (ex. 2026-04-30)
 *     responses:
 *       200:
 *         description: Lista de registos de ponto
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 $ref: '#/components/schemas/AttendanceRecord'
 *       400:
 *         description: Parâmetro userId obrigatório
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN ou MANAGER)
 *       404:
 *         description: Utilizador não encontrado
 */
router.get('/', requireRole('ADMIN', 'MANAGER'), async (req: Request, res: Response) => {
  const { userId, from, to } = req.query

  if (!userId) {
    return res.status(400).json({ message: 'userId query param is required' })
  }

  const userIdNum = Number(userId)

  if (!Number.isInteger(userIdNum) || userIdNum <= 0) {
    return res.status(400).json({ message: 'userId must be a valid integer' })
  }

  const user = await prisma.user.findUnique({ where: { id: userIdNum } })

  if (!user) {
    return res.status(404).json({ message: 'User not found' })
  }

  const where: any = { userId: userIdNum }

  if (from || to) {
    where.timestamp = {}

    if (from) {
      where.timestamp.gte = new Date(from as string)
    }

    if (to) {
      const toDate = new Date(to as string)
      toDate.setUTCHours(23, 59, 59, 999)
      where.timestamp.lte = toDate
    }
  }

  const records = await prisma.attendanceRecord.findMany({
    where,
    include: {
      user: {
        select: {
          id: true,
          name: true,
          employeeNumber: true
        }
      }
    },
    orderBy: { timestamp: 'desc' }
  })

  return res.status(200).json(records)
})

export default router