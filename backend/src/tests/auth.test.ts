import request from 'supertest'
import app from '../app'
import { PrismaClient } from '@prisma/client'
import * as tokenBlacklistService from '../services/tokenBlacklist'
import * as refreshTokenService from '../services/refreshToken'

jest.mock('@prisma/client', () => {
  const mockInstance = { user: { findUnique: jest.fn() } }
  return { PrismaClient: jest.fn(() => mockInstance) }
})

jest.mock('../services/tokenBlacklist')
jest.mock('../services/refreshToken')

const mockIsTokenBlacklisted = tokenBlacklistService.isTokenBlacklisted as jest.Mock
const mockAddToBlacklist = tokenBlacklistService.addToBlacklist as jest.Mock
const mockFindRefreshToken = refreshTokenService.findRefreshToken as jest.Mock
const mockCreateRefreshToken = refreshTokenService.createRefreshToken as jest.Mock
const mockDeleteRefreshToken = refreshTokenService.deleteRefreshToken as jest.Mock

let mockUserFindUnique: jest.Mock

const mockUser = {
  id: 1,
  employeeNumber: 'EMP001',
  name: 'Test User',
  email: 'test@test.com',
  passwordHash: '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', // "password"
  role: 'EMPLOYEE',
  category: 'VETERINARIAN',
  active: true
}

beforeAll(() => {
  const prismaInstance = (PrismaClient as jest.Mock).mock.results[0]?.value
  mockUserFindUnique = prismaInstance?.user?.findUnique
})

beforeEach(() => {
  jest.clearAllMocks()
  mockIsTokenBlacklisted.mockResolvedValue(false)
  mockAddToBlacklist.mockResolvedValue(undefined)
  mockCreateRefreshToken.mockResolvedValue(undefined)
  mockDeleteRefreshToken.mockResolvedValue(undefined)
})

describe('Auth API', () => {

  // ======================
  // POST /api/auth/login
  // ======================
  describe('POST /api/auth/login', () => {

    it('should login successfully with valid credentials', async () => {
      mockUserFindUnique.mockResolvedValue(mockUser)

      const res = await request(app)
        .post('/api/auth/login')
        .send({ employeeNumber: 'EMP001', password: 'password' })

      expect(res.status).toBe(200)
      expect(res.body).toHaveProperty('accessToken')
      expect(res.body).toHaveProperty('refreshToken')
      expect(res.body.user.employeeNumber).toBe('EMP001')
      expect(res.body.user).not.toHaveProperty('passwordHash')
    })

    it('should fail if employeeNumber is missing', async () => {
      const res = await request(app)
        .post('/api/auth/login')
        .send({ password: 'password' })

      expect(res.status).toBe(400)
    })

    it('should fail if password is missing', async () => {
      const res = await request(app)
        .post('/api/auth/login')
        .send({ employeeNumber: 'EMP001' })

      expect(res.status).toBe(400)
    })

    it('should fail if user does not exist', async () => {
      mockUserFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .post('/api/auth/login')
        .send({ employeeNumber: 'INVALID', password: 'password' })

      expect(res.status).toBe(401)
    })

    it('should fail if user is inactive', async () => {
      mockUserFindUnique.mockResolvedValue({ ...mockUser, active: false })

      const res = await request(app)
        .post('/api/auth/login')
        .send({ employeeNumber: 'EMP001', password: 'password' })

      expect(res.status).toBe(401)
    })

    it('should fail with wrong password', async () => {
      mockUserFindUnique.mockResolvedValue(mockUser)

      const res = await request(app)
        .post('/api/auth/login')
        .send({ employeeNumber: 'EMP001', password: 'wrongpassword' })

      expect(res.status).toBe(401)
    })

  })

  // ======================
  // POST /api/auth/refresh
  // ======================
  describe('POST /api/auth/refresh', () => {

    it('should return new tokens with valid refresh token', async () => {
      mockFindRefreshToken.mockResolvedValue({
        token: 'valid-refresh-token',
        expiresAt: new Date(Date.now() + 1000 * 60 * 60),
        user: { id: 1, email: 'test@test.com', role: 'EMPLOYEE' }
      })

      const res = await request(app)
        .post('/api/auth/refresh')
        .send({ refreshToken: 'valid-refresh-token' })

      expect(res.status).toBe(200)
      expect(res.body).toHaveProperty('accessToken')
      expect(res.body).toHaveProperty('refreshToken')
    })

    it('should fail if refreshToken is missing', async () => {
      const res = await request(app)
        .post('/api/auth/refresh')
        .send({})

      expect(res.status).toBe(400)
    })

    it('should fail if refresh token does not exist', async () => {
      mockFindRefreshToken.mockResolvedValue(null)

      const res = await request(app)
        .post('/api/auth/refresh')
        .send({ refreshToken: 'invalid-token' })

      expect(res.status).toBe(401)
    })

    it('should fail if refresh token is expired', async () => {
      mockFindRefreshToken.mockResolvedValue({
        token: 'expired-token',
        expiresAt: new Date(Date.now() - 1000),
        user: { id: 1, email: 'test@test.com', role: 'EMPLOYEE' }
      })

      const res = await request(app)
        .post('/api/auth/refresh')
        .send({ refreshToken: 'expired-token' })

      expect(res.status).toBe(401)
    })

  })

  // ======================
  // POST /api/auth/logout
  // ======================
  describe('POST /api/auth/logout', () => {

    it('should logout successfully with valid token', async () => {
      mockUserFindUnique.mockResolvedValue(mockUser)

      const loginRes = await request(app)
        .post('/api/auth/login')
        .send({ employeeNumber: 'EMP001', password: 'password' })

      const res = await request(app)
        .post('/api/auth/logout')
        .set('Authorization', `Bearer ${loginRes.body.accessToken}`)
        .send({ refreshToken: 'some-refresh-token' })

      expect(res.status).toBe(200)
      expect(res.body).toHaveProperty('message')
    })

    it('should fail without Authorization header', async () => {
      const res = await request(app)
        .post('/api/auth/logout')
        .send({})

      expect(res.status).toBe(401)
    })

  })

})
