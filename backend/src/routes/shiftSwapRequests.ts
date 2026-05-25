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

/**
 * @openapi
 * components:
 *   schemas:
 *     ShiftSwapRequest:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         requesterId:
 *           type: integer
 *         requesterShiftId:
 *           type: integer
 *         targetShiftId:
 *           type: integer
 *         reason:
 *           type: string
 *           nullable: true
 *         status:
 *           type: string
 *           enum: [PENDING, APPROVED, REJECTED]
 *         targetAccepted:
 *           type: boolean
 *           nullable: true
 *         approvedByName:
 *           type: string
 *           nullable: true
 *     CreateShiftSwapRequestInput:
 *       type: object
 *       required: [requesterShiftId, targetShiftId]
 *       properties:
 *         requesterShiftId:
 *           type: integer
 *           example: 1
 *         targetShiftId:
 *           type: integer
 *           example: 2
 *         reason:
 *           type: string
 */

/**
 * @openapi
 * /shift-swap-requests:
 *   get:
 *     summary: Listar pedidos de troca de turno
 *     description: EMPLOYEE vê os próprios e os que visam os seus turnos; Admin/Gestor vê todos.
 *     tags: [Trocas de Turno]
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
 *         description: Lista de pedidos de troca
 *       401:
 *         description: Não autenticado
 */
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

/**
 * @openapi
 * /shift-swap-requests:
 *   post:
 *     summary: Pedir a troca de um turno próprio por outro
 *     tags: [Trocas de Turno]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/CreateShiftSwapRequestInput'
 *     responses:
 *       201:
 *         description: Pedido de troca criado
 *       400:
 *         description: Dados inválidos ou turno não publicado
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Só pode trocar os seus próprios turnos
 *       404:
 *         description: Turno de origem ou de destino não encontrado
 */
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

/**
 * @openapi
 * /shift-swap-requests/{id}/target-response:
 *   patch:
 *     summary: Aceitar ou recusar uma troca (dono do turno de destino)
 *     tags: [Trocas de Turno]
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
 *             required: [accept]
 *             properties:
 *               accept:
 *                 type: boolean
 *     responses:
 *       200:
 *         description: Resposta registada
 *       400:
 *         description: accept tem de ser booleano
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Apenas o dono do turno de destino pode responder
 *       404:
 *         description: Pedido de troca não encontrado
 *       409:
 *         description: Já respondeu a este pedido
 */
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

/**
 * @openapi
 * /shift-swap-requests/{id}/status:
 *   patch:
 *     summary: Aprovar ou rejeitar uma troca de turno (Admin/Gestor)
 *     description: Só permitido depois de o funcionário de destino ter aceitado a troca.
 *     tags: [Trocas de Turno]
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
 *         description: Pedido atualizado (se aprovado, os turnos são trocados)
 *       400:
 *         description: Estado inválido
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN ou MANAGER)
 *       404:
 *         description: Pedido de troca não encontrado
 *       409:
 *         description: Pedido já processado ou ainda não aceite pelo funcionário de destino
 */
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

/**
 * @openapi
 * /shift-swap-requests/{id}:
 *   delete:
 *     summary: Eliminar um pedido de troca (próprio pendente ou Admin/Gestor)
 *     tags: [Trocas de Turno]
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
 *         description: Pedido de troca não encontrado
 *       409:
 *         description: Não é possível eliminar um pedido já processado
 */
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
