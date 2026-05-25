import request from 'supertest'
import app from '../app'
import { PrismaClient } from '@prisma/client'

jest.mock('@prisma/client', () => {
  const mockInstance = {
    weekAssignment: {
      findMany:   jest.fn(),
      upsert:     jest.fn(),
      findUnique: jest.fn(),
      delete:     jest.fn(),
    },
    shift: {
      deleteMany: jest.fn(),
    },
    user: {
      findUnique: jest.fn(),
    }
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

let mockWAFindMany:   jest.Mock
let mockWAUpsert:     jest.Mock
let mockWAFindUnique: jest.Mock
let mockWADelete:     jest.Mock
let mockShiftDeleteMany: jest.Mock
let mockUserFindUnique:  jest.Mock

beforeAll(() => {
  const p = (PrismaClient as jest.Mock).mock.results[0]?.value
  mockWAFindMany      = p?.weekAssignment?.findMany
  mockWAUpsert        = p?.weekAssignment?.upsert
  mockWAFindUnique    = p?.weekAssignment?.findUnique
  mockWADelete        = p?.weekAssignment?.delete
  mockShiftDeleteMany = p?.shift?.deleteMany
  mockUserFindUnique  = p?.user?.findUnique
})

const mockUser = { id: 1, name: 'Test User', employeeNumber: 'EMP001', role: 'EMPLOYEE', category: 'NURSE' }

const mockAssignment = {
  id: 1, userId: 1,
  weekStart: new Date('2026-05-25T00:00:00.000Z'),
  user: mockUser
}

beforeEach(() => { jest.clearAllMocks() })

describe('Week Assignments API', () => {

  describe('GET /week-assignments', () => {

    it('should return assignments for the given week', async () => {
      mockWAFindMany.mockResolvedValue([mockAssignment])

      const res = await request(app)
        .get('/week-assignments?week=2026-05-26')
        .set('x-user-id', '1')

      expect(res.status).toBe(200)
      expect(Array.isArray(res.body)).toBe(true)
      expect(res.body.length).toBe(1)
    })

    it('should return empty array when no assignments for that week', async () => {
      mockWAFindMany.mockResolvedValue([])

      const res = await request(app)
        .get('/week-assignments?week=2026-06-01')
        .set('x-user-id', '1')

      expect(res.status).toBe(200)
      expect(res.body).toEqual([])
    })

    it('should return 400 if week param is missing', async () => {
      const res = await request(app)
        .get('/week-assignments')
        .set('x-user-id', '1')

      expect(res.status).toBe(400)
    })

    it('normalises week to monday of that week', async () => {
      mockWAFindMany.mockResolvedValue([])

      await request(app)
        .get('/week-assignments?week=2026-05-28')
        .set('x-user-id', '1')

      const call = mockWAFindMany.mock.calls[0][0]
      const weekStart: Date = call.where.weekStart
      expect(weekStart.getUTCDay()).toBe(1)
    })

  })

  describe('POST /week-assignments', () => {

    it('admin can create an assignment', async () => {
      mockUserFindUnique.mockResolvedValue(mockUser)
      mockWAUpsert.mockResolvedValue(mockAssignment)

      const res = await request(app)
        .post('/week-assignments')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ userId: 1, weekStart: '2026-05-25' })

      expect(res.status).toBe(201)
      expect(res.body.userId).toBe(1)
    })

    it('manager can create an assignment', async () => {
      mockUserFindUnique.mockResolvedValue(mockUser)
      mockWAUpsert.mockResolvedValue(mockAssignment)

      const res = await request(app)
        .post('/week-assignments')
        .set('x-user-id', '1')
        .set('x-user-role', 'MANAGER')
        .send({ userId: 1, weekStart: '2026-05-25' })

      expect(res.status).toBe(201)
    })

    it('employee cannot create an assignment', async () => {
      const res = await request(app)
        .post('/week-assignments')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')
        .send({ userId: 1, weekStart: '2026-05-25' })

      expect(res.status).toBe(403)
    })

    it('should return 400 if userId is missing', async () => {
      const res = await request(app)
        .post('/week-assignments')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ weekStart: '2026-05-25' })

      expect(res.status).toBe(400)
    })

    it('should return 400 if weekStart is missing', async () => {
      const res = await request(app)
        .post('/week-assignments')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ userId: 1 })

      expect(res.status).toBe(400)
    })

    it('should return 404 if user does not exist', async () => {
      mockUserFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .post('/week-assignments')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ userId: 999, weekStart: '2026-05-25' })

      expect(res.status).toBe(404)
    })

    it('upsert is idempotent for duplicate assignment', async () => {
      mockUserFindUnique.mockResolvedValue(mockUser)
      mockWAUpsert.mockResolvedValue(mockAssignment)

      const res = await request(app)
        .post('/week-assignments')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ userId: 1, weekStart: '2026-05-25' })

      expect(res.status).toBe(201)
      expect(mockWAUpsert).toHaveBeenCalledTimes(1)
    })

  })

  describe('DELETE /week-assignments/:id', () => {

    it('admin can delete and it cascades shifts', async () => {
      mockWAFindUnique.mockResolvedValue(mockAssignment)
      mockShiftDeleteMany.mockResolvedValue({ count: 3 })
      mockWADelete.mockResolvedValue({})

      const res = await request(app)
        .delete('/week-assignments/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(204)
      expect(mockShiftDeleteMany).toHaveBeenCalledTimes(1)
    })

    it('manager can delete an assignment', async () => {
      mockWAFindUnique.mockResolvedValue(mockAssignment)
      mockShiftDeleteMany.mockResolvedValue({ count: 0 })
      mockWADelete.mockResolvedValue({})

      const res = await request(app)
        .delete('/week-assignments/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'MANAGER')

      expect(res.status).toBe(204)
    })

    it('employee cannot delete an assignment', async () => {
      const res = await request(app)
        .delete('/week-assignments/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })

    it('should return 404 if assignment not found', async () => {
      mockWAFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .delete('/week-assignments/999')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(404)
    })

  })

})
