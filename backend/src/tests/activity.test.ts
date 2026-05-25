import request from 'supertest'
import app from '../app'
import { PrismaClient } from '@prisma/client'

jest.mock('@prisma/client', () => {
  const mockInstance = {
    attendanceRecord: { findMany: jest.fn() },
    timeOffRequest:   { findMany: jest.fn() },
    shiftSwapRequest: { findMany: jest.fn() },
  }
  return { PrismaClient: jest.fn(() => mockInstance) }
})

jest.mock('../middleware/auth', () => ({
  authenticate: (req: any, _res: any, next: any) => {
    const userId = req.headers['x-user-id']
    const role   = req.headers['x-user-role'] || 'EMPLOYEE'
    req.user = { userId: userId ? Number(userId) : 1, email: '', role }
    next()
  },
  requireRole: (...roles: string[]) => (req: any, res: any, next: any) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return res.status(403).json({ error: 'Acesso não autorizado' })
    }
    next()
  }
}))

let mockAttendanceFindMany: jest.Mock
let mockTimeOffFindMany: jest.Mock
let mockSwapFindMany: jest.Mock

beforeAll(() => {
  const p = (PrismaClient as jest.Mock).mock.results[0]?.value
  mockAttendanceFindMany = p?.attendanceRecord?.findMany
  mockTimeOffFindMany   = p?.timeOffRequest?.findMany
  mockSwapFindMany      = p?.shiftSwapRequest?.findMany
})

const mockAttendance = {
  id: 1, userId: 1, type: 'IN', note: null,
  timestamp: new Date('2026-05-25T09:00:00Z'),
  user: { id: 1, name: 'Test User', employeeNumber: 'EMP001' }
}

const mockTimeOff = {
  id: 1, userId: 1, status: 'PENDING', approvedByName: null, reason: null,
  startDate: new Date('2026-06-01'), endDate: new Date('2026-06-05'),
  createdAt: new Date(), updatedAt: new Date(),
  user: { id: 1, name: 'Test User', employeeNumber: 'EMP001' }
}

const mockSwap = {
  id: 1, requesterId: 1, status: 'PENDING', targetAccepted: null, approvedByName: null, reason: null,
  createdAt: new Date(), updatedAt: new Date(),
  requester: { id: 1, name: 'Test User', employeeNumber: 'EMP001' },
  requesterShift: {
    id: 1, date: new Date('2026-05-26'),
    user: { id: 1, name: 'Test User', employeeNumber: 'EMP001' }
  },
  targetShift: {
    id: 2, date: new Date('2026-05-27'),
    user: { id: 2, name: 'Other User', employeeNumber: 'EMP002' }
  }
}

beforeEach(() => {
  jest.clearAllMocks()
  mockAttendanceFindMany.mockResolvedValue([])
  mockTimeOffFindMany.mockResolvedValue([])
  mockSwapFindMany.mockResolvedValue([])
})

describe('Activity API', () => {

  describe('GET /activity', () => {

    it('should return all activity types merged and sorted', async () => {
      mockAttendanceFindMany.mockResolvedValue([mockAttendance])
      mockTimeOffFindMany.mockResolvedValue([mockTimeOff])
      mockSwapFindMany.mockResolvedValue([mockSwap])

      const res = await request(app).get('/activity').set('x-user-id', '1').set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(200)
      expect(Array.isArray(res.body)).toBe(true)
      expect(res.body.length).toBe(3)
      const types = res.body.map((e: any) => e.type)
      expect(types).toContain('attendance')
      expect(types).toContain('timeoff')
      expect(types).toContain('swap')
    })

    it('should filter type=attendance', async () => {
      mockAttendanceFindMany.mockResolvedValue([mockAttendance])

      const res = await request(app).get('/activity?type=attendance').set('x-user-id', '1')

      expect(res.status).toBe(200)
      expect(res.body.every((e: any) => e.type === 'attendance')).toBe(true)
      expect(mockTimeOffFindMany).not.toHaveBeenCalled()
      expect(mockSwapFindMany).not.toHaveBeenCalled()
    })

    it('should filter type=timeoff', async () => {
      mockTimeOffFindMany.mockResolvedValue([mockTimeOff])

      const res = await request(app).get('/activity?type=timeoff').set('x-user-id', '1')

      expect(res.status).toBe(200)
      expect(res.body.every((e: any) => e.type === 'timeoff')).toBe(true)
      expect(mockAttendanceFindMany).not.toHaveBeenCalled()
    })

    it('should filter type=swap', async () => {
      mockSwapFindMany.mockResolvedValue([mockSwap])

      const res = await request(app).get('/activity?type=swap').set('x-user-id', '1')

      expect(res.status).toBe(200)
      expect(res.body.every((e: any) => e.type === 'swap')).toBe(true)
    })

    it('should return correct action labels for attendance', async () => {
      mockAttendanceFindMany.mockResolvedValue([
        mockAttendance,
        { ...mockAttendance, id: 2, type: 'OUT' }
      ])

      const res = await request(app).get('/activity?type=attendance').set('x-user-id', '1')

      expect(res.status).toBe(200)
      const actions = res.body.map((e: any) => e.action)
      expect(actions).toContain('Entrada registada')
      expect(actions).toContain('Saída registada')
    })

    it('admin can filter by ?userId', async () => {
      mockAttendanceFindMany.mockResolvedValue([mockAttendance])

      const res = await request(app)
        .get('/activity?userId=2')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
      const call = mockAttendanceFindMany.mock.calls[0][0]
      expect(call.where.userId).toBe(2)
    })

    it('employee always queries own userId regardless of ?userId param', async () => {
      const res = await request(app)
        .get('/activity?userId=99')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(200)
      const call = mockAttendanceFindMany.mock.calls[0][0]
      expect(call.where.userId).toBe(1)
    })

    it('should return empty array when no activity exists', async () => {
      const res = await request(app).get('/activity').set('x-user-id', '1')

      expect(res.status).toBe(200)
      expect(res.body).toEqual([])
    })

  })

})
