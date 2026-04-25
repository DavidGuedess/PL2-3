import request from 'supertest'
import app from '../app'

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
    shift: {
      findMany: jest.fn(),
      findUnique: jest.fn(),
      create: jest.fn()
    },
    user: {
      findUnique: jest.fn()
    },
    shiftType: {
      findUnique: jest.fn()
    }
  }
  return { PrismaClient: jest.fn(() => mockInstance) }
})

import { PrismaClient } from '@prisma/client'

const prismaInstance = new (PrismaClient as any)()
const mockFindMany = prismaInstance.shift.findMany as jest.Mock

const mockShift = {
  id: 1,
  userId: 1,
  shiftTypeId: 1,
  date: '2026-04-21T00:00:00.000Z',
  shiftType: {
    id: 1,
    name: 'Manhã',
    startTime: '08:00',
    endTime: '16:00'
  }
}

beforeEach(() => {
  jest.clearAllMocks()
  mockFindMany.mockResolvedValue([mockShift])
})

describe('GET /shifts/me', () => {
  it('deve retornar os turnos do utilizador autenticado por semana', async () => {
    const res = await request(app)
      .get('/shifts/me?week=2026-04-21')
      .set('x-user-id', '1')
      .set('x-user-role', 'EMPLOYEE')

    expect(res.status).toBe(200)
    expect(Array.isArray(res.body)).toBe(true)
    expect(res.body[0]).toHaveProperty('shiftType')
    expect(res.body[0].shiftType).toHaveProperty('name', 'Manhã')
    expect(res.body[0].shiftType).toHaveProperty('startTime')
    expect(res.body[0].shiftType).toHaveProperty('endTime')
  })

  it('deve retornar os turnos do utilizador autenticado por mês', async () => {
    const res = await request(app)
      .get('/shifts/me?month=2026-04')
      .set('x-user-id', '1')
      .set('x-user-role', 'EMPLOYEE')

    expect(res.status).toBe(200)
    expect(Array.isArray(res.body)).toBe(true)
  })

  it('deve retornar 400 quando não é fornecido week nem month', async () => {
    const res = await request(app)
      .get('/shifts/me')
      .set('x-user-id', '1')
      .set('x-user-role', 'EMPLOYEE')

    expect(res.status).toBe(400)
    expect(res.body).toHaveProperty('message')
  })

  it('deve filtrar apenas os turnos do próprio utilizador', async () => {
    await request(app)
      .get('/shifts/me?week=2026-04-21')
      .set('x-user-id', '1')
      .set('x-user-role', 'EMPLOYEE')

    expect(mockFindMany).toHaveBeenCalledWith(
      expect.objectContaining({
        where: expect.objectContaining({ userId: 1 })
      })
    )
  })

  it('deve retornar lista vazia quando não há turnos no período', async () => {
    mockFindMany.mockResolvedValueOnce([])

    const res = await request(app)
      .get('/shifts/me?month=2025-01')
      .set('x-user-id', '1')
      .set('x-user-role', 'EMPLOYEE')

    expect(res.status).toBe(200)
    expect(res.body).toEqual([])
  })

  it('também deve funcionar para MANAGER e ADMIN', async () => {
    for (const role of ['MANAGER', 'ADMIN']) {
      const res = await request(app)
        .get('/shifts/me?week=2026-04-21')
        .set('x-user-id', '2')
        .set('x-user-role', role)

      expect(res.status).toBe(200)
    }
  })
})
