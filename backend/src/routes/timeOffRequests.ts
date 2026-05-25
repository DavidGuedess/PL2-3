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

/**
 * @openapi
 * components:
 *   schemas:
 *     TimeOffRequest:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         userId:
 *           type: integer
 *         startDate:
 *           type: string
 *           format: date
 *         endDate:
 *           type: string
 *           format: date
 *         allDay:
 *           type: boolean
 *         reason:
 *           type: string
 *           nullable: true
 *         status:
 *           type: string
 *           enum: [PENDING, APPROVED, REJECTED]
 *         approvedByName:
 *           type: string
 *           nullable: true
 *         user:
 *           type: object
 *           properties:
 *             id:
 *               type: integer
 *             name:
 *               type: string
 *             employeeNumber:
 *               type: string
 *     CreateTimeOffRequestInput:
 *       type: object
 *       required: [startDate, endDate]
 *       properties:
 *         startDate:
 *           type: string
 *           format: date
 *           example: "2026-03-25"
 *         endDate:
 *           type: string
 *           format: date
 *           example: "2026-03-27"
 *         allDay:
 *           type: boolean
 *           default: true
 *         reason:
 *           type: string
 */

/**
 * @openapi
 * /time-off-requests:
 *   get:
 *     summary: Listar pedidos de folga (próprios para EMPLOYEE, todos para Admin/Gestor)
 *     tags: [Pedidos de Folga]
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
 *         description: Data inicial do intervalo de criação
 *       - in: query
 *         name: to
 *         schema:
 *           type: string
 *           format: date
 *         description: Data final do intervalo de criação
 *     responses:
 *       200:
 *         description: Lista de pedidos de folga
 *       401:
 *         description: Não autenticado
 */
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

/**
 * @openapi
 * /time-off-requests:
 *   post:
 *     summary: Submeter um pedido de folga
 *     tags: [Pedidos de Folga]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/CreateTimeOffRequestInput'
 *     responses:
 *       201:
 *         description: Pedido criado
 *       400:
 *         description: Dados inválidos
 *       401:
 *         description: Não autenticado
 */
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

/**
 * @openapi
 * /time-off-requests/{id}/status:
 *   patch:
 *     summary: Aprovar ou rejeitar um pedido de folga (Admin/Gestor)
 *     tags: [Pedidos de Folga]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required: [status]
 *             properties:
 *               status:
 *                 type: string
 *                 enum: [APPROVED, REJECTED]
 *     responses:
 *       200:
 *         description: Pedido atualizado
 *       400:
 *         description: Estado inválido
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN ou MANAGER)
 *       404:
 *         description: Pedido não encontrado
 */
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

/**
 * @openapi
 * /time-off-requests/{id}:
 *   delete:
 *     summary: Eliminar um pedido de folga (próprio pendente ou Admin/Gestor)
 *     tags: [Pedidos de Folga]
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
 *         description: Pedido eliminado
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão
 *       404:
 *         description: Pedido não encontrado
 *       409:
 *         description: Não é possível eliminar um pedido já processado
 */
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
