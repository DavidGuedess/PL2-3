import { Router } from 'express'
import { authenticate } from '../middleware/auth'
import { PrismaClient } from '@prisma/client'
import { createAvailabilitySchema } from '../schemas'

const prisma = new PrismaClient()

const router = Router()

/**
 * @openapi
 * components:
 *   schemas:
 *     Availability:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         userId:
 *           type: integer
 *         date:
 *           type: string
 *           format: date
 *         type:
 *           type: string
 *           enum: [PREFERRED, UNAVAILABLE]
 *         note:
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
 *     CreateAvailabilityInput:
 *       type: object
 *       required: [date, type]
 *       properties:
 *         date:
 *           type: string
 *           format: date
 *           example: "2026-03-25"
 *         type:
 *           type: string
 *           enum: [PREFERRED, UNAVAILABLE]
 *         note:
 *           type: string
 */

/**
 * @openapi
 * /availability:
 *   get:
 *     summary: Listar disponibilidades (próprias para EMPLOYEE, todas para Admin/Gestor)
 *     tags: [Disponibilidades]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: userId
 *         schema:
 *           type: integer
 *       - in: query
 *         name: startDate
 *         schema:
 *           type: string
 *           format: date
 *       - in: query
 *         name: endDate
 *         schema:
 *           type: string
 *           format: date
 *     responses:
 *       200:
 *         description: Lista de disponibilidades
 *       401:
 *         description: Não autenticado
 */
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

/**
 * @openapi
 * /availability:
 *   post:
 *     summary: Definir a própria disponibilidade numa data (cria ou atualiza)
 *     tags: [Disponibilidades]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/CreateAvailabilityInput'
 *     responses:
 *       201:
 *         description: Disponibilidade registada
 *       400:
 *         description: Dados inválidos
 *       401:
 *         description: Não autenticado
 */
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

/**
 * @openapi
 * /availability/{id}:
 *   delete:
 *     summary: Remover uma disponibilidade (própria ou Admin/Gestor)
 *     tags: [Disponibilidades]
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
 *         description: Disponibilidade removida
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão
 *       404:
 *         description: Disponibilidade não encontrada
 */
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
