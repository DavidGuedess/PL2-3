import request from 'supertest'
import app from '../app'
import { users } from '../data/users'

describe('Users API', () => {
  beforeEach(() => {
    users.length = 0
  })

  describe('GET /users', () => {
    it('should return all users without password field', async () => {
      await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'List User',
          email: 'listuser@test.com',
          employeeNumber: 'LIST001',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      const res = await request(app).get('/users')

      expect(res.status).toBe(200)
      expect(Array.isArray(res.body)).toBe(true)
      expect(res.body[0].password).toBeUndefined()
    })
  })

  describe('GET /users/me', () => {
    it('should return the authenticated user', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'Me User',
          email: 'me@test.com',
          employeeNumber: 'ME001',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .get('/users/me')
        .set('x-user-id', userId)

      expect(res.status).toBe(200)
      expect(res.body.id).toBe(userId)
      expect(res.body.email).toBe('me@test.com')
      expect(res.body.password).toBeUndefined()
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
          fullName: 'Target User',
          email: 'target@test.com',
          employeeNumber: 'TARGET001',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .get(`/users/${userId}`)
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
      expect(res.body.id).toBe(userId)
      expect(res.body.password).toBeUndefined()
    })

    it('should return 404 if user does not exist', async () => {
      const res = await request(app)
        .get('/users/non-existing-id')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(404)
    })

    it('should return 403 if non-admin tries to get user by id', async () => {
      const res = await request(app)
        .get('/users/non-existing-id')
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
          fullName: 'Test User',
          email: 'testuser1@test.com',
          employeeNumber: 'TEST001',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      expect(res.status).toBe(201)
      expect(res.body).toHaveProperty('id')
      expect(res.body.email).toBe('testuser1@test.com')
      expect(res.body.isActive).toBe(true)
      expect(res.body.password).toBeUndefined()
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

    it('should fail if contact is missing', async () => {
      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'Missing Contact',
          email: 'missingcontact@test.com',
          employeeNumber: 'TEST012',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          password: 'password123'
        })

      expect(res.status).toBe(400)
    })

    it('should fail if password is missing', async () => {
      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'Missing Password',
          email: 'missingpassword@test.com',
          employeeNumber: 'TEST013',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678'
        })

      expect(res.status).toBe(400)
    })

    it('should fail with invalid email', async () => {
      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'Invalid Email',
          email: 'invalid-email',
          employeeNumber: 'TEST002',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      expect(res.status).toBe(400)
    })

    it('should fail with invalid role', async () => {
      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'Invalid Role',
          email: 'role@test.com',
          employeeNumber: 'TEST003',
          role: 'INVALID_ROLE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      expect(res.status).toBe(400)
    })

    it('should fail with invalid professional category', async () => {
      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'Invalid Category',
          email: 'invalidcategory@test.com',
          employeeNumber: 'TEST004',
          role: 'EMPLOYEE',
          professionalCategory: 'INVALID_CATEGORY',
          contact: '912345678',
          password: 'password123'
        })

      expect(res.status).toBe(400)
    })

    it('should fail if email already exists', async () => {
      const user = {
        fullName: 'Duplicate Email',
        email: 'duplicate@test.com',
        employeeNumber: 'TEST005',
        role: 'EMPLOYEE',
        professionalCategory: 'FILLER1',
        contact: '912345678',
        password: 'password123'
      }

      await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send(user)

      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          ...user,
          employeeNumber: 'TEST006'
        })

      expect(res.status).toBe(409)
    })

    it('should fail if employee number already exists', async () => {
      const user = {
        fullName: 'Duplicate Employee',
        email: 'employee@test.com',
        employeeNumber: 'TEST007',
        role: 'EMPLOYEE',
        professionalCategory: 'FILLER1',
        contact: '912345678',
        password: 'password123'
      }

      await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send(user)

      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          ...user,
          email: 'newemail@test.com'
        })

      expect(res.status).toBe(409)
    })

    it('should return 403 if non-admin tries to create a user', async () => {
      const res = await request(app)
        .post('/users')
        .set('x-user-role', 'EMPLOYEE')
        .send({
          fullName: 'Forbidden User',
          email: 'forbidden@test.com',
          employeeNumber: 'TEST010',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
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
          fullName: 'Deactivate User',
          email: 'deactivate@test.com',
          employeeNumber: 'TEST008',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch(`/users/${userId}/deactivate`)
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(200)
      expect(res.body.isActive).toBe(false)
      expect(res.body.password).toBeUndefined()
    })

    it('should fail if user does not exist', async () => {
      const res = await request(app)
        .patch('/users/non-existing-id/deactivate')
        .set('x-user-role', 'ADMIN')

      expect(res.status).toBe(404)
    })

    it('should fail if user is already deactivated', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'Already Deactivated',
          email: 'already@test.com',
          employeeNumber: 'TEST009',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      const userId = createRes.body.id

      await request(app)
        .patch(`/users/${userId}/deactivate`)
        .set('x-user-role', 'ADMIN')

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
          fullName: 'Protected User',
          email: 'protected@test.com',
          employeeNumber: 'TEST011',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch(`/users/${userId}/deactivate`)
        .set('x-user-role', 'EMPLOYEE')

      expect(res.status).toBe(403)
    })
  })

  describe('PATCH /users/me', () => {
    it('should update the authenticated user fullName', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'Old Name',
          email: 'patch1@test.com',
          employeeNumber: 'PATCH001',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', userId)
        .send({
          fullName: 'New Name'
        })

      expect(res.status).toBe(200)
      expect(res.body.fullName).toBe('New Name')
      expect(res.body.password).toBeUndefined()
    })

    it('should update the authenticated user contact', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'Contact User',
          email: 'patch2@test.com',
          employeeNumber: 'PATCH002',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', userId)
        .send({
          contact: '919999999'
        })

      expect(res.status).toBe(200)
      expect(res.body.contact).toBe('919999999')
      expect(res.body.password).toBeUndefined()
    })

    it('should update the authenticated user password without returning it', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'Password User',
          email: 'patch3@test.com',
          employeeNumber: 'PATCH003',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', userId)
        .send({
          password: 'newpassword456'
        })

      expect(res.status).toBe(200)
      expect(res.body.password).toBeUndefined()
    })

    it('should update multiple fields of the authenticated user', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'Multi User',
          email: 'patch4@test.com',
          employeeNumber: 'PATCH004',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', userId)
        .send({
          fullName: 'Updated Multi User',
          contact: '911111111',
          password: 'updatedpassword'
        })

      expect(res.status).toBe(200)
      expect(res.body.fullName).toBe('Updated Multi User')
      expect(res.body.contact).toBe('911111111')
      expect(res.body.password).toBeUndefined()
    })

    it('should return 400 if no fields are provided', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'Empty Patch',
          email: 'patch5@test.com',
          employeeNumber: 'PATCH005',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', userId)
        .send({})

      expect(res.status).toBe(400)
    })

    it('should return 400 for invalid fullName', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'Invalid Name',
          email: 'patch6@test.com',
          employeeNumber: 'PATCH006',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', userId)
        .send({
          fullName: ''
        })

      expect(res.status).toBe(400)
    })

    it('should return 400 for invalid contact', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'Invalid Contact',
          email: 'patch7@test.com',
          employeeNumber: 'PATCH007',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', userId)
        .send({
          contact: ''
        })

      expect(res.status).toBe(400)
    })

    it('should return 400 for invalid password', async () => {
      const createRes = await request(app)
        .post('/users')
        .set('x-user-role', 'ADMIN')
        .send({
          fullName: 'Invalid Password',
          email: 'patch8@test.com',
          employeeNumber: 'PATCH008',
          role: 'EMPLOYEE',
          professionalCategory: 'FILLER1',
          contact: '912345678',
          password: 'password123'
        })

      const userId = createRes.body.id

      const res = await request(app)
        .patch('/users/me')
        .set('x-user-id', userId)
        .send({
          password: ''
        })

      expect(res.status).toBe(400)
    })

    it('should return 401 if authenticated user is missing', async () => {
      const res = await request(app)
        .patch('/users/me')
        .send({
          fullName: 'No Auth'
        })

      expect(res.status).toBe(401)
    })
  })
})