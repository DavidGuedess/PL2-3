import request from 'supertest'
import app from '../app'
import { PrismaClient } from '@prisma/client'

jest.mock('@prisma/client', () => {
  const mockInstance = {
    shiftSwapRequest: {
      findMany:   jest.fn(),
      create:     jest.fn(),
      findUnique: jest.fn(),
      update:     jest.fn(),
      delete:     jest.fn(),
    },
    shift: {
      findUnique: jest.fn(),
      update:     jest.fn(),
    },
    user: {
      findUnique: jest.fn(),
    },
    $transaction: jest.fn(),
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

let mockSwapFindMany:   jest.Mock
let mockSwapCreate:     jest.Mock
let mockSwapFindUnique: jest.Mock
let mockSwapUpdate:     jest.Mock
let mockSwapDelete:     jest.Mock
let mockShiftFindUnique: jest.Mock
let mockUserFindUnique:  jest.Mock
let mockTransaction:     jest.Mock

beforeAll(() => {
  const p = (PrismaClient as jest.Mock).mock.results[0]?.value
  mockSwapFindMany    = p?.shiftSwapRequest?.findMany
  mockSwapCreate      = p?.shiftSwapRequest?.create
  mockSwapFindUnique  = p?.shiftSwapRequest?.findUnique
  mockSwapUpdate      = p?.shiftSwapRequest?.update
  mockSwapDelete      = p?.shiftSwapRequest?.delete
  mockShiftFindUnique = p?.shift?.findUnique
  mockUserFindUnique  = p?.user?.findUnique
  mockTransaction     = p?.$transaction
})

const shiftInclude = {
  shiftType: { id: 1, name: 'Manhã', startTime: '08:00', endTime: '16:00', color: '#fff' },
  user: { id: 1, name: 'Test User', employeeNumber: 'EMP001', role: 'EMPLOYEE', category: 'NURSE' }
}

const mockShiftA = { id: 1, userId: 1, date: new Date('2026-05-26'), published: true, ...shiftInclude }
const mockShiftB = { id: 2, userId: 2, date: new Date('2026-05-27'), published: true, ...shiftInclude }

const mockSwapRequest = {
  id: 1, requesterId: 1, requesterShiftId: 1, targetShiftId: 2,
  reason: null, status: 'PENDING', targetAccepted: null, approvedByName: null,
  createdAt: new Date(), updatedAt: new Date(),
  requester: { id: 1, name: 'Test User', employeeNumber: 'EMP001' },
  requesterShift: mockShiftA,
  targetShift: mockShiftB,
}

beforeEach(() => { jest.clearAllMocks() })

describe('Shift Swap Requests API', () => {

  describe('GET /shift-swap-requests', () => {

    it('employee sees own requests with OR filter', async () => {
      mockSwapFindMany.mockResolvedValue([mockSwapRequest])

      const res = await request(app)
        .get('/shift-swap-requests')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(200)
      expect(Array.isArray(res.body)).toBe(true)
      const call = mockSwapFindMany.mock.calls[0][0]
      expect(call.where.OR).toBeDefined()
    })

    it('admin sees all requests (no OR filter)', async () => {
      mockSwapFindMany.mockResolvedValue([mockSwapRequest])

      const res = await request(app)
        .get('/shift-swap-requests')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
      const call = mockSwapFindMany.mock.calls[0][0]
      expect(call.where.OR).toBeUndefined()
    })

    it('admin can filter by ?userId', async () => {
      mockSwapFindMany.mockResolvedValue([])

      const res = await request(app)
        .get('/shift-swap-requests?userId=2')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
      const call = mockSwapFindMany.mock.calls[0][0]
      expect(call.where.OR).toBeDefined()
    })

  })

  describe('POST /shift-swap-requests', () => {

    it('should create a swap request', async () => {
      mockShiftFindUnique.mockResolvedValueOnce(mockShiftA).mockResolvedValueOnce(mockShiftB)
      mockSwapCreate.mockResolvedValue(mockSwapRequest)

      const res = await request(app)
        .post('/shift-swap-requests')
        .set('x-user-id', '1')
        .send({ requesterShiftId: 1, targetShiftId: 2 })

      expect(res.status).toBe(201)
      expect(res.body.status).toBe('PENDING')
    })

    it('should return 404 if requester shift not found', async () => {
      mockShiftFindUnique.mockResolvedValueOnce(null)

      const res = await request(app)
        .post('/shift-swap-requests')
        .set('x-user-id', '1')
        .send({ requesterShiftId: 99, targetShiftId: 2 })

      expect(res.status).toBe(404)
    })

    it('should return 403 if trying to swap someone elses shift', async () => {
      mockShiftFindUnique.mockResolvedValueOnce({ ...mockShiftA, userId: 99 })

      const res = await request(app)
        .post('/shift-swap-requests')
        .set('x-user-id', '1')
        .send({ requesterShiftId: 1, targetShiftId: 2 })

      expect(res.status).toBe(403)
    })

    it('should return 400 if requester shift is not published', async () => {
      mockShiftFindUnique.mockResolvedValueOnce({ ...mockShiftA, published: false })

      const res = await request(app)
        .post('/shift-swap-requests')
        .set('x-user-id', '1')
        .send({ requesterShiftId: 1, targetShiftId: 2 })

      expect(res.status).toBe(400)
    })

    it('should return 404 if target shift not found', async () => {
      mockShiftFindUnique.mockResolvedValueOnce(mockShiftA).mockResolvedValueOnce(null)

      const res = await request(app)
        .post('/shift-swap-requests')
        .set('x-user-id', '1')
        .send({ requesterShiftId: 1, targetShiftId: 99 })

      expect(res.status).toBe(404)
    })

    it('should return 400 if target shift is not published', async () => {
      mockShiftFindUnique.mockResolvedValueOnce(mockShiftA).mockResolvedValueOnce({ ...mockShiftB, published: false })

      const res = await request(app)
        .post('/shift-swap-requests')
        .set('x-user-id', '1')
        .send({ requesterShiftId: 1, targetShiftId: 2 })

      expect(res.status).toBe(400)
    })

    it('should fail with missing required fields', async () => {
      const res = await request(app)
        .post('/shift-swap-requests')
        .set('x-user-id', '1')
        .send({ requesterShiftId: 1 })

      expect(res.status).toBe(400)
    })

  })

  describe('PATCH /shift-swap-requests/:id/target-response', () => {

    it('target shift owner can accept', async () => {
      mockSwapFindUnique.mockResolvedValue({ ...mockSwapRequest, targetShift: { ...mockShiftB, userId: 1 } })
      mockSwapUpdate.mockResolvedValue({ ...mockSwapRequest, targetAccepted: true })

      const res = await request(app)
        .patch('/shift-swap-requests/1/target-response')
        .set('x-user-id', '1')
        .send({ accept: true })

      expect(res.status).toBe(200)
    })

    it('target shift owner can reject', async () => {
      mockSwapFindUnique.mockResolvedValue({ ...mockSwapRequest, targetShift: { ...mockShiftB, userId: 1 } })
      mockSwapUpdate.mockResolvedValue({ ...mockSwapRequest, targetAccepted: false, status: 'REJECTED' })

      const res = await request(app)
        .patch('/shift-swap-requests/1/target-response')
        .set('x-user-id', '1')
        .send({ accept: false })

      expect(res.status).toBe(200)
    })

    it('should return 400 if accept is not boolean', async () => {
      const res = await request(app)
        .patch('/shift-swap-requests/1/target-response')
        .set('x-user-id', '1')
        .send({ accept: 'yes' })

      expect(res.status).toBe(400)
    })

    it('should return 403 if user is not the target shift owner', async () => {
      mockSwapFindUnique.mockResolvedValue({ ...mockSwapRequest, targetShift: { ...mockShiftB, userId: 99 } })

      const res = await request(app)
        .patch('/shift-swap-requests/1/target-response')
        .set('x-user-id', '1')
        .send({ accept: true })

      expect(res.status).toBe(403)
    })

    it('should return 409 if already responded', async () => {
      mockSwapFindUnique.mockResolvedValue({
        ...mockSwapRequest, targetAccepted: true,
        targetShift: { ...mockShiftB, userId: 1 }
      })

      const res = await request(app)
        .patch('/shift-swap-requests/1/target-response')
        .set('x-user-id', '1')
        .send({ accept: true })

      expect(res.status).toBe(409)
    })

    it('should return 404 if not found', async () => {
      mockSwapFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .patch('/shift-swap-requests/999/target-response')
        .set('x-user-id', '1')
        .send({ accept: true })

      expect(res.status).toBe(404)
    })

  })

  describe('PATCH /shift-swap-requests/:id/status', () => {

    it('admin can approve after target accepted', async () => {
      mockSwapFindUnique.mockResolvedValue({ ...mockSwapRequest, targetAccepted: true })
      mockTransaction.mockResolvedValue([])
      mockUserFindUnique.mockResolvedValue({ id: 1, name: 'Admin' })
      mockSwapUpdate.mockResolvedValue({ ...mockSwapRequest, status: 'APPROVED' })

      const res = await request(app)
        .patch('/shift-swap-requests/1/status')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ status: 'APPROVED' })

      expect(res.status).toBe(200)
    })

    it('admin can reject', async () => {
      mockSwapFindUnique.mockResolvedValue({ ...mockSwapRequest, targetAccepted: true })
      mockTransaction.mockResolvedValue([])
      mockUserFindUnique.mockResolvedValue({ id: 1, name: 'Admin' })
      mockSwapUpdate.mockResolvedValue({ ...mockSwapRequest, status: 'REJECTED' })

      const res = await request(app)
        .patch('/shift-swap-requests/1/status')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ status: 'REJECTED' })

      expect(res.status).toBe(200)
    })

    it('employee cannot change status', async () => {
      const res = await request(app)
        .patch('/shift-swap-requests/1/status')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')
        .send({ status: 'APPROVED' })

      expect(res.status).toBe(403)
    })

    it('should return 404 if not found', async () => {
      mockSwapFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .patch('/shift-swap-requests/999/status')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ status: 'APPROVED' })

      expect(res.status).toBe(404)
    })

    it('should return 409 if request already processed', async () => {
      mockSwapFindUnique.mockResolvedValue({ ...mockSwapRequest, status: 'APPROVED', targetAccepted: true })

      const res = await request(app)
        .patch('/shift-swap-requests/1/status')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ status: 'APPROVED' })

      expect(res.status).toBe(409)
    })

    it('should return 409 if target has not accepted yet', async () => {
      mockSwapFindUnique.mockResolvedValue({ ...mockSwapRequest, targetAccepted: null })

      const res = await request(app)
        .patch('/shift-swap-requests/1/status')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ status: 'APPROVED' })

      expect(res.status).toBe(409)
    })

  })

  describe('DELETE /shift-swap-requests/:id', () => {

    it('owner can delete own PENDING request', async () => {
      mockSwapFindUnique.mockResolvedValue(mockSwapRequest)
      mockSwapDelete.mockResolvedValue({})

      const res = await request(app)
        .delete('/shift-swap-requests/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(204)
    })

    it('employee cannot delete own already processed request', async () => {
      mockSwapFindUnique.mockResolvedValue({ ...mockSwapRequest, status: 'APPROVED' })

      const res = await request(app)
        .delete('/shift-swap-requests/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(409)
    })

    it('non-owner employee cannot delete', async () => {
      mockSwapFindUnique.mockResolvedValue({ ...mockSwapRequest, requesterId: 99 })

      const res = await request(app)
        .delete('/shift-swap-requests/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })

    it('admin can delete any request', async () => {
      mockSwapFindUnique.mockResolvedValue({ ...mockSwapRequest, requesterId: 99, status: 'APPROVED' })
      mockSwapDelete.mockResolvedValue({})

      const res = await request(app)
        .delete('/shift-swap-requests/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(204)
    })

    it('should return 404 if not found', async () => {
      mockSwapFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .delete('/shift-swap-requests/999')
        .set('x-user-id', '1')

      expect(res.status).toBe(404)
    })

  })

})
