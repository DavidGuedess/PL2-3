import { Request, Response } from 'express'
import { PrismaClient } from '@prisma/client'

const prisma = new PrismaClient()

export const createAttendance = async (req: Request, res: Response) => {
  const { type } = req.body
  const userId = req.user!.userId

  if (!type || !['IN', 'OUT'].includes(type)) {
    return res.status(400).json({ message: 'type must be IN or OUT' })
  }

  const lastRecord = await prisma.attendanceRecord.findFirst({
    where: { userId },
    orderBy: { timestamp: 'desc' }
  })

  if (!lastRecord) {
    if (type !== 'IN') {
      return res.status(400).json({ message: 'First attendance record must be IN' })
    }
  } else {
    if (lastRecord.type === type) {
      return res.status(400).json({
        message: `Invalid attendance sequence. Last record was ${lastRecord.type}, next must be ${lastRecord.type === 'IN' ? 'OUT' : 'IN'}`
      })
    }
  }

  const record = await prisma.attendanceRecord.create({
    data: {
      userId,
      type
    },
    include: {
      user: {
        select: {
          id: true,
          name: true,
          employeeNumber: true
        }
      }
    }
  })

  return res.status(201).json(record)
}

export const getMyAttendance = async (req: Request, res: Response) => {
  const userId = req.user!.userId
  const { from, to } = req.query

  const where: any = { userId }

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
    orderBy: { timestamp: 'desc' }
  })

  return res.status(200).json(records)
}