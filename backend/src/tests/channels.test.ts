import request from 'supertest'
import app from '../app'
import { PrismaClient } from '@prisma/client'

jest.mock('@prisma/client', () => {
  const mockInstance = {
    channel: {
      findMany:   jest.fn(),
      create:     jest.fn(),
      findUnique: jest.fn(),
      delete:     jest.fn(),
      update:     jest.fn(),
    },
    channelMember: {
      createMany:  jest.fn(),
      findUnique:  jest.fn(),
    },
    message: {
      findMany: jest.fn(),
      create:   jest.fn(),
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

let mockChannelFindMany:   jest.Mock
let mockChannelCreate:     jest.Mock
let mockChannelFindUnique: jest.Mock
let mockChannelDelete:     jest.Mock
let mockChannelUpdate:     jest.Mock
let mockMemberCreateMany:  jest.Mock
let mockMemberFindUnique:  jest.Mock
let mockMessageFindMany:   jest.Mock
let mockMessageCreate:     jest.Mock

beforeAll(() => {
  const p = (PrismaClient as jest.Mock).mock.results[0]?.value
  mockChannelFindMany   = p?.channel?.findMany
  mockChannelCreate     = p?.channel?.create
  mockChannelFindUnique = p?.channel?.findUnique
  mockChannelDelete     = p?.channel?.delete
  mockChannelUpdate     = p?.channel?.update
  mockMemberCreateMany  = p?.channelMember?.createMany
  mockMemberFindUnique  = p?.channelMember?.findUnique
  mockMessageFindMany   = p?.message?.findMany
  mockMessageCreate     = p?.message?.create
})

const mockChannel = {
  id: 1, name: 'geral', description: null, isPublic: true,
  type: 'GROUP', createdById: 1,
  updatedAt: new Date(), createdAt: new Date(),
  messages: [],
  members: [{ userId: 1 }]
}

const mockMessage = {
  id: 1, channelId: 1, userId: 1, content: 'Olá',
  createdAt: new Date(),
  user: { id: 1, name: 'Test User', profilePicture: null }
}

beforeEach(() => { jest.clearAllMocks() })

describe('Channels API', () => {

  describe('GET /channels', () => {

    it('should return channels accessible to the user', async () => {
      mockChannelFindMany.mockResolvedValue([mockChannel])

      const res = await request(app).get('/channels').set('x-user-id', '1')

      expect(res.status).toBe(200)
      expect(Array.isArray(res.body)).toBe(true)
      expect(res.body[0].id).toBe(1)
    })

    it('should return empty array when no channels exist', async () => {
      mockChannelFindMany.mockResolvedValue([])

      const res = await request(app).get('/channels').set('x-user-id', '1')

      expect(res.status).toBe(200)
      expect(res.body).toEqual([])
    })

  })

  describe('POST /channels', () => {

    it('admin can create a GROUP channel', async () => {
      mockChannelCreate.mockResolvedValue({ ...mockChannel, type: 'GROUP' })
      mockMemberCreateMany.mockResolvedValue({ count: 1 })

      const res = await request(app)
        .post('/channels')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ name: 'equipa', type: 'GROUP', memberIds: [2, 3] })

      expect(res.status).toBe(201)
    })

    it('admin can create an ANNOUNCEMENT channel', async () => {
      mockChannelCreate.mockResolvedValue({ ...mockChannel, type: 'ANNOUNCEMENT', isPublic: true })
      mockMemberCreateMany.mockResolvedValue({ count: 1 })

      const res = await request(app)
        .post('/channels')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ name: 'avisos', type: 'ANNOUNCEMENT' })

      expect(res.status).toBe(201)
    })

    it('any user can create a DM channel', async () => {
      mockChannelCreate.mockResolvedValue({ ...mockChannel, name: 'dm-1-2', type: 'DM', isPublic: false })
      mockMemberCreateMany.mockResolvedValue({ count: 2 })

      const res = await request(app)
        .post('/channels')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')
        .send({ name: 'dm-1-2', type: 'DM', memberIds: [2] })

      expect(res.status).toBe(201)
    })

    it('employee cannot create GROUP channel', async () => {
      const res = await request(app)
        .post('/channels')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')
        .send({ name: 'equipa', type: 'GROUP' })

      expect(res.status).toBe(403)
    })

    it('should return 400 with missing channel name', async () => {
      const res = await request(app)
        .post('/channels')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ type: 'GROUP' })

      expect(res.status).toBe(400)
    })

    it('should return 409 on duplicate channel name', async () => {
      mockChannelCreate.mockRejectedValue({ code: 'P2002' })

      const res = await request(app)
        .post('/channels')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')
        .send({ name: 'geral', type: 'GROUP' })

      expect(res.status).toBe(409)
    })

  })

  describe('DELETE /channels/:id', () => {

    it('DM member can delete own DM', async () => {
      mockChannelFindUnique.mockResolvedValue({ ...mockChannel, type: 'DM', isPublic: false, members: [{ userId: 1 }] })
      mockChannelDelete.mockResolvedValue({})

      const res = await request(app)
        .delete('/channels/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(204)
    })

    it('admin can delete GROUP channel', async () => {
      mockChannelFindUnique.mockResolvedValue({ ...mockChannel, type: 'GROUP', members: [] })
      mockChannelDelete.mockResolvedValue({})

      const res = await request(app)
        .delete('/channels/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(204)
    })

    it('employee cannot delete GROUP channel', async () => {
      mockChannelFindUnique.mockResolvedValue({ ...mockChannel, type: 'GROUP', members: [] })

      const res = await request(app)
        .delete('/channels/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })

    it('non-member cannot delete DM', async () => {
      mockChannelFindUnique.mockResolvedValue({ ...mockChannel, type: 'DM', isPublic: false, members: [{ userId: 99 }] })

      const res = await request(app)
        .delete('/channels/1')
        .set('x-user-id', '1')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })

    it('should return 404 if channel not found', async () => {
      mockChannelFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .delete('/channels/999')
        .set('x-user-id', '1')

      expect(res.status).toBe(404)
    })

  })

  describe('GET /channels/:id/messages', () => {

    it('should return messages for public channel', async () => {
      mockChannelFindUnique.mockResolvedValue({ ...mockChannel, isPublic: true })
      mockMessageFindMany.mockResolvedValue([mockMessage])

      const res = await request(app)
        .get('/channels/1/messages')
        .set('x-user-id', '1')

      expect(res.status).toBe(200)
      expect(Array.isArray(res.body)).toBe(true)
    })

    it('should return messages for private channel when user is member', async () => {
      mockChannelFindUnique.mockResolvedValue({ ...mockChannel, isPublic: false })
      mockMemberFindUnique.mockResolvedValue({ channelId: 1, userId: 1 })
      mockMessageFindMany.mockResolvedValue([mockMessage])

      const res = await request(app)
        .get('/channels/1/messages')
        .set('x-user-id', '1')

      expect(res.status).toBe(200)
    })

    it('should return 403 for private channel non-member', async () => {
      mockChannelFindUnique.mockResolvedValue({ ...mockChannel, isPublic: false })
      mockMemberFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .get('/channels/1/messages')
        .set('x-user-id', '1')

      expect(res.status).toBe(403)
    })

    it('should return 404 if channel not found', async () => {
      mockChannelFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .get('/channels/999/messages')
        .set('x-user-id', '1')

      expect(res.status).toBe(404)
    })

  })

  describe('POST /channels/:id/messages', () => {

    it('should send message to public channel', async () => {
      mockChannelFindUnique.mockResolvedValue({ ...mockChannel, isPublic: true })
      mockMessageCreate.mockResolvedValue(mockMessage)
      mockChannelUpdate.mockResolvedValue({})

      const res = await request(app)
        .post('/channels/1/messages')
        .set('x-user-id', '1')
        .send({ content: 'Olá' })

      expect(res.status).toBe(201)
      expect(res.body.content).toBe('Olá')
    })

    it('should return 400 if content is whitespace', async () => {
      mockChannelFindUnique.mockResolvedValue({ ...mockChannel, isPublic: true })

      const res = await request(app)
        .post('/channels/1/messages')
        .set('x-user-id', '1')
        .send({ content: '   ' })

      expect(res.status).toBe(400)
    })

    it('should return 400 if content is missing', async () => {
      mockChannelFindUnique.mockResolvedValue({ ...mockChannel, isPublic: true })

      const res = await request(app)
        .post('/channels/1/messages')
        .set('x-user-id', '1')
        .send({})

      expect(res.status).toBe(400)
    })

    it('should return 404 if channel not found', async () => {
      mockChannelFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .post('/channels/999/messages')
        .set('x-user-id', '1')
        .send({ content: 'Olá' })

      expect(res.status).toBe(404)
    })

    it('should return 403 for private channel non-member', async () => {
      mockChannelFindUnique.mockResolvedValue({ ...mockChannel, isPublic: false })
      mockMemberFindUnique.mockResolvedValue(null)

      const res = await request(app)
        .post('/channels/1/messages')
        .set('x-user-id', '1')
        .send({ content: 'Olá' })

      expect(res.status).toBe(403)
    })

  })

})
