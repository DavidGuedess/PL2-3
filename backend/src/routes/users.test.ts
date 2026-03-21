import request from 'supertest'
import express from 'express'
import usersRoutes from './users'
import { users } from '../data/users'

const app = express()
app.use(express.json())
app.use('/users', usersRoutes)

beforeEach(() => {
  users.splice(3)
})

describe('GET /users', () => {
  it('retorna 200 com um array', async () => {
    const res = await request(app).get('/users')
    expect(res.status).toBe(200)
    expect(Array.isArray(res.body)).toBe(true)
  })
})

describe('POST /users', () => {
  const validUser = {
    name: 'Test User',
    email: 'test@example.com',
    employeeNumber: 'EMP999',
    role: 'EMPLOYEE',
    category: 'VETERINARIAN'
  }

  it('retorna 201 com dados válidos', async () => {
    const res = await request(app)
      .post('/users')
      .set('Content-Type', 'application/json')
      .send(JSON.stringify(validUser))
    expect(res.status).toBe(201)
    expect(res.body).toMatchObject({
      name: 'Test User',
      email: 'test@example.com',
      role: 'EMPLOYEE'
    })
  })

  it('retorna 400 quando faltam campos obrigatórios', async () => {
    const res = await request(app)
      .post('/users')
      .set('Content-Type', 'application/json')
      .send(JSON.stringify({ fullName: 'Test' }))
    expect(res.status).toBe(400)
  })

  it('retorna 400 com role inválido', async () => {
    const res = await request(app)
      .post('/users')
      .set('Content-Type', 'application/json')
      .send(JSON.stringify({ ...validUser, role: 'INVALID' }))
    expect(res.status).toBe(400)
  })

  it('retorna 409 com email duplicado', async () => {
    await request(app)
      .post('/users')
      .set('Content-Type', 'application/json')
      .send(JSON.stringify(validUser))
    const res = await request(app)
      .post('/users')
      .set('Content-Type', 'application/json')
      .send(JSON.stringify(validUser))
    expect(res.status).toBe(409)
  })

  it('retorna 409 com employeeNumber duplicado', async () => {
    await request(app)
      .post('/users')
      .set('Content-Type', 'application/json')
      .send(JSON.stringify(validUser))
    const res = await request(app)
      .post('/users')
      .set('Content-Type', 'application/json')
      .send(JSON.stringify({ ...validUser, email: 'outro@example.com' }))
    expect(res.status).toBe(409)
  })
})
