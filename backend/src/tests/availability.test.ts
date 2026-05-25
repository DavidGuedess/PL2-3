import request from 'supertest'
import app from '../app'
import { PrismaClient } from '@prisma/client'

jest.mock('@prisma/client', () => {
  const mockInstance = {
    availability: {
      findMany:   jest.fn(),
      upsert:     jest.fn(),
      findUnique: jest.fn(),
      delete:     jest.fn(),
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

let mockFindMany:   jest.Mock
let mockUpsert:     jest.Mock
let mockFindUnique: jest.Mock
let mockDelete:     jest.Mock

beforeAll(() => {
  const p = (PrismaClient as jest.Mock).mock.results[0]?.value
  mockFindMany   = p?.availability?.findMany
  mockUpsert     = p?.availability?.upsert
  mockFindUnique = p?.availability?.findUnique
  mockDelete     = p?.availability?.delete
})

const mockAvailability = {
  id: 1, userId: 1,
  date: new Date('2026-06-01'),
  type: 'PREFERRED',
  note: null,
  user: { id: 1, name: 'Test User', employeeNumber: 'EMP001' }
}

beforeEach(() => { jest.clearAllMocks() })

describe('Availability API', () => {

  describe('GET /availability', () => {

    it('employee sees only own availability', async () => {
      mockFindMany.mockResolvedValue([mockAvailability])

      const res = await request(app)
        .get('/availability')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(200)
      expect(Array.isArray(res.body)).toBe(true)
      const call = mockFindMany.mock.calls[0][0]
      expect(call.where.userId).toBe(1)
    })

    it('admin sees all availability (no userId filter)', async () => {
      mockFindMany.mockResolvedValue([mockAvailability])

      const res = await request(app)
        .get('/availability')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
      const call = mockFindMany.mock.calls[0][0]
      expect(call.where.userId).toBeUndefined()
    })

    it('admin can filter by ?userId', async () => {
      mockFindMany.mockResolvedValue([mockAvailability])

      const res = await request(app)
        .get('/availability?userId=2')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
      const call = mockFindMany.mock.calls[0][0]
      expect(call.where.userId).toBe(2)
    })

    it('filters by startDate and endDate', async () => {
      mockFindMany.mockResolvedValue([])

      const res = await request(app)
        .get('/availability?startDate=2026-06-01&endDate=2026-06-30')
        .set('x-user-id', '1')

      expect(res.status).toBe(200)
      const call = mockFindMany.mock.calls[0][0]
      expect(call.where.date).toBeDefined()
    })

  })

  describe('POST /availability', () => {

    it('should create availability with type PREFERRED', async () => {
      mockUpsert.mockResolvedValue(mockAvailability)

      const res = await request(app)
        .post('/availability')
        .set('x-user-id', '1')
        .send({ date: '2026-06-01', type: 'PREFERRED' })

      expect(res.status).toBe(201)
      expect(res.body.type).toBe('PREFERRED')
    })

    it('should create availability with type UNAVAILABLE and note', async () => {
      mockUpsert.mockResolvedValue({ ...mockAvailability, type: 'UNAVAILABLE', note: 'Consulta' })

      const res = await request(app)
        .post('/availability')
        .set('x-user-id', '1')
        .send({ date: '2026-06-02', type: 'UNAVAILABLE', note: 'Consulta' })

      expect(res.status).toBe(201)
    })

    it('should fail with missing date', async () => {
      const res = await request(app)
        .post('/availability')
        .set('x-user-id', '1')
        .send({ type: 'PREFERRED' })

      expect(res.status).toBe(400)
    })

    it('should fail with missing type', async () => {
      const res = await request(app)
        .post('/availability')
        .set('x-user-id', '1')
        .send({ date: '2026-06-01' })

      expect(res.status).toBe(400)
    })

    it('should fail with invalid type', async () => {
      const res = await request(app)
        .post('/availability')
        .set('x-user-id', '1')
        .send({ date: '2026-06-01', type: 'INVALID' })

      expect(res.status).toBe(400)
    })

  })

  describe('DELETE /availability/:id', () => {

    it('should delete own availability record', async () => {
      mockFindUnique.mockResolvedValue(mockAvailability)
      mockDelete.mockResolvedValue(mockAvailability)

      const res = await request(app)
        .delete('/availability/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(204)
    })

    it('admin can delete another users availability', async () => {
      mockFindUnique.mockResolvedValue({ ...mockAvailability, userId: 2 })
      mockDelete.mockResolvedValue({})

      const res = await request(app)
        .delete('/availability/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(204)
    })

    it('employee cannot delete another users availability', async () => {
      mockFindUnique.mockResolvedValue({ ...mockAvailability, userId: 99 })

      const res = await request(app)
        .delete('/availability/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })

    it('should return 404 if not found', async () => {
      mockFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .delete('/availability/999')
        .set('x-user-id', '1')

      expect(res.status).toBe(404)
    })

  })

})
