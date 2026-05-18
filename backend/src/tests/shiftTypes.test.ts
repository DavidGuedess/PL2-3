import request from 'supertest'
import app from '../app'
import { PrismaClient } from '@prisma/client'

jest.mock('@prisma/client', () => {
  const mockInstance = {
    shiftType: {
      findMany: jest.fn(),
      findUnique: jest.fn(),
      create: jest.fn(),
      update: jest.fn(),
      delete: jest.fn(),
      count: jest.fn(),
    }
  }
  return { PrismaClient: jest.fn(() => mockInstance) }
})

jest.mock('../middleware/auth', () => ({
  authenticate: (req: any, _res: any, next: any) => {
    const userId = req.headers['x-user-id']
    const role = req.headers['x-user-role'] || ''
    req.user = { userId: userId ? Number(userId) : NaN, email: '', role }
    next()
  },
  requireRole: (...roles: string[]) => (req: any, res: any, next: any) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return res.status(403).json({ error: 'Acesso não autorizado' })
    }
    next()
  }
}))

const mockShiftType = {
  id: 1,
  name: 'Manhã',
  startTime: '08:00',
  endTime: '16:00',
  createdAt: new Date(),
  updatedAt: new Date(),
}

let mockFindMany: jest.Mock
let mockFindUnique: jest.Mock
let mockCreate: jest.Mock
let mockUpdate: jest.Mock
let mockDelete: jest.Mock
let mockCount: jest.Mock

beforeAll(() => {
  const prismaInstance = (PrismaClient as jest.Mock).mock.results[0]?.value
  mockFindMany = prismaInstance?.shiftType?.findMany
  mockFindUnique = prismaInstance?.shiftType?.findUnique
  mockCreate = prismaInstance?.shiftType?.create
  mockUpdate = prismaInstance?.shiftType?.update
  mockDelete = prismaInstance?.shiftType?.delete
  mockCount = prismaInstance?.shiftType?.count
})

beforeEach(() => {
  jest.clearAllMocks()
})

describe('ShiftTypes API', () => {

  describe('GET /shift-types', () => {
    it('should return all shift types', async () => {
      mockFindMany.mockResolvedValue([mockShiftType])

      const res = await request(app)
        .get('/shift-types')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(200)
      expect(Array.isArray(res.body)).toBe(true)
      expect(res.body[0].name).toBe('Manhã')
    })

    it('should return paginated shift types when page and limit are provided', async () => {
      mockCount.mockResolvedValue(1)
      mockFindMany.mockResolvedValue([mockShiftType])

      const res = await request(app)
        .get('/shift-types?page=1&limit=10')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(200)
      expect(Array.isArray(res.body.data)).toBe(true)
      expect(res.body.total).toBe(1)
      expect(res.body.page).toBe(1)
      expect(res.body.limit).toBe(10)
      expect(res.body.totalPages).toBe(1)
    })

    it('should calculate totalPages correctly', async () => {
      mockCount.mockResolvedValue(25)
      mockFindMany.mockResolvedValue([mockShiftType])

      const res = await request(app)
        .get('/shift-types?page=1&limit=10')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(200)
      expect(res.body.totalPages).toBe(3)
    })

    it('should return 400 for page=0', async () => {
      const res = await request(app)
        .get('/shift-types?page=0&limit=10')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(400)
      expect(res.body.message).toBe('page must be a positive integer')
    })

    it('should return 400 for limit=0', async () => {
      const res = await request(app)
        .get('/shift-types?page=1&limit=0')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(400)
      expect(res.body.message).toBe('limit must be a positive integer')
    })

    it('should return 400 for non-numeric page', async () => {
      const res = await request(app)
        .get('/shift-types?page=abc&limit=10')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(400)
      expect(res.body.message).toBe('page must be a positive integer')
    })

    it('should return 400 for negative limit', async () => {
      const res = await request(app)
        .get('/shift-types?page=1&limit=-5')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(400)
      expect(res.body.message).toBe('limit must be a positive integer')
    })

    it('should return plain array when only page is provided without limit', async () => {
      mockFindMany.mockResolvedValue([mockShiftType])

      const res = await request(app)
        .get('/shift-types?page=1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(200)
      expect(Array.isArray(res.body)).toBe(true)
    })
  })

  describe('POST /shift-types', () => {
    it('should create a new shift type', async () => {
      mockFindUnique.mockResolvedValue(null)
      mockCreate.mockResolvedValue(mockShiftType)

      const res = await request(app)
        .post('/shift-types')
        .set('x-user-role', 'ADMIN')
        .send({ name: 'Manhã', startTime: '08:00', endTime: '16:00' })

      expect(res.status).toBe(201)
      expect(res.body.name).toBe('Manhã')
    })

    it('should return 400 if required fields are missing', async () => {
      const res = await request(app)
        .post('/shift-types')
        .set('x-user-role', 'ADMIN')
        .send({ name: 'Manhã' })

      expect(res.status).toBe(400)
    })

    it('should return 409 if shift type name already exists', async () => {
      mockFindUnique.mockResolvedValue(mockShiftType)

      const res = await request(app)
        .post('/shift-types')
        .set('x-user-role', 'ADMIN')
        .send({ name: 'Manhã', startTime: '08:00', endTime: '16:00' })

      expect(res.status).toBe(409)
    })

    it('should return 403 if employee tries to create shift type', async () => {
      const res = await request(app)
        .post('/shift-types')
        .set('x-user-role', 'EMPLOYEE')
        .send({ name: 'Tarde', startTime: '16:00', endTime: '00:00' })

      expect(res.status).toBe(403)
    })
  })

  describe('PATCH /shift-types/:id', () => {
    it('should update a shift type', async () => {
      mockFindUnique.mockResolvedValue(mockShiftType)
      mockUpdate.mockResolvedValue({ ...mockShiftType, name: 'Manhã Atualizada' })

      const res = await request(app)
        .patch('/shift-types/1')
        .set('x-user-role', 'ADMIN')
        .send({ name: 'Manhã Atualizada' })

      expect(res.status).toBe(200)
      expect(res.body.name).toBe('Manhã Atualizada')
    })

    it('should return 404 if shift type not found', async () => {
      mockFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .patch('/shift-types/99999')
        .set('x-user-role', 'ADMIN')
        .send({ name: 'Inexistente' })

      expect(res.status).toBe(404)
    })

    it('should return 403 if employee tries to update shift type', async () => {
      const res = await request(app)
        .patch('/shift-types/1')
        .set('x-user-role', 'EMPLOYEE')
        .send({ name: 'Tentativa' })

      expect(res.status).toBe(403)
    })
  })

  describe('DELETE /shift-types/:id', () => {
    it('should delete a shift type', async () => {
      mockFindUnique.mockResolvedValue(mockShiftType)
      mockDelete.mockResolvedValue(mockShiftType)

      const res = await request(app)
        .delete('/shift-types/1')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(204)
    })

    it('should return 404 if shift type not found', async () => {
      mockFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .delete('/shift-types/99999')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(404)
    })

    it('should return 403 if employee tries to delete shift type', async () => {
      const res = await request(app)
        .delete('/shift-types/1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })
  })
})
