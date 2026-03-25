import { Router, Request, Response } from 'express'
import { PrismaClient } from '@prisma/client'

const router = Router()
const prisma = new PrismaClient()

router.get('/', async (_req: Request, res: Response) => {
  const shiftTypes = await prisma.shiftType.findMany({ orderBy: { name: 'asc' } })
  res.json(shiftTypes)
})

router.post('/', async (req: Request, res: Response) => {
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

router.patch('/:id', async (req: Request, res: Response) => {
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

router.delete('/:id', async (req: Request, res: Response) => {
  const id = parseInt(req.params.id)

  const existing = await prisma.shiftType.findUnique({ where: { id } })
  if (!existing) {
    return res.status(404).json({ message: 'Shift type not found' })
  }

  await prisma.shiftType.delete({ where: { id } })
  res.status(204).send()
})

export default router
