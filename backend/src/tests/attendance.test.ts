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
    attendanceRecord: {
      create: jest.fn(),
      findMany: jest.fn(),
      findFirst: jest.fn()
    },
    user: {
      findUnique: jest.fn()
    },
    shift: {
      findMany: jest.fn()
    }
  }
  return { PrismaClient: jest.fn(() => mockInstance) }
})

import { PrismaClient } from '@prisma/client'

const prismaInstance = new (PrismaClient as any)()
const mockCreate = prismaInstance.attendanceRecord.create as jest.Mock
const mockFindMany = prismaInstance.attendanceRecord.findMany as jest.Mock
const mockFindFirst = prismaInstance.attendanceRecord.findFirst as jest.Mock
const mockUserFindUnique = prismaInstance.user.findUnique as jest.Mock
const mockShiftFindMany = prismaInstance.shift.findMany as jest.Mock

const mockRecord = {
  id: 1,
  userId: 1,
  type: 'IN',
  timestamp: new Date().toISOString(),
  user: { id: 1, name: 'Alice', employeeNumber: 'E001' }
}

const mockUser = {
  id: 2,
  name: 'Bob',
  employeeNumber: 'E002',
  email: 'bob@test.com',
  role: 'EMPLOYEE',
  active: true
}

const mockShift = {
  id: 1,
  userId: 1,
  shiftTypeId: 1,
  date: new Date('2026-04-10T00:00:00.000Z'),
  user: { id: 1, name: 'Alice', employeeNumber: 'E001' }
}

const mockAttendanceIN = {
  id: 1,
  userId: 1,
  type: 'IN',
  timestamp: new Date('2026-04-10T08:00:00.000Z')
}

beforeEach(() => {
  jest.clearAllMocks()
  mockCreate.mockResolvedValue(mockRecord)
  mockFindMany.mockResolvedValue([mockRecord])
  mockFindFirst.mockResolvedValue(null)
  mockUserFindUnique.mockResolvedValue(mockUser)
  mockShiftFindMany.mockResolvedValue([])
})

describe('POST /attendance', () => {
  it('deve registar entrada (IN) para funcionário autenticado', async () => {
    const res = await request(app)
      .post('/attendance')
      .set('x-user-id', '1')
      .set('x-user-role', 'EMPLOYEE')
      .send({ type: 'IN' })

    expect(res.status).toBe(201)
    expect(res.body.type).toBe('IN')
  })

  it('deve registar saída (OUT) para funcionário autenticado', async () => {
    mockFindFirst.mockResolvedValueOnce({ ...mockRecord, type: 'IN' })
    mockCreate.mockResolvedValueOnce({ ...mockRecord, type: 'OUT' })

    const res = await request(app)
      .post('/attendance')
      .set('x-user-id', '1')
      .set('x-user-role', 'EMPLOYEE')
      .send({ type: 'OUT' })

    expect(res.status).toBe(201)
    expect(res.body.type).toBe('OUT')
  })

  it('deve retornar 400 quando type está em falta', async () => {
    const res = await request(app)
      .post('/attendance')
      .set('x-user-id', '1')
      .set('x-user-role', 'EMPLOYEE')
      .send({})

    expect(res.status).toBe(400)
  })

  it('deve retornar 400 quando type é inválido', async () => {
    const res = await request(app)
      .post('/attendance')
      .set('x-user-id', '1')
      .set('x-user-role', 'EMPLOYEE')
      .send({ type: 'INVALID' })

    expect(res.status).toBe(400)
  })
})

describe('GET /attendance/me', () => {
  it('deve retornar o histórico do próprio utilizador', async () => {
    const res = await request(app)
      .get('/attendance/me')
      .set('x-user-id', '1')
      .set('x-user-role', 'EMPLOYEE')

    expect(res.status).toBe(200)
    expect(Array.isArray(res.body)).toBe(true)
  })

  it('deve aceitar filtros from e to', async () => {
    const res = await request(app)
      .get('/attendance/me?from=2026-04-01&to=2026-04-30')
      .set('x-user-id', '1')
      .set('x-user-role', 'EMPLOYEE')

    expect(res.status).toBe(200)
  })
})

describe('GET /attendance (monitorização - ADMIN/MANAGER)', () => {
  it('deve retornar 403 para EMPLOYEE', async () => {
    const res = await request(app)
      .get('/attendance?userId=2')
      .set('x-user-id', '1')
      .set('x-user-role', 'EMPLOYEE')

    expect(res.status).toBe(403)
  })

  it('deve retornar 400 se userId não for fornecido (MANAGER)', async () => {
    const res = await request(app)
      .get('/attendance')
      .set('x-user-id', '1')
      .set('x-user-role', 'MANAGER')

    expect(res.status).toBe(400)
  })

  it('deve retornar 404 se utilizador não existir (MANAGER)', async () => {
    mockUserFindUnique.mockResolvedValueOnce(null)

    const res = await request(app)
      .get('/attendance?userId=999')
      .set('x-user-id', '1')
      .set('x-user-role', 'MANAGER')

    expect(res.status).toBe(404)
  })

  it('deve retornar registos para MANAGER com userId válido', async () => {
    const res = await request(app)
      .get('/attendance?userId=2')
      .set('x-user-id', '1')
      .set('x-user-role', 'MANAGER')

    expect(res.status).toBe(200)
    expect(Array.isArray(res.body)).toBe(true)
  })

  it('deve retornar registos para ADMIN com userId válido', async () => {
    const res = await request(app)
      .get('/attendance?userId=2')
      .set('x-user-id', '1')
      .set('x-user-role', 'ADMIN')

    expect(res.status).toBe(200)
  })

  it('deve aceitar filtros from e to', async () => {
    const res = await request(app)
      .get('/attendance?userId=2&from=2026-04-01&to=2026-04-30')
      .set('x-user-id', '1')
      .set('x-user-role', 'ADMIN')

    expect(res.status).toBe(200)
  })
})

