import { Router, Request, Response } from 'express'
import { PrismaClient } from '@prisma/client'
import { authenticate, requireRole } from '../middleware/auth'

const router = Router()
const prisma = new PrismaClient()

router.use(authenticate)

/**
 * @openapi
 * components:
 *   schemas:
 *     ShiftType:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         name:
 *           type: string
 *         startTime:
 *           type: string
 *           example: "08:00"
 *         endTime:
 *           type: string
 *           example: "16:00"
 *         createdAt:
 *           type: string
 *           format: date-time
 *         updatedAt:
 *           type: string
 *           format: date-time
 *     CreateShiftTypeInput:
 *       type: object
 *       required: [name, startTime, endTime]
 *       properties:
 *         name:
 *           type: string
 *           example: "Manhã"
 *         startTime:
 *           type: string
 *           example: "08:00"
 *         endTime:
 *           type: string
 *           example: "16:00"
 */

/**
 * @openapi
 * /shift-types:
 *   get:
 *     summary: Listar todos os tipos de turno
 *     tags: [Turnos]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *           minimum: 1
 *         description: Número da página
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           minimum: 1
 *         description: Número de resultados por página
 *     responses:
 *       200:
 *         description: Lista de tipos de turno (com paginação se page e limit fornecidos)
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 $ref: '#/components/schemas/ShiftType'
 *       400:
 *         description: Parâmetros inválidos
 *       401:
 *         description: Não autenticado
 */
router.get('/', async (req: Request, res: Response) => {
  const { page, limit } = req.query

  const pageNum = page !== undefined ? parseInt(page as string, 10) : undefined
  const limitNum = limit !== undefined ? parseInt(limit as string, 10) : undefined

  if (pageNum !== undefined && (isNaN(pageNum) || pageNum < 1)) {
    return res.status(400).json({ message: 'page must be a positive integer' })
  }
  if (limitNum !== undefined && (isNaN(limitNum) || limitNum < 1)) {
    return res.status(400).json({ message: 'limit must be a positive integer' })
  }

  if (pageNum !== undefined && limitNum !== undefined) {
    const skip = (pageNum - 1) * limitNum
    const [total, shiftTypes] = await Promise.all([
      prisma.shiftType.count(),
      prisma.shiftType.findMany({ orderBy: { name: 'asc' }, skip, take: limitNum })
    ])
    return res.status(200).json({
      data: shiftTypes,
      total,
      page: pageNum,
      limit: limitNum,
      totalPages: Math.ceil(total / limitNum)
    })
  }

  const shiftTypes = await prisma.shiftType.findMany({ orderBy: { name: 'asc' } })
  return res.status(200).json(shiftTypes)
})

/**
 * @openapi
 * /shift-types:
 *   post:
 *     summary: Criar um novo tipo de turno (Admin/Gestor)
 *     tags: [Turnos]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/CreateShiftTypeInput'
 *     responses:
 *       201:
 *         description: Tipo de turno criado
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ShiftType'
 *       400:
 *         description: Campos obrigatórios em falta
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN ou MANAGER)
 *       409:
 *         description: Já existe um tipo de turno com esse nome
 */
router.post('/', requireRole('ADMIN', 'MANAGER'), async (req: Request, res: Response) => {
  const { name, startTime, endTime } = req.body

  if (!name || !startTime || !endTime) {
    return res.status(400).json({ message: 'name, startTime and endTime are required' })
  }

  const existing = await prisma.shiftType.findUnique({ where: { name } })
  if (existing) {
    return res.status(409).json({ message: 'Shift type with this name already exists' })
  }

  const shiftType = await prisma.shiftType.create({
    data: { name, startTime, endTime }
  })

  res.status(201).json(shiftType)
})

/**
 * @openapi
 * /shift-types/{id}:
 *   patch:
 *     summary: Atualizar um tipo de turno (Admin/Gestor)
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
 *             $ref: '#/components/schemas/CreateShiftTypeInput'
 *     responses:
 *       200:
 *         description: Tipo de turno atualizado
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ShiftType'
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN ou MANAGER)
 *       404:
 *         description: Tipo de turno não encontrado
 */
router.patch('/:id', requireRole('ADMIN', 'MANAGER'), async (req: Request, res: Response) => {
  const id = parseInt(req.params.id)
  const { name, startTime, endTime } = req.body

  const existing = await prisma.shiftType.findUnique({ where: { id } })
  if (!existing) {
    return res.status(404).json({ message: 'Shift type not found' })
  }

  const shiftType = await prisma.shiftType.update({
    where: { id },
    data: { name, startTime, endTime }
  })

  res.json(shiftType)
})

/**
 * @openapi
 * /shift-types/{id}:
 *   delete:
 *     summary: Eliminar um tipo de turno (Admin/Gestor)
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
 *         description: Tipo de turno eliminado
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN ou MANAGER)
 *       404:
 *         description: Tipo de turno não encontrado
 */
router.delete('/:id', requireRole('ADMIN', 'MANAGER'), async (req: Request, res: Response) => {
  const id = parseInt(req.params.id)

  const existing = await prisma.shiftType.findUnique({ where: { id } })
  if (!existing) {
    return res.status(404).json({ message: 'Shift type not found' })
  }

  await prisma.shiftType.delete({ where: { id } })
  res.status(204).send()
})

export default router
