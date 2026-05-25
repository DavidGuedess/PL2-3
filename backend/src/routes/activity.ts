import { Router, Request, Response, NextFunction } from 'express'
import { PrismaClient } from '@prisma/client'
import { authenticate } from '../middleware/auth'

const router = Router()
const prisma = new PrismaClient()

router.use(authenticate)

/**
 * @openapi
 * /activity:
 *   get:
 *     summary: Cronologia de atividade (presenças, folgas e trocas de turno)
 *     description: EMPLOYEE vê apenas a própria atividade; Admin/Gestor pode filtrar por funcionário com ?userId.
 *     tags: [Atividade]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: userId
 *         schema:
 *           type: integer
 *         description: Filtrar por funcionário (apenas Admin/Gestor)
 *       - in: query
 *         name: from
 *         schema:
 *           type: string
 *           format: date
 *         description: Data inicial do intervalo
 *       - in: query
 *         name: to
 *         schema:
 *           type: string
 *           format: date
 *         description: Data final do intervalo
 *       - in: query
 *         name: type
 *         schema:
 *           type: string
 *           enum: [all, attendance, timeoff, swap]
 *           default: all
 *         description: Tipo de evento a incluir
 *     responses:
 *       200:
 *         description: Lista de eventos por ordem cronológica decrescente
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 type: object
 *                 properties:
 *                   id:
 *                     type: string
 *                   type:
 *                     type: string
 *                     enum: [attendance, timeoff, swap]
 *                   action:
 *                     type: string
 *                   detail:
 *                     type: string
 *                   timestamp:
 *                     type: string
 *                     format: date-time
 *                   userId:
 *                     type: integer
 *                   userName:
 *                     type: string
 *                   employeeNumber:
 *                     type: string
 *                   status:
 *                     type: string
 *                     nullable: true
 *       401:
 *         description: Não autenticado
 */
// GET /activity
// Returns a unified timeline of attendance, time-off and shift-swap events for a user.
// EMPLOYEE: only own activity
// ADMIN/MANAGER: ?userId (required) or all if omitted
router.get('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { userId: authUserId, role } = req.user!
    const isManager = role === 'ADMIN' || role === 'MANAGER'

    const targetUserId = isManager && req.query.userId
      ? parseInt(req.query.userId as string)
      : authUserId

    const from = req.query.from ? new Date(req.query.from as string) : undefined
    const to   = req.query.to
      ? (() => { const d = new Date(req.query.to as string); d.setUTCHours(23,59,59,999); return d })()
      : undefined

    const typeFilter = (req.query.type as string | undefined) ?? 'all'

    const dateRange = (field: string) => {
      const filter: any = {}
      if (from) filter.gte = from
      if (to)   filter.lte = to
      return Object.keys(filter).length ? { [field]: filter } : {}
    }

    // ── attendance ──────────────────────────────────────────────────────────
    const attendanceItems: any[] = []
    if (typeFilter === 'all' || typeFilter === 'attendance') {
      const records = await prisma.attendanceRecord.findMany({
        where: {
          userId: targetUserId,
          ...dateRange('timestamp'),
        },
        include: { user: { select: { id: true, name: true, employeeNumber: true } } },
        orderBy: { timestamp: 'desc' },
      })
      for (const r of records) {
        attendanceItems.push({
          id:        `attendance-${r.id}`,
          type:      'attendance',
          action:    r.type === 'IN' ? 'Entrada registada' : 'Saída registada',
          detail:    r.type === 'IN' ? 'Check-in' : 'Check-out',
          timestamp: r.timestamp.toISOString(),
          userId:    r.userId,
          userName:  r.user.name,
          employeeNumber: r.user.employeeNumber,
          status:    null,
          ref:       { attendanceType: r.type, note: r.note ?? null },
        })
      }
    }

    // ── time-off requests ────────────────────────────────────────────────────
    const timeOffItems: any[] = []
    if (typeFilter === 'all' || typeFilter === 'timeoff') {
      const where: any = isManager
        ? { userId: targetUserId, ...dateRange('createdAt') }
        : { userId: authUserId, ...dateRange('createdAt') }

      const requests = await prisma.timeOffRequest.findMany({
        where,
        include: { user: { select: { id: true, name: true, employeeNumber: true } } },
        orderBy: { createdAt: 'desc' },
      })
      for (const r of requests) {
        const start = r.startDate.toISOString().slice(0, 10)
        const end   = r.endDate.toISOString().slice(0, 10)
        timeOffItems.push({
          id:        `timeoff-${r.id}`,
          type:      'timeoff',
          action:    'Pedido de ferias',
          detail:    `${start} ate ${end}${r.reason ? ` · ${r.reason}` : ''}`,
          timestamp: r.updatedAt.toISOString(),
          userId:    r.userId,
          userName:  r.user.name,
          employeeNumber: r.user.employeeNumber,
          status:    r.status,
          ref:       { startDate: start, endDate: end, approvedByName: r.approvedByName },
        })
      }
    }

    // ── shift-swap requests ──────────────────────────────────────────────────
    const swapItems: any[] = []
    if (typeFilter === 'all' || typeFilter === 'swap') {
      const where: any = {
        OR: [
          { requesterId: targetUserId },
          { targetShift: { userId: targetUserId } },
        ],
        ...dateRange('createdAt'),
      }

      const requests = await prisma.shiftSwapRequest.findMany({
        where,
        include: {
          requester:      { select: { id: true, name: true, employeeNumber: true } },
          requesterShift: { include: { user: { select: { id: true, name: true, employeeNumber: true } } } },
          targetShift:    { include: { user: { select: { id: true, name: true, employeeNumber: true } } } },
        },
        orderBy: { createdAt: 'desc' },
      })
      for (const r of requests) {
        const requDate = r.requesterShift.date.toISOString().slice(0, 10)
        const targDate = r.targetShift.date.toISOString().slice(0, 10)
        swapItems.push({
          id:        `swap-${r.id}`,
          type:      'swap',
          action:    'Pedido de troca de turno',
          detail:    `${requDate} ↔ ${targDate}${r.reason ? ` · ${r.reason}` : ''}`,
          timestamp: r.updatedAt.toISOString(),
          userId:    r.requesterId,
          userName:  r.requester.name,
          employeeNumber: r.requester.employeeNumber,
          status:    r.status,
          ref:       {
            requesterName: r.requester.name,
            targetName:    r.targetShift.user?.name ?? null,
            approvedByName: r.approvedByName,
            targetAccepted: r.targetAccepted,
          },
        })
      }
    }

    // ── merge & sort ─────────────────────────────────────────────────────────
    const all = [...attendanceItems, ...timeOffItems, ...swapItems]
      .sort((a, b) => b.timestamp.localeCompare(a.timestamp))

    return res.json(all)
  } catch (err) {
    next(err)
  }
})

export default router
