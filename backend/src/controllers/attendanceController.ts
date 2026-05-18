import { Request, Response, NextFunction } from 'express'
import { PrismaClient } from '@prisma/client'
import { calculateAttendanceStats } from '../utils/attendanceStats'

const prisma = new PrismaClient()

export const getAttendanceReport = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { from, to, userId } = req.query

    let userIdNum: number | undefined

    if (userId !== undefined) {
      userIdNum = Number(userId)
      if (!Number.isInteger(userIdNum) || userIdNum <= 0) {
        return res.status(400).json({ message: 'userId must be a valid integer' })
      }
      const user = await prisma.user.findUnique({ where: { id: userIdNum } })
      if (!user) {
        return res.status(404).json({ message: 'User not found' })
      }
    }

    const shiftWhere: any = {}
    if (userIdNum) shiftWhere.userId = userIdNum
    if (from || to) {
      shiftWhere.date = {}
      if (from) shiftWhere.date.gte = new Date(from as string)
      if (to) shiftWhere.date.lte = new Date(to as string)
    }

    const attendanceWhere: any = { type: 'IN' }
    if (userIdNum) attendanceWhere.userId = userIdNum
    if (from || to) {
      attendanceWhere.timestamp = {}
      if (from) attendanceWhere.timestamp.gte = new Date(from as string)
      if (to) {
        const toDate = new Date(to as string)
        toDate.setUTCHours(23, 59, 59, 999)
        attendanceWhere.timestamp.lte = toDate
      }
    }

    const [shifts, attendanceRecords] = await Promise.all([
      prisma.shift.findMany({
        where: shiftWhere,
        include: { user: { select: { id: true, name: true, employeeNumber: true } } }
      }),
      prisma.attendanceRecord.findMany({ where: attendanceWhere })
    ])

    const userShiftsMap = new Map<number, {
      user: { id: number; name: string; employeeNumber: string }
      scheduledDates: Set<string>
    }>()
    for (const shift of shifts) {
      const uid = shift.userId
      if (!userShiftsMap.has(uid)) {
        userShiftsMap.set(uid, { user: shift.user, scheduledDates: new Set() })
      }
      const dateKey = new Date(shift.date).toISOString().split('T')[0]
      userShiftsMap.get(uid)!.scheduledDates.add(dateKey)
    }

    const attendanceDatesByUser = new Map<number, Set<string>>()
    for (const record of attendanceRecords) {
      const uid = record.userId
      if (!attendanceDatesByUser.has(uid)) {
        attendanceDatesByUser.set(uid, new Set())
      }
      const dateKey = new Date(record.timestamp).toISOString().split('T')[0]
      attendanceDatesByUser.get(uid)!.add(dateKey)
    }

    const report = Array.from(userShiftsMap.entries()).map(([uid, { user, scheduledDates }]) => {
      const attendedDates = attendanceDatesByUser.get(uid) ?? new Set<string>()
      let daysPresent = 0
      let daysAbsent = 0
      for (const date of scheduledDates) {
        if (attendedDates.has(date)) {
          daysPresent++
        } else {
          daysAbsent++
        }
      }
      return {
        userId: uid,
        name: user.name,
        employeeNumber: user.employeeNumber,
        totalScheduledDays: scheduledDates.size,
        daysPresent,
        daysAbsent
      }
    })

    return res.status(200).json(report)
  } catch (err) {
    next(err)
  }
}

export const createAttendance = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { type } = req.body
    const userId = req.user!.userId

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
  } catch (err) {
    next(err)
  }
}

export const getAttendanceStats = async (req: Request, res: Response, next: NextFunction) => {
  try {
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

    const recordsWhere: any = { userId: userIdNum }
    const shiftsWhere: any = { userId: userIdNum }

    if (from || to) {
      recordsWhere.timestamp = {}
      shiftsWhere.date = {}
      if (from) {
        recordsWhere.timestamp.gte = new Date(from as string)
        shiftsWhere.date.gte = new Date(from as string)
      }
      if (to) {
        const toDate = new Date(to as string)
        toDate.setUTCHours(23, 59, 59, 999)
        recordsWhere.timestamp.lte = toDate
        shiftsWhere.date.lte = new Date(to as string)
      }
    }

    const [records, shifts] = await Promise.all([
      prisma.attendanceRecord.findMany({
        where: recordsWhere,
        orderBy: { timestamp: 'asc' }
      }),
      prisma.shift.findMany({
        where: shiftsWhere,
        include: { shiftType: true }
      })
    ])

    const stats = calculateAttendanceStats(records, shifts)

    return res.status(200).json({
      userId: userIdNum,
      name: user.name,
      employeeNumber: user.employeeNumber,
      ...stats
    })
  } catch (err) {
    next(err)
  }
}

export const getMyAttendance = async (req: Request, res: Response, next: NextFunction) => {
  try {
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
  } catch (err) {
    next(err)
  }
}
