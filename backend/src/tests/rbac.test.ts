import request from 'supertest'
import app from '../app'
import { users } from '../data/users'

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

jest.mock('@prisma/client', () => {
  const mockInstance = {
    shiftType: {
      findMany: jest.fn().mockResolvedValue([]),
      findUnique: jest.fn().mockResolvedValue(null),
      create: jest.fn(),
      update: jest.fn(),
      delete: jest.fn()
    },
    shift: {
      findMany: jest.fn().mockResolvedValue([]),
      findUnique: jest.fn().mockResolvedValue(null),
      create: jest.fn()
    },
    user: {
      findUnique: jest.fn().mockResolvedValue(null)
    }
  }
  return { PrismaClient: jest.fn(() => mockInstance) }
})

describe('RBAC - Controlo de acesso por papel', () => {

  beforeEach(() => {
    users.length = 0
  })

  // ===========================
  // Users - ADMIN only
  // ===========================
  describe('Users - ADMIN only endpoints', () => {
    it('GET /users deve retornar 403 para EMPLOYEE', async () => {
      const res = await request(app)
        .get('/users')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })

    it('GET /users deve retornar 200 para ADMIN', async () => {
      const res = await request(app)
        .get('/users')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
    })

    it('GET /users deve retornar 200 para MANAGER', async () => {
      const res = await request(app)
        .get('/users')
        .set('x-user-role', 'MANAGER')

      expect(res.status).toBe(200)
    })

    it('GET /users/:id deve retornar 403 para EMPLOYEE', async () => {
      const res = await request(app)
        .get('/users/1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })

    it('GET /users/:id deve retornar 403 para MANAGER', async () => {
      const res = await request(app)
        .get('/users/1')
        .set('x-user-role', 'MANAGER')

      expect(res.status).toBe(403)
    })

    it('POST /users deve retornar 403 para EMPLOYEE', async () => {
      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'EMPLOYEE')
        .send({
          name: 'Test',
          email: 'test@test.com',
          employeeNumber: 'T001',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      expect(res.status).toBe(403)
    })

    it('POST /users deve retornar 403 para MANAGER', async () => {
      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'MANAGER')
        .send({
          name: 'Test',
          email: 'test@test.com',
          employeeNumber: 'T001',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      expect(res.status).toBe(403)
    })

    it('PATCH /users/:id/deactivate deve retornar 403 para EMPLOYEE', async () => {
      const res = await request(app)
        .patch('/users/1/deactivate')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })

    it('PATCH /users/:id/deactivate deve retornar 403 para MANAGER', async () => {
      const res = await request(app)
        .patch('/users/1/deactivate')
        .set('x-user-role', 'MANAGER')

      expect(res.status).toBe(403)
    })
  })

  // ===========================
  // Shifts - ADMIN/MANAGER only para escrita
  // ===========================
  describe('Shifts - ADMIN/MANAGER para criação de turnos', () => {
    it('POST /shifts deve retornar 403 para EMPLOYEE', async () => {
      const res = await request(app)
        .post('/shifts')
        .set('x-user-role', 'EMPLOYEE')
        .send({ userId: 1, shiftTypeId: 1, date: '2026-03-25' })

      expect(res.status).toBe(403)
    })

    it('POST /shifts deve ser acessível para ADMIN', async () => {
      const res = await request(app)
        .post('/shifts')
        .set('x-user-role', 'ADMIN')
        .send({ userId: 1, shiftTypeId: 1, date: '2026-03-25' })

      // 404 porque o utilizador/tipo não existe no mock, mas a autorização passou
      expect(res.status).not.toBe(403)
    })

    it('POST /shifts deve ser acessível para MANAGER', async () => {
      const res = await request(app)
        .post('/shifts')
        .set('x-user-role', 'MANAGER')
        .send({ userId: 1, shiftTypeId: 1, date: '2026-03-25' })

      expect(res.status).not.toBe(403)
    })

    it('GET /shifts deve ser acessível para EMPLOYEE', async () => {
      const res = await request(app)
        .get('/shifts?week=2026-03-23')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(200)
    })
  })

  // ===========================
  // ShiftTypes - ADMIN/MANAGER para escrita
  // ===========================
  describe('ShiftTypes - ADMIN/MANAGER para escrita', () => {
    it('POST /shift-types deve retornar 403 para EMPLOYEE', async () => {
      const res = await request(app)
        .post('/shift-types')
        .set('x-user-role', 'EMPLOYEE')
        .send({ name: 'Manhã', startTime: '08:00', endTime: '16:00' })

      expect(res.status).toBe(403)
    })

    it('POST /shift-types deve ser acessível para ADMIN', async () => {
      const res = await request(app)
        .post('/shift-types')
        .set('x-user-role', 'ADMIN')
        .send({ name: 'Manhã', startTime: '08:00', endTime: '16:00' })

      expect(res.status).not.toBe(403)
    })

    it('POST /shift-types deve ser acessível para MANAGER', async () => {
      const res = await request(app)
        .post('/shift-types')
        .set('x-user-role', 'MANAGER')
        .send({ name: 'Tarde', startTime: '16:00', endTime: '00:00' })

      expect(res.status).not.toBe(403)
    })

    it('PATCH /shift-types/:id deve retornar 403 para EMPLOYEE', async () => {
      const res = await request(app)
        .patch('/shift-types/1')
        .set('x-user-role', 'EMPLOYEE')
        .send({ name: 'Noite' })

      expect(res.status).toBe(403)
    })

    it('PATCH /shift-types/:id deve ser acessível para ADMIN', async () => {
      const res = await request(app)
        .patch('/shift-types/1')
        .set('x-user-role', 'ADMIN')
        .send({ name: 'Noite' })

      expect(res.status).not.toBe(403)
    })

    it('DELETE /shift-types/:id deve retornar 403 para EMPLOYEE', async () => {
      const res = await request(app)
        .delete('/shift-types/1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })

    it('DELETE /shift-types/:id deve ser acessível para MANAGER', async () => {
      const res = await request(app)
        .delete('/shift-types/1')
        .set('x-user-role', 'MANAGER')

      expect(res.status).not.toBe(403)
    })

    it('GET /shift-types deve ser acessível para EMPLOYEE', async () => {
      const res = await request(app)
        .get('/shift-types')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(200)
    })
  })

  // ===========================
  // Sem role definido - 403 em endpoints protegidos
  // ===========================
  describe('Sem role definido', () => {
    it('GET /users deve retornar 403 sem role', async () => {
      const res = await request(app).get('/users')
      expect(res.status).toBe(403)
    })

    it('POST /users deve retornar 403 sem role', async () => {
      const res = await request(app).post('/users').send({})
      expect(res.status).toBe(403)
    })

    it('POST /shifts deve retornar 403 sem role', async () => {
      const res = await request(app).post('/shifts').send({})
      expect(res.status).toBe(403)
    })

    it('POST /shift-types deve retornar 403 sem role', async () => {
      const res = await request(app).post('/shift-types').send({})
      expect(res.status).toBe(403)
    })
  })
})