describe('GET /attendance/report (relatório - ADMIN/MANAGER)', () => {
  it('deve retornar 403 para EMPLOYEE', async () => {
    const res = await request(app)
      .get('/attendance/report')
      .set('x-user-id', '1')
      .set('x-user-role', 'EMPLOYEE')

    expect(res.status).toBe(403)
  })

  it('deve retornar 400 se userId for inválido', async () => {
    const res = await request(app)
      .get('/attendance/report?userId=abc')
      .set('x-user-id', '1')
      .set('x-user-role', 'ADMIN')

    expect(res.status).toBe(400)
  })

  it('deve retornar 404 se utilizador não existir', async () => {
    mockUserFindUnique.mockResolvedValueOnce(null)

    const res = await request(app)
      .get('/attendance/report?userId=999')
      .set('x-user-id', '1')
      .set('x-user-role', 'ADMIN')

    expect(res.status).toBe(404)
  })

  it('deve retornar relatório vazio quando não existem turnos', async () => {
    mockShiftFindMany.mockResolvedValueOnce([])
    mockFindMany.mockResolvedValueOnce([])

    const res = await request(app)
      .get('/attendance/report')
      .set('x-user-id', '1')
      .set('x-user-role', 'ADMIN')

    expect(res.status).toBe(200)
    expect(res.body).toEqual([])
  })

  it('deve calcular dias presentes e ausentes corretamente', async () => {
    const shiftAbsent = {
      ...mockShift,
      date: new Date('2026-04-11T00:00:00.000Z')
    }
    mockShiftFindMany.mockResolvedValueOnce([mockShift, shiftAbsent])
    mockFindMany.mockResolvedValueOnce([mockAttendanceIN])

    const res = await request(app)
      .get('/attendance/report')
      .set('x-user-id', '1')
      .set('x-user-role', 'ADMIN')

    expect(res.status).toBe(200)
    expect(Array.isArray(res.body)).toBe(true)
    const entry = res.body[0]
    expect(entry.userId).toBe(1)
    expect(entry.totalScheduledDays).toBe(2)
    expect(entry.daysPresent).toBe(1)
    expect(entry.daysAbsent).toBe(1)
  })

  it('deve agrupar resultados por utilizador', async () => {
    const shift1 = { ...mockShift, userId: 1, user: { id: 1, name: 'Alice', employeeNumber: 'E001' } }
    const shift2 = { ...mockShift, id: 2, userId: 2, date: new Date('2026-04-10T00:00:00.000Z'), user: { id: 2, name: 'Bob', employeeNumber: 'E002' } }
    mockShiftFindMany.mockResolvedValueOnce([shift1, shift2])
    mockFindMany.mockResolvedValueOnce([mockAttendanceIN])

    const res = await request(app)
      .get('/attendance/report')
      .set('x-user-id', '1')
      .set('x-user-role', 'MANAGER')

    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(2)
    const names = res.body.map((e: any) => e.name)
    expect(names).toContain('Alice')
    expect(names).toContain('Bob')
  })

  it('deve filtrar por userId quando fornecido', async () => {
    mockUserFindUnique.mockResolvedValueOnce(mockUser)
    mockShiftFindMany.mockResolvedValueOnce([mockShift])
    mockFindMany.mockResolvedValueOnce([mockAttendanceIN])

    const res = await request(app)
      .get('/attendance/report?userId=1')
      .set('x-user-id', '1')
      .set('x-user-role', 'MANAGER')

    expect(res.status).toBe(200)
    expect(Array.isArray(res.body)).toBe(true)
  })

  it('deve aceitar filtros from e to', async () => {
    mockShiftFindMany.mockResolvedValueOnce([mockShift])
    mockFindMany.mockResolvedValueOnce([mockAttendanceIN])

    const res = await request(app)
      .get('/attendance/report?from=2026-04-01&to=2026-04-30')
      .set('x-user-id', '1')
      .set('x-user-role', 'ADMIN')

    expect(res.status).toBe(200)
  })

  it('deve retornar relatório para MANAGER', async () => {
    mockShiftFindMany.mockResolvedValueOnce([mockShift])
    mockFindMany.mockResolvedValueOnce([mockAttendanceIN])

    const res = await request(app)
      .get('/attendance/report')
      .set('x-user-id', '1')
      .set('x-user-role', 'MANAGER')

    expect(res.status).toBe(200)
    expect(Array.isArray(res.body)).toBe(true)
  })
})
