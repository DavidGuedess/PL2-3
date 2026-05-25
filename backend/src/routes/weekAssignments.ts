import { Router, Request, Response, NextFunction } from 'express'
import { PrismaClient } from '@prisma/client'
import { authenticate, requireRole } from '../middleware/auth'

const router = Router()
const prisma = new PrismaClient()

router.use(authenticate)

function getWeekStart(dateStr: string): Date {
  const base = new Date(dateStr)
  const day  = base.getUTCDay()
  const diff = day === 0 ? -6 : 1 - day
  const start = new Date(base)
  start.setUTCDate(base.getUTCDate() + diff)
  start.setUTCHours(0, 0, 0, 0)
  return start
}

/**
 * @openapi
 * components:
 *   schemas:
 *     WeekAssignment:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         userId:
 *           type: integer
 *         weekStart:
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
 *             role:
 *               type: string
 *             category:
 *               type: string
 */

/**
 * @openapi
 * /week-assignments:
 *   get:
 *     summary: Listar os funcionários atribuídos a uma semana
 *     tags: [Atribuições Semanais]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: week
 *         required: true
 *         schema:
 *           type: string
 *           format: date
 *         description: Qualquer data dentro da semana pretendida (YYYY-MM-DD)
 *     responses:
 *       200:
 *         description: Lista de atribuições da semana
 *       400:
 *         description: Parâmetro week obrigatório
 *       401:
 *         description: Não autenticado
 */
// GET /week-assignments?week=YYYY-MM-DD
router.get('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { week } = req.query
    if (!week || typeof week !== 'string') {
      return res.status(400).json({ message: 'Query param week is required (YYYY-MM-DD)' })
    }
    const weekStart = getWeekStart(week)
    const assignments = await prisma.weekAssignment.findMany({
      where: { weekStart },
      include: {
        user: { select: { id: true, name: true, employeeNumber: true, role: true, category: true } }
      }
    })
    res.json(assignments)
  } catch (err) {
    next(err)
  }
})

/**
 * @openapi
 * /week-assignments:
 *   post:
 *     summary: Atribuir um funcionário a uma semana (Admin/Gestor)
 *     tags: [Atribuições Semanais]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required: [userId, weekStart]
 *             properties:
 *               userId:
 *                 type: integer
 *                 example: 1
 *               weekStart:
 *                 type: string
 *                 format: date
 *                 example: "2026-03-23"
 *     responses:
 *       201:
 *         description: Atribuição criada
 *       400:
 *         description: userId e weekStart obrigatórios
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN ou MANAGER)
 *       404:
 *         description: Utilizador não encontrado
 */
// POST /week-assignments  { userId, weekStart: "YYYY-MM-DD" }
router.post('/', requireRole('ADMIN', 'MANAGER'), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { userId, weekStart: weekStartStr } = req.body
    if (!userId || !weekStartStr) {
      return res.status(400).json({ message: 'userId and weekStart are required' })
    }
    const user = await prisma.user.findUnique({ where: { id: Number(userId) } })
    if (!user) return res.status(404).json({ message: 'User not found' })

    const weekStart = getWeekStart(weekStartStr)
    const assignment = await prisma.weekAssignment.upsert({
      where: { userId_weekStart: { userId: Number(userId), weekStart } },
      update: {},
      create: { userId: Number(userId), weekStart },
      include: {
        user: { select: { id: true, name: true, employeeNumber: true, role: true, category: true } }
      }
    })
    res.status(201).json(assignment)
  } catch (err) {
    next(err)
  }
})

/**
 * @openapi
 * /week-assignments/{id}:
 *   delete:
 *     summary: Remover uma atribuição semanal (Admin/Gestor)
 *     description: Apaga também os turnos desse funcionário nessa semana.
 *     tags: [Atribuições Semanais]
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
 *         description: Atribuição removida
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão (requer ADMIN ou MANAGER)
 *       404:
 *         description: Atribuição não encontrada
 */
// DELETE /week-assignments/:id  (também apaga os turnos do utilizador nessa semana)
router.delete('/:id', requireRole('ADMIN', 'MANAGER'), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id)
    const assignment = await prisma.weekAssignment.findUnique({ where: { id } })
    if (!assignment) return res.status(404).json({ message: 'Assignment not found' })

    const weekEnd = new Date(assignment.weekStart)
    weekEnd.setUTCDate(weekEnd.getUTCDate() + 6)
    weekEnd.setUTCHours(23, 59, 59, 999)

    await prisma.shift.deleteMany({
      where: { userId: assignment.userId, date: { gte: assignment.weekStart, lte: weekEnd } }
    })
    await prisma.weekAssignment.delete({ where: { id } })
    res.status(204).send()
  } catch (err) {
    next(err)
  }
})

export default router
