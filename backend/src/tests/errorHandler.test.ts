import request from 'supertest'
import express from 'express'
import { errorHandler } from '../middleware/errorHandler'

function makeApp(err: unknown) {
  const app = express()
  app.get('/test', (_req, _res, next) => next(err))
  app.use(errorHandler)
  return app
}

beforeEach(() => {
  jest.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  jest.restoreAllMocks()
})

describe('errorHandler middleware', () => {

  it('returns 409 with field message for P2002 email', async () => {
    const app = makeApp({ code: 'P2002', meta: { target: ['email'] } })
    const res = await request(app).get('/test')
    expect(res.status).toBe(409)
    expect(res.body.message).toBe('Email already exists')
  })

  it('returns 409 with field message for P2002 employeeNumber', async () => {
    const app = makeApp({ code: 'P2002', meta: { target: ['employeeNumber'] } })
    const res = await request(app).get('/test')
    expect(res.status).toBe(409)
    expect(res.body.message).toBe('Employee number already exists')
  })

  it('returns 409 with generic message for P2002 unknown field', async () => {
    const app = makeApp({ code: 'P2002', meta: { target: ['otherField'] } })
    const res = await request(app).get('/test')
    expect(res.status).toBe(409)
    expect(res.body).toHaveProperty('message')
  })

  it('returns 404 for P2025', async () => {
    const app = makeApp({ code: 'P2025' })
    const res = await request(app).get('/test')
    expect(res.status).toBe(404)
    expect(res.body).toHaveProperty('message')
  })

  it('returns 500 for unknown errors', async () => {
    const app = makeApp(new Error('Unexpected error'))
    const res = await request(app).get('/test')
    expect(res.status).toBe(500)
    expect(res.body).toHaveProperty('message')
  })

  it('returns 500 for non-Prisma errors', async () => {
    const app = makeApp({ someOtherField: 'unexpected' })
    const res = await request(app).get('/test')
    expect(res.status).toBe(500)
  })

})
