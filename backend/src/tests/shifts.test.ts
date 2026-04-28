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
      create: jest.fn(),
      update: jest.fn(),
      delete: jest.fn()
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
const mockFindUnique = prismaInstance.shift.findUnique as jest.Mock
const mockUpdate = prismaInstance.shift.update as jest.Mock
const mockDelete = prismaInstance.shift.delete as jest.Mock
const mockShiftTypeFindUnique = prismaInstance.shiftType.findUnique as jest.Mock

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
  },
  user: { id: 1, name: 'João Silva', employeeNumber: 'EMP001' }
}

const mockShiftType = {
  id: 2,
  name: 'Tarde',
  startTime: '16:00',
  endTime: '00:00'
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

describe('PATCH /shifts/:id', () => {
  it('deve atualizar o tipo de turno com sucesso (ADMIN)', async () => {
    mockFindUnique.mockResolvedValueOnce(mockShift)
    mockShiftTypeFindUnique.mockResolvedValueOnce(mockShiftType)
    mockUpdate.mockResolvedValueOnce({ ...mockShift, shiftTypeId: 2, shiftType: mockShiftType })

    const res = await request(app)
      .patch('/shifts/1')
      .set('x-user-id', '1')
      .set('x-user-role', 'ADMIN')
      .send({ shiftTypeId: 2 })

    expect(res.status).toBe(200)
    expect(res.body).toHaveProperty('shiftTypeId', 2)
  })

  it('deve atualizar a data do turno com sucesso (MANAGER)', async () => {
    mockFindUnique
      .mockResolvedValueOnce(mockShift)
      .mockResolvedValueOnce(null)
    mockUpdate.mockResolvedValueOnce({ ...mockShift, date: '2026-04-30T00:00:00.000Z' })

    const res = await request(app)
      .patch('/shifts/1')
      .set('x-user-id', '1')
      .set('x-user-role', 'MANAGER')
      .send({ date: '2026-04-30' })

    expect(res.status).toBe(200)
    expect(mockUpdate).toHaveBeenCalledWith(
      expect.objectContaining({
        where: { id: 1 },
        data: expect.objectContaining({ date: new Date('2026-04-30') })
      })
    )
  })

  it('deve retornar 400 quando nenhum campo é fornecido', async () => {
    const res = await request(app)
      .patch('/shifts/1')
      .set('x-user-id', '1')
      .set('x-user-role', 'ADMIN')
      .send({})

    expect(res.status).toBe(400)
    expect(res.body).toHaveProperty('message')
  })

  it('deve retornar 404 quando o turno não existe', async () => {
    mockFindUnique.mockResolvedValueOnce(null)

    const res = await request(app)
      .patch('/shifts/99')
      .set('x-user-id', '1')
      .set('x-user-role', 'ADMIN')
      .send({ shiftTypeId: 2 })

    expect(res.status).toBe(404)
    expect(res.body).toHaveProperty('message', 'Shift not found')
  })

  it('deve retornar 404 quando o tipo de turno não existe', async () => {
    mockFindUnique.mockResolvedValueOnce(mockShift)
    mockShiftTypeFindUnique.mockResolvedValueOnce(null)

    const res = await request(app)
      .patch('/shifts/1')
      .set('x-user-id', '1')
      .set('x-user-role', 'ADMIN')
      .send({ shiftTypeId: 99 })

    expect(res.status).toBe(404)
    expect(res.body).toHaveProperty('message', 'Shift type not found')
  })

  it('deve retornar 409 quando o utilizador já tem turno na nova data', async () => {
    const conflictShift = { ...mockShift, id: 2 }
    mockFindUnique
      .mockResolvedValueOnce(mockShift)
      .mockResolvedValueOnce(conflictShift)

    const res = await request(app)
      .patch('/shifts/1')
      .set('x-user-id', '1')
      .set('x-user-role', 'ADMIN')
      .send({ date: '2026-04-22' })

    expect(res.status).toBe(409)
    expect(res.body).toHaveProperty('message', 'User already has a shift on this date')
  })

  it('deve retornar 403 para EMPLOYEE', async () => {
    const res = await request(app)
      .patch('/shifts/1')
      .set('x-user-id', '1')
      .set('x-user-role', 'EMPLOYEE')
      .send({ shiftTypeId: 2 })

    expect(res.status).toBe(403)
  })
})

describe('DELETE /shifts/:id', () => {
  it('deve remover o turno com sucesso (ADMIN)', async () => {
    mockFindUnique.mockResolvedValueOnce(mockShift)
    mockDelete.mockResolvedValueOnce(mockShift)

    const res = await request(app)
      .delete('/shifts/1')
      .set('x-user-id', '1')
      .set('x-user-role', 'ADMIN')

    expect(res.status).toBe(204)
    expect(mockDelete).toHaveBeenCalledWith({ where: { id: 1 } })
  })

  it('deve remover o turno com sucesso (MANAGER)', async () => {
    mockFindUnique.mockResolvedValueOnce(mockShift)
    mockDelete.mockResolvedValueOnce(mockShift)

    const res = await request(app)
      .delete('/shifts/1')
      .set('x-user-id', '1')
      .set('x-user-role', 'MANAGER')

    expect(res.status).toBe(204)
  })

  it('deve retornar 404 quando o turno não existe', async () => {
    mockFindUnique.mockResolvedValueOnce(null)

    const res = await request(app)
      .delete('/shifts/99')
      .set('x-user-id', '1')
      .set('x-user-role', 'ADMIN')

    expect(res.status).toBe(404)
    expect(res.body).toHaveProperty('message', 'Shift not found')
  })

  it('deve retornar 403 para EMPLOYEE', async () => {
    const res = await request(app)
      .delete('/shifts/1')
      .set('x-user-id', '1')
      .set('x-user-role', 'EMPLOYEE')

    expect(res.status).toBe(403)
  })
})
