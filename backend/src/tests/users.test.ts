import request from 'supertest'
import app from '../app'

jest.mock('../middleware/auth', () => ({
  authenticate: (_req: any, _res: any, next: any) => next()
}))

describe('Users API', () => {

  // ======================
  // GET /users
  // ======================
  describe('GET /users', () => {
    it('should return all users', async () => {
      const res = await request(app).get('/users')

      expect(res.status).toBe(200)
      expect(Array.isArray(res.body)).toBe(true)
    })
  })


  // ======================
  // POST /users
  // ======================
  describe('POST /users', () => {

    it('should create a new user', async () => {
      const res = await request(app)
        .post('/users')
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
      expect(res.body).not.toHaveProperty('passwordHash')
    })


    it('should fail if required fields are missing', async () => {
      const res = await request(app)
        .post('/users')
        .send({
          email: 'missing@test.com'
        })

      expect(res.status).toBe(400)
    })


    it('should fail with invalid email', async () => {
      const res = await request(app)
        .post('/users')
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

      await request(app).post('/users').send(user)

      const res = await request(app).post('/users').send({
        ...user,
        employeeNumber: 'TEST005'
      })

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

      await request(app).post('/users').send(user)

      const res = await request(app).post('/users').send({
        ...user,
        email: 'newemail@test.com'
      })

      expect(res.status).toBe(409)
    })

  })


  // ======================
  // PATCH /users/:id/deactivate
  // ======================
  describe('PATCH /users/:id/deactivate', () => {

    it('should deactivate a user', async () => {
      const createRes = await request(app)
        .post('/users')
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

      expect(res.status).toBe(200)
      expect(res.body.active).toBe(false)
    })


    it('should fail if user does not exist', async () => {
      const res = await request(app)
        .patch('/users/99999/deactivate')

      expect(res.status).toBe(404)
    })


    it('should fail if user is already deactivated', async () => {
      const createRes = await request(app)
        .post('/users')
        .send({
          name: 'Already Deactivated',
          email: 'already@test.com',
          employeeNumber: 'TEST008',
          role: 'EMPLOYEE',
          category: 'VETERINARIAN',
          password: 'secret123'
        })

      const userId = createRes.body.id

      await request(app).patch(`/users/${userId}/deactivate`)

      const res = await request(app)
        .patch(`/users/${userId}/deactivate`)

      expect(res.status).toBe(409)
    })

  })

})
