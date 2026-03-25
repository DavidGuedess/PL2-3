import request from 'supertest'
import app from '../app'
import { users } from '../data/users'

jest.mock('../middleware/auth', () => ({
  authenticate: (req: any, _res: any, next: any) => {
    const userId = req.headers['x-user-id']
    req.user = { userId: userId ? Number(userId) : NaN, email: '' }
    next()
  }
}))

describe('Users API', () => {
  beforeEach(() => {
    users.length = 0
  })

  describe('GET /users', () => {
    it('should return all users without passwordHash field', async () => {
      await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'List User',
          email: 'listuser@test.com',
          employeeNumber: 'LIST001',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      const res = await request(app).get('/users')

      expect(res.status).toBe(200)
      expect(Array.isArray(res.body)).toBe(true)
      expect(res.body[0].passwordHash).toBeUndefined()
    })
  })

  describe('GET /users/me', () => {
    it('should return the authenticated user', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Me User',
          email: 'me@test.com',
          employeeNumber: 'ME001',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .get('/users/me')
        .set('x-user-id', userId)

      expect(res.status).toBe(200)
      expect(res.body.id).toBe(userId)
      expect(res.body.email).toBe('me@test.com')
      expect(res.body.passwordHash).toBeUndefined()
    })

    it('should return 401 if authenticated user is missing', async () => {
      const res = await request(app).get('/users/me')

      expect(res.status).toBe(401)
    })
  })

  describe('GET /users/:id', () => {
    it('should return a user by id for admin', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Target User',
          email: 'target@test.com',
          employeeNumber: 'TARGET001',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .get(`/users/${userId}`)
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
      expect(res.body.id).toBe(userId)
      expect(res.body.passwordHash).toBeUndefined()
    })

    it('should return 404 if user does not exist', async () => {
      const res = await request(app)
        .get('/users/99999')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(404)
    })

    it('should return 403 if non-admin tries to get user by id', async () => {
      const res = await request(app)
        .get('/users/99999')
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })
  })

  describe('POST /users', () => {
    it('should create a new user', async () => {
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
        .send({
          email: 'missing@test.com'
        })

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
      const user = {
        name: 'Duplicate Email',
        email: 'duplicate@test.com',
        employeeNumber: 'TEST004',
        role: 'EMPLOYEE',
        category: 'VETERINARIAN',
        password: 'secret123'
      }

      await request(app).post('/users').set('x-user-role', 'ADMIN').send(user)

      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({ ...user, employeeNumber: 'TEST005' })

      expect(res.status).toBe(409)
    })

    it('should fail if employee number already exists', async () => {
      const user = {
        name: 'Duplicate Employee',
        email: 'employee@test.com',
        employeeNumber: 'TEST006',
        role: 'EMPLOYEE',
        category: 'VETERINARIAN',
        password: 'secret123'
      }

      await request(app).post('/users').set('x-user-role', 'ADMIN').send(user)

      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({ ...user, email: 'newemail@test.com' })

      expect(res.status).toBe(409)
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
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Deactivate User',
          email: 'deactivate@test.com',
          employeeNumber: 'TEST007',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch(`/users/${userId}/deactivate`)
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
      expect(res.body.active).toBe(false)
      expect(res.body.passwordHash).toBeUndefined()
    })

    it('should fail if user does not exist', async () => {
      const res = await request(app)
        .patch('/users/99999/deactivate')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(404)
    })

    it('should fail if user is already deactivated', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Already Deactivated',
          email: 'already@test.com',
          employeeNumber: 'TEST008',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      const userId = createRes.body.id

      await request(app).patch(`/users/${userId}/deactivate`).set('x-user-role', 'ADMIN')

      const res = await request(app)
        .patch(`/users/${userId}/deactivate`)
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(409)
    })

    it('should return 403 if non-admin tries to deactivate a user', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Protected User',
          email: 'protected@test.com',
          employeeNumber: 'TEST011',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch(`/users/${userId}/deactivate`)
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })
  })

  describe('PATCH /users/me', () => {
    it('should update the authenticated user name', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Old Name',
          email: 'patch1@test.com',
          employeeNumber: 'PATCH001',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', userId)
        .send({ name: 'New Name' })

      expect(res.status).toBe(200)
      expect(res.body.name).toBe('New Name')
      expect(res.body.passwordHash).toBeUndefined()
    })

    it('should update the authenticated user password without returning it', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Password User',
          email: 'patch3@test.com',
          employeeNumber: 'PATCH003',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', userId)
        .send({ password: 'newpassword456' })

      expect(res.status).toBe(200)
      expect(res.body.passwordHash).toBeUndefined()
    })

    it('should update multiple fields of the authenticated user', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Multi User',
          email: 'patch4@test.com',
          employeeNumber: 'PATCH004',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', userId)
        .send({ name: 'Updated Multi User', password: 'updatedpassword' })

      expect(res.status).toBe(200)
      expect(res.body.name).toBe('Updated Multi User')
      expect(res.body.passwordHash).toBeUndefined()
    })

    it('should return 400 if no fields are provided', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Empty Patch',
          email: 'patch5@test.com',
          employeeNumber: 'PATCH005',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', userId)
        .send({})

      expect(res.status).toBe(400)
    })

    it('should return 400 for invalid name', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Invalid Name',
          email: 'patch6@test.com',
          employeeNumber: 'PATCH006',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', userId)
        .send({ name: '' })

      expect(res.status).toBe(400)
    })

    it('should return 400 for invalid password', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          name: 'Invalid Password',
          email: 'patch8@test.com',
          employeeNumber: 'PATCH008',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', userId)
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
