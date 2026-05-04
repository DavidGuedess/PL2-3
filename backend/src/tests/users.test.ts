import request from 'supertest'
import app from '../app'
import { PrismaClient } from '@prisma/client'

jest.mock('@prisma/client', () => {
  const mockInstance = {
    user: {
      findMany: jest.fn(),
      findUnique: jest.fn(),
      create: jest.fn(),
      update: jest.fn(),
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

const mockUser = {
  id: 1,
  employeeNumber: 'TEST001',
  name: 'Test User',
  email: 'test@test.com',
  passwordHash: '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
  contact: null,
  role: 'EMPLOYEE',
  category: 'VETERINARIAN',
  active: true,
  createdAt: new Date(),
  updatedAt: new Date(),
}

let mockFindMany: jest.Mock
let mockFindUnique: jest.Mock
let mockCreate: jest.Mock
let mockUpdate: jest.Mock

beforeAll(() => {
  const prismaInstance = (PrismaClient as jest.Mock).mock.results[0]?.value
  mockFindMany = prismaInstance?.user?.findMany
  mockFindUnique = prismaInstance?.user?.findUnique
  mockCreate = prismaInstance?.user?.create
  mockUpdate = prismaInstance?.user?.update
})

beforeEach(() => {
  jest.clearAllMocks()
})

describe('Users API', () => {

  describe('GET /users', () => {
    it('should return all users without passwordHash field', async () => {
      mockFindMany.mockResolvedValue([mockUser])

      const res = await request(app).get('/users').set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
      expect(Array.isArray(res.body)).toBe(true)
      expect(res.body[0].passwordHash).toBeUndefined()
      expect(res.body[0].email).toBe('test@test.com')
    })

    it('should return 403 if non-admin tries to list users', async () => {
      const res = await request(app).get('/users').set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })
  })

  describe('GET /users/me', () => {
    it('should return the authenticated user', async () => {
      mockFindUnique.mockResolvedValue(mockUser)

      const res = await request(app)
        .get('/users/me')
        .set('x-user-id', '1')

      expect(res.status).toBe(200)
      expect(res.body.id).toBe(1)
      expect(res.body.email).toBe('test@test.com')
      expect(res.body.passwordHash).toBeUndefined()
    })

    it('should return 401 if authenticated user is missing', async () => {
      const res = await request(app).get('/users/me')

      expect(res.status).toBe(401)
    })
  })

  describe('GET /users/:id', () => {
    it('should return a user by id for admin', async () => {
      mockFindUnique.mockResolvedValue(mockUser)

      const res = await request(app)
        .get('/users/1')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
      expect(res.body.id).toBe(1)
      expect(res.body.passwordHash).toBeUndefined()
    })

    it('should return 404 if user does not exist', async () => {
      mockFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .get('/users/99999')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(404)
    })

    it('should return 403 if non-admin tries to get user by id', async () => {
      const res = await request(app)
        .get('/users/1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })
  })

  describe('POST /users', () => {
    it('should create a new user', async () => {
      mockCreate.mockResolvedValue({ ...mockUser, email: 'testuser1@test.com', employeeNumber: 'TEST001' })

      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Test User',
          email: 'testuser1@test.com',
          employeeNumber: 'TEST001',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      expect(res.status).toBe(201)
      expect(res.body).toHaveProperty('id')
      expect(res.body.email).toBe('testuser1@test.com')
      expect(res.body.active).toBe(true)
      expect(res.body.passwordHash).toBeUndefined()
    })

    it('should fail if required fields are missing', async () => {
      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({ email: 'missing@test.com' })

      expect(res.status).toBe(400)
    })

    it('should fail with invalid email', async () => {
      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Invalid Email',
          email: 'invalid-email',
          employeeNumber: 'TEST002',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      expect(res.status).toBe(400)
    })

    it('should fail with invalid role', async () => {
      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Invalid Role',
          email: 'role@test.com',
          employeeNumber: 'TEST003',
          role: 'INVALID_ROLE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      expect(res.status).toBe(400)
    })

    it('should fail with invalid category', async () => {
      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Invalid Category',
          email: 'category@test.com',
          employeeNumber: 'TEST003B',
          role: 'EMPLOYEE',
          category: 'INVALID_CATEGORY',
          password: 'secret123'
        })

      expect(res.status).toBe(400)
    })

    it('should fail with password too short', async () => {
      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Short Password',
          email: 'short@test.com',
          employeeNumber: 'TEST003C',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: '123'
        })

      expect(res.status).toBe(400)
    })

    it('should fail if email already exists', async () => {
      mockCreate.mockRejectedValue({ code: 'P2002', meta: { target: ['email'] } })

      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Duplicate Email',
          email: 'duplicate@test.com',
          employeeNumber: 'TEST004',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      expect(res.status).toBe(409)
      expect(res.body.message).toBe('Email already exists')
    })

    it('should fail if employee number already exists', async () => {
      mockCreate.mockRejectedValue({ code: 'P2002', meta: { target: ['employeeNumber'] } })

      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Duplicate Employee',
          email: 'employee@test.com',
          employeeNumber: 'TEST006',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      expect(res.status).toBe(409)
      expect(res.body.message).toBe('Employee number already exists')
    })

    it('should return 403 if non-admin tries to create a user', async () => {
      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'EMPLOYEE')
        .send({
          name: 'Forbidden User',
          email: 'forbidden@test.com',
          employeeNumber: 'TEST010',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      expect(res.status).toBe(403)
    })
  })

  describe('PATCH /users/:id/deactivate', () => {
    it('should deactivate a user', async () => {
      mockFindUnique.mockResolvedValue(mockUser)
      mockUpdate.mockResolvedValue({ ...mockUser, active: false })

      const res = await request(app)
        .patch('/users/1/deactivate')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
      expect(res.body.active).toBe(false)
      expect(res.body.passwordHash).toBeUndefined()
    })

    it('should fail if user does not exist', async () => {
      mockFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .patch('/users/99999/deactivate')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(404)
    })

    it('should fail if user is already deactivated', async () => {
      mockFindUnique.mockResolvedValue({ ...mockUser, active: false })

      const res = await request(app)
        .patch('/users/1/deactivate')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(409)
    })

    it('should return 403 if non-admin tries to deactivate a user', async () => {
      const res = await request(app)
        .patch('/users/1/deactivate')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })
  })

  describe('PATCH /users/:id/activate', () => {
    it('should activate a user', async () => {
      mockFindUnique.mockResolvedValue({ ...mockUser, active: false })
      mockUpdate.mockResolvedValue({ ...mockUser, active: true })

      const res = await request(app)
        .patch('/users/1/activate')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
      expect(res.body.active).toBe(true)
      expect(res.body.passwordHash).toBeUndefined()
    })

    it('should fail if user does not exist', async () => {
      mockFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .patch('/users/99999/activate')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(404)
    })

    it('should fail if user is already active', async () => {
      mockFindUnique.mockResolvedValue(mockUser)

      const res = await request(app)
        .patch('/users/1/activate')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(409)
    })

    it('should return 403 if non-admin tries to activate a user', async () => {
      const res = await request(app)
        .patch('/users/1/activate')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })
  })

  describe('PATCH /users/me', () => {
    it('should update the authenticated user name', async () => {
      mockFindUnique.mockResolvedValue(mockUser)
      mockUpdate.mockResolvedValue({ ...mockUser, name: 'New Name' })

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', '1')
        .send({ name: 'New Name' })

      expect(res.status).toBe(200)
      expect(res.body.name).toBe('New Name')
      expect(res.body.passwordHash).toBeUndefined()
    })

    it('should update the authenticated user contact', async () => {
      mockFindUnique.mockResolvedValue(mockUser)
      mockUpdate.mockResolvedValue({ ...mockUser, contact: '+351912345678' })

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', '1')
        .send({ contact: '+351912345678' })

      expect(res.status).toBe(200)
      expect(res.body.contact).toBe('+351912345678')
      expect(res.body.passwordHash).toBeUndefined()
    })

    it('should update the authenticated user password without returning it', async () => {
      mockFindUnique.mockResolvedValue(mockUser)
      mockUpdate.mockResolvedValue(mockUser)

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', '1')
        .send({ password: 'newpassword456' })

      expect(res.status).toBe(200)
      expect(res.body.passwordHash).toBeUndefined()
    })

    it('should update multiple fields of the authenticated user', async () => {
      mockFindUnique.mockResolvedValue(mockUser)
      mockUpdate.mockResolvedValue({ ...mockUser, name: 'Updated Multi User', contact: '+351911111111' })

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', '1')
        .send({ name: 'Updated Multi User', contact: '+351911111111', password: 'updatedpassword' })

      expect(res.status).toBe(200)
      expect(res.body.name).toBe('Updated Multi User')
      expect(res.body.contact).toBe('+351911111111')
      expect(res.body.passwordHash).toBeUndefined()
    })

    it('should return 400 if no fields are provided', async () => {
      mockFindUnique.mockResolvedValue(mockUser)

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', '1')
        .send({})

      expect(res.status).toBe(400)
    })

    it('should return 400 for invalid name', async () => {
      mockFindUnique.mockResolvedValue(mockUser)

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', '1')
        .send({ name: '' })

      expect(res.status).toBe(400)
    })

    it('should return 400 for invalid contact', async () => {
      mockFindUnique.mockResolvedValue(mockUser)

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', '1')
        .send({ contact: '' })

      expect(res.status).toBe(400)
    })

    it('should return 400 for invalid password', async () => {
      mockFindUnique.mockResolvedValue(mockUser)

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', '1')
        .send({ password: '123' })

      expect(res.status).toBe(400)
    })

    it('should return 401 if authenticated user is missing', async () => {
      const res = await request(app)
        .patch('/users/me')
        .send({ name: 'No Auth' })

      expect(res.status).toBe(401)
    })
  })
})
