import request from 'supertest'
import app from '../app'
import { PrismaClient } from '@prisma/client'

jest.mock('@prisma/client', () => {
  const mockInstance = {
    timeOffRequest: {
      findMany:   jest.fn(),
      create:     jest.fn(),
      findUnique: jest.fn(),
      update:     jest.fn(),
      delete:     jest.fn(),
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

let mockFindMany:    jest.Mock
let mockCreate:      jest.Mock
let mockFindUnique:  jest.Mock
let mockUpdate:      jest.Mock
let mockDelete:      jest.Mock
let mockUserFindUnique: jest.Mock

beforeAll(() => {
  const p = (PrismaClient as jest.Mock).mock.results[0]?.value
  mockFindMany       = p?.timeOffRequest?.findMany
  mockCreate         = p?.timeOffRequest?.create
  mockFindUnique     = p?.timeOffRequest?.findUnique
  mockUpdate         = p?.timeOffRequest?.update
  mockDelete         = p?.timeOffRequest?.delete
  mockUserFindUnique = p?.user?.findUnique
})

const mockRequest = {
  id: 1, userId: 1, allDay: true, reason: null,
  status: 'PENDING', approvedByName: null,
  startDate: new Date('2026-06-01'), endDate: new Date('2026-06-05'),
  createdAt: new Date(), updatedAt: new Date(),
  user: { id: 1, name: 'Test User', employeeNumber: 'EMP001' }
}

beforeEach(() => { jest.clearAllMocks() })

describe('Time-Off Requests API', () => {

  describe('GET /time-off-requests', () => {

    it('employee sees only own requests', async () => {
      mockFindMany.mockResolvedValue([mockRequest])

      const res = await request(app)
        .get('/time-off-requests')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(200)
      expect(Array.isArray(res.body)).toBe(true)
      const call = mockFindMany.mock.calls[0][0]
      expect(call.where.userId).toBe(1)
    })

    it('admin sees all requests', async () => {
      mockFindMany.mockResolvedValue([mockRequest])

      const res = await request(app)
        .get('/time-off-requests')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
      const call = mockFindMany.mock.calls[0][0]
      expect(call.where.userId).toBeUndefined()
    })

    it('admin can filter by ?userId', async () => {
      mockFindMany.mockResolvedValue([mockRequest])

      const res = await request(app)
        .get('/time-off-requests?userId=2')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
      const call = mockFindMany.mock.calls[0][0]
      expect(call.where.userId).toBe(2)
    })

  })

  describe('POST /time-off-requests', () => {

    it('should create a time-off request', async () => {
      mockCreate.mockResolvedValue(mockRequest)

      const res = await request(app)
        .post('/time-off-requests')
        .set('x-user-id', '1')
        .send({ startDate: '2026-06-01', endDate: '2026-06-05' })

      expect(res.status).toBe(201)
      expect(res.body.status).toBe('PENDING')
    })

    it('should create with reason and allDay false', async () => {
      mockCreate.mockResolvedValue({ ...mockRequest, reason: 'Férias', allDay: false })

      const res = await request(app)
        .post('/time-off-requests')
        .set('x-user-id', '1')
        .send({ startDate: '2026-06-01', endDate: '2026-06-05', allDay: false, reason: 'Férias' })

      expect(res.status).toBe(201)
    })

    it('should fail with missing startDate', async () => {
      const res = await request(app)
        .post('/time-off-requests')
        .set('x-user-id', '1')
        .send({ endDate: '2026-06-05' })

      expect(res.status).toBe(400)
    })

    it('should fail with missing endDate', async () => {
      const res = await request(app)
        .post('/time-off-requests')
        .set('x-user-id', '1')
        .send({ startDate: '2026-06-01' })

      expect(res.status).toBe(400)
    })

  })

  describe('PATCH /time-off-requests/:id/status', () => {

    it('admin can approve a request', async () => {
      mockFindUnique.mockResolvedValue(mockRequest)
      mockUserFindUnique.mockResolvedValue({ id: 1, name: 'Admin User' })
      mockUpdate.mockResolvedValue({ ...mockRequest, status: 'APPROVED', approvedByName: 'Admin User' })

      const res = await request(app)
        .patch('/time-off-requests/1/status')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ status: 'APPROVED' })

      expect(res.status).toBe(200)
      expect(res.body.status).toBe('APPROVED')
    })

    it('manager can reject a request', async () => {
      mockFindUnique.mockResolvedValue(mockRequest)
      mockUserFindUnique.mockResolvedValue({ id: 2, name: 'Manager' })
      mockUpdate.mockResolvedValue({ ...mockRequest, status: 'REJECTED' })

      const res = await request(app)
        .patch('/time-off-requests/1/status')
        .set('x-user-id', '2')
        .set('x-user-role', 'MANAGER')
        .send({ status: 'REJECTED' })

      expect(res.status).toBe(200)
      expect(res.body.status).toBe('REJECTED')
    })

    it('employee cannot change request status', async () => {
      const res = await request(app)
        .patch('/time-off-requests/1/status')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')
        .send({ status: 'APPROVED' })

      expect(res.status).toBe(403)
    })

    it('should return 404 if request not found', async () => {
      mockFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .patch('/time-off-requests/999/status')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ status: 'APPROVED' })

      expect(res.status).toBe(404)
    })

    it('should return 400 with invalid status value', async () => {
      const res = await request(app)
        .patch('/time-off-requests/1/status')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ status: 'INVALID' })

      expect(res.status).toBe(400)
    })

  })

  describe('DELETE /time-off-requests/:id', () => {

    it('employee can delete own PENDING request', async () => {
      mockFindUnique.mockResolvedValue(mockRequest)
      mockDelete.mockResolvedValue({})

      const res = await request(app)
        .delete('/time-off-requests/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(204)
    })

    it('employee cannot delete own already processed request', async () => {
      mockFindUnique.mockResolvedValue({ ...mockRequest, status: 'APPROVED' })

      const res = await request(app)
        .delete('/time-off-requests/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(409)
    })

    it('employee cannot delete another users request', async () => {
      mockFindUnique.mockResolvedValue({ ...mockRequest, userId: 99 })

      const res = await request(app)
        .delete('/time-off-requests/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })

    it('admin can delete any request regardless of status', async () => {
      mockFindUnique.mockResolvedValue({ ...mockRequest, userId: 99, status: 'APPROVED' })
      mockDelete.mockResolvedValue({})

      const res = await request(app)
        .delete('/time-off-requests/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(204)
    })

    it('should return 404 if request not found', async () => {
      mockFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .delete('/time-off-requests/999')
        .set('x-user-id', '1')

      expect(res.status).toBe(404)
    })

  })

})
