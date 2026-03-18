import request from 'supertest'
import app from '../app'

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
          fullName: 'Test User',
          email: 'testuser1@test.com',
          employeeNumber: 'TEST001',
          role: 'EMPLOYEE'
        })

      expect(res.status).toBe(201)
      expect(res.body).toHaveProperty('id')
      expect(res.body.email).toBe('testuser1@test.com')
      expect(res.body.isActive).toBe(true)
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
          fullName: 'Invalid Email',
          email: 'invalid-email',
          employeeNumber: 'TEST002',
          role: 'EMPLOYEE'
        })

      expect(res.status).toBe(400)
    })


    it('should fail with invalid role', async () => {
      const res = await request(app)
        .post('/users')
        .send({
          fullName: 'Invalid Role',
          email: 'role@test.com',
          employeeNumber: 'TEST003',
          role: 'INVALID_ROLE'
        })

      expect(res.status).toBe(400)
    })


    it('should fail if email already exists', async () => {
      const user = {
        fullName: 'Duplicate Email',
        email: 'duplicate@test.com',
        employeeNumber: 'TEST004',
        role: 'EMPLOYEE'
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
        fullName: 'Duplicate Employee',
        email: 'employee@test.com',
        employeeNumber: 'TEST006',
        role: 'EMPLOYEE'
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
          fullName: 'Deactivate User',
          email: 'deactivate@test.com',
          employeeNumber: 'TEST007',
          role: 'EMPLOYEE'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch(`/users/${userId}/deactivate`)

      expect(res.status).toBe(200)
      expect(res.body.isActive).toBe(false)
    })


    it('should fail if user does not exist', async () => {
      const res = await request(app)
        .patch('/users/non-existing-id/deactivate')

      expect(res.status).toBe(404)
    })


    it('should fail if user is already deactivated', async () => {
      const createRes = await request(app)
        .post('/users')
        .send({
          fullName: 'Already Deactivated',
          email: 'already@test.com',
          employeeNumber: 'TEST008',
          role: 'EMPLOYEE'
        })

      const userId = createRes.body.id

      await request(app).patch(`/users/${userId}/deactivate`)

      const res = await request(app)
        .patch(`/users/${userId}/deactivate`)

      expect(res.status).toBe(409)
    })

  })

})